package io.github.nexalloy.ui

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.libxposed.service.XposedService
import io.github.nexalloy.AppPatchInfo
import io.github.nexalloy.Patch
import io.github.nexalloy.activity.SettingApplication
import io.github.nexalloy.core.BundleRegistry
import io.github.nexalloy.core.InstalledBundle
import io.github.nexalloy.core.ModuleStatus
import io.github.nexalloy.core.R
import io.github.nexalloy.core.compatibility.VersionComparator
import io.github.nexalloy.core.discovery.AppDiscovery
import io.github.nexalloy.core.discovery.AppItemState
import io.github.nexalloy.manager.BundleManager
import io.github.nexalloy.manager.PatchSource
import io.github.nexalloy.manager.SourceClient
import io.github.nexalloy.manager.SourceProvenance
import io.github.nexalloy.manager.SourceRelease
import io.github.nexalloy.manager.SourceReleaseCache
import io.github.nexalloy.manager.SourceStore
import io.github.nexalloy.patchPreferenceKey
import io.github.nexalloy.updates.AppInstaller
import io.github.nexalloy.updates.AppPreferenceNames
import io.github.nexalloy.updates.AppRelease
import io.github.nexalloy.updates.AppSettings
import io.github.nexalloy.updates.AppUpdateChecker
import io.github.nexalloy.updates.UpdateDownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import java.io.File

data class SourceDownloadProgress(
    val bundleName: String,
    val bundleIndex: Int,
    val totalBundles: Int,
    val progress: Float, // 0f..1f, or -1f if indeterminate
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val isInstalling: Boolean = false,
)

data class ManagerState(
    /** The Xposed service, null while the module is not active. */
    val service: XposedService? = null,
    /** Name and version of the framework running the module. */
    val framework: String? = null,
    val bundles: List<InstalledBundle> = emptyList(),
    val sources: List<PatchSource> = emptyList(),
    /** Latest release of each source, or the error fetching it. */
    val releases: Map<String, Result<SourceRelease>> = emptyMap(),
    /** Sources currently performing an install/download operation. */
    val busySources: Set<String> = emptySet(),
    /** Live download progress per source id. */
    val sourceDownloadProgress: Map<String, SourceDownloadProgress> = emptyMap(),
    /** Installed apps patched by enabled bundles that are missing from the module scope. */
    val missingScope: List<String> = emptyList(),
    val appUpdate: AppRelease? = null,
    val updateDownloadState: UpdateDownloadState = UpdateDownloadState.Idle,
    val checkingUpdates: Boolean = false,
    /** Full catalog of supported and installed applications. */
    val appCatalog: List<AppItemState> = emptyList(),
    val currentTheme: String = AppSettings.THEME_SYSTEM,
    val currentThemeStyle: String = AppSettings.THEME_STYLE_MATERIAL3,
    val currentColorScheme: String = AppSettings.COLOR_SCHEME_DEFAULT,
    /** Miuix-only: floating navigation bar preference. Disabled by default. */
    val floatingNavigation: Boolean = AppPreferenceNames.DEFAULT_FLOATING_NAVIGATION,
    val searchQuery: String = "",
) {
    val isActive get() = service != null

    /** Package name to display name of the apps patched by enabled bundles. */
    val apps: Map<String, String>
        get() = bundles.filter { it.enabled }.flatMap { it.apps.entries }
            .associate { it.key to it.value }

    fun hasUpdate(source: PatchSource): Boolean {
        if (!source.enabled) return false
        val release = releases[source.id]?.getOrNull() ?: return false
        val installed = source.installedVersion ?: return false
        return VersionComparator.compare(release.version, installed) > 0
    }

    val sourcesWithUpdates get() = sources.filter { it.enabled }.filter(::hasUpdate)
}

/** The patches of one bundle for one app, with whether each is enabled and any duplicates. */
class BundlePatches(
    val bundle: InstalledBundle,
    val patches: List<Pair<Patch, Boolean>>,
    val duplicateNames: Set<String> = emptySet(),
    val sourceName: String? = null,
)

class ManagerViewModel(application: Application) : AndroidViewModel(application),
    SettingApplication.ServiceStateListener {

    private val context get() = getApplication<Application>()
    private val store = SourceStore(application)
    private val releaseCache = SourceReleaseCache(application)
    val settings = AppSettings(application)

    private val _state = MutableStateFlow(
        ManagerState(
            sources = store.all(),
            currentTheme = settings.theme,
            currentThemeStyle = settings.themeStyle,
            currentColorScheme = settings.colorScheme,
            floatingNavigation = settings.miuixFloatingNavigation,
            releases = loadInitialReleases(),
        )
    )
    val state: StateFlow<ManagerState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    /** One-off messages for a snackbar. */
    val messages = _messages.asSharedFlow()

    private var ownScopeRequested = false
    /** Current update-check job so repeated taps cancel the previous one. */
    private var updateCheckJob: Job? = null

    init {
        SettingApplication.addServiceStateListener(this, true)
        refresh()
        checkUpdates()
    }

    private fun loadInitialReleases(): Map<String, Result<SourceRelease>> {
        return store.all().mapNotNull { source ->
            releaseCache.get(source.id)?.let { source.id to Result.success(it) }
        }.toMap()
    }

    override fun onCleared() {
        SettingApplication.removeServiceStateListener(this)
    }

    override fun onServiceStateChanged(service: XposedService?) {
        val active = service?.takeIf {
            runCatching { it.getRemotePreferences(BundleRegistry.PREFS_GROUP) }.isSuccess
        }
        val framework = active?.let { "${it.frameworkName} ${it.frameworkVersion}" }
            ?: ModuleStatus.frameworkName()
        _state.update { it.copy(service = active, framework = framework) }
        refresh()
        if (active != null) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { BundleManager(context, active).syncToService() }
            }
            requestOwnScope(active)
        }
    }

    private val manager get() = BundleManager(context, _state.value.service)

    fun message(text: String) {
        _messages.tryEmit(text)
    }

    fun message(resource: Int, vararg args: Any?) {
        _messages.tryEmit(context.getString(resource, *args))
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun setTheme(theme: String) {
        settings.theme = theme
        _state.update { it.copy(currentTheme = theme) }
    }

    fun setThemeStyle(style: String) {
        val normalized = AppPreferenceNames.normalizeStyle(style)
        settings.themeStyle = normalized
        _state.update { it.copy(currentThemeStyle = normalized) }
    }

    /** Selects the accent colour scheme applied to both design systems. */
    fun setColorScheme(scheme: String) {
        val normalized = AppPreferenceNames.normalizeColorScheme(scheme)
        settings.colorScheme = normalized
        _state.update { it.copy(currentColorScheme = normalized) }
    }

    /**
     * Miuix-only floating navigation bar. Disabled by default; switching this only changes which
     * Miuix navigation arrangement is used and never touches patch configuration.
     */
    fun setFloatingNavigation(enabled: Boolean) {
        settings.miuixFloatingNavigation = enabled
        _state.update { it.copy(floatingNavigation = enabled) }
    }

    /** Re-reads bundles, sources, scope, and reconstructs the app catalog. */
    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val mgr = manager
            val sources = store.all()
            val bundles = runCatching { mgr.installed }.getOrDefault(emptyList())
            val missing = runCatching { mgr.missingScope() }.getOrDefault(emptyList())
            val catalog = AppDiscovery.buildCatalog(context, bundles, missing.toSet(), sources)
            _state.update { state ->
                state.copy(
                    sources = sources,
                    bundles = bundles,
                    missingScope = missing,
                    appCatalog = catalog,
                    currentTheme = settings.theme,
                    currentThemeStyle = settings.themeStyle,
                    currentColorScheme = settings.colorScheme,
                    floatingNavigation = settings.miuixFloatingNavigation,
                )
            }
        }
    }

    /** Checks NexAlloy and all enabled sources for updates independently. */
    fun checkUpdates(userInitiated: Boolean = false) {
        // Cancel any in-progress check so repeated taps don't stack up.
        updateCheckJob?.cancel()
        updateCheckJob = viewModelScope.launch {
            // Small debounce so a quick tap doesn't retrigger immediately.
            delay(200)
            _state.update { it.copy(checkingUpdates = true) }
            val appUpdate = withContext(Dispatchers.IO) { runCatching { AppUpdateChecker.latest() } }
            val sources = store.all()
            val currentReleases = _state.value.releases.toMutableMap()

            withContext(Dispatchers.IO) {
                for (source in sources) {
                    if (!source.enabled) continue
                    val result = runCatching { SourceClient.latestRelease(source) }
                    if (result.isSuccess) {
                        val rel = result.getOrThrow()
                        releaseCache.put(source.id, rel)
                        store.updateStatus(source.id, System.currentTimeMillis(), null)
                        currentReleases[source.id] = Result.success(rel)
                    } else {
                        val err = result.exceptionOrNull()
                        store.updateStatus(source.id, source.lastRefreshTime, err?.message)
                        val cached = releaseCache.get(source.id)
                        if (cached != null) {
                            // Preserve cached release data so failure does not wipe the card
                            currentReleases[source.id] = Result.success(cached)
                        } else {
                            currentReleases[source.id] = Result.failure(err ?: Exception("Unknown error"))
                        }
                    }
                }
            }

            _state.update {
                it.copy(
                    sources = store.all(),
                    appUpdate = appUpdate.getOrNull()?.takeIf { release -> release.isNewer },
                    releases = currentReleases,
                    checkingUpdates = false,
                )
            }
            if (userInitiated) {
                val state = _state.value
                val update = state.appUpdate
                when {
                    appUpdate.isFailure -> message(R.string.update_check_failed, appUpdate.exceptionOrNull()?.message)
                    update != null -> message(R.string.app_update_available, update.tag)
                    state.sourcesWithUpdates.isNotEmpty() -> message(R.string.patch_updates_available)
                    else -> message(R.string.up_to_date)
                }
            }
        }
    }

    /**
     * Directly downloads and installs an update for NexAlloyNext in-app without third-party browsers.
     */
    fun downloadAndInstallUpdate(release: AppRelease) {
        val apkUrl = release.apkUrl
        if (apkUrl.isNullOrBlank()) {
            message(R.string.app_update_no_apk)
            return
        }

        viewModelScope.launch {
            val result = AppInstaller.downloadApk(
                context = context,
                tag = release.tag,
                apkUrl = apkUrl,
                onProgress = { downloadState ->
                    _state.update { it.copy(updateDownloadState = downloadState) }
                },
            )

            result.onSuccess { apkFile ->
                _state.update { it.copy(updateDownloadState = UpdateDownloadState.ReadyToInstall(apkFile)) }
                val installResult = AppInstaller.installApk(context, apkFile)
                installResult.onFailure { err ->
                    message(R.string.install_failed, err.message)
                }
            }.onFailure { err ->
                _state.update { it.copy(updateDownloadState = UpdateDownloadState.Error(err.message ?: "Download failed")) }
                message(R.string.download_failed, err.message)
            }
        }
    }

    // region Sources

    /** Refreshes a single source independently. */
    fun refreshSource(source: PatchSource) {
        _state.update { it.copy(busySources = it.busySources + source.id) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { SourceClient.latestRelease(source) }
            }
            _state.update { it.copy(busySources = it.busySources - source.id) }
            result.onSuccess { release ->
                releaseCache.put(source.id, release)
                store.updateStatus(source.id, System.currentTimeMillis(), null)
                _state.update {
                    it.copy(
                        releases = it.releases + (source.id to Result.success(release)),
                        sources = store.all(),
                    )
                }
                message(R.string.source_refreshed, source.name)
            }.onFailure { err ->
                store.updateStatus(source.id, source.lastRefreshTime, err.message)
                val cached = releaseCache.get(source.id)
                val effective = if (cached != null) Result.success(cached) else Result.failure(err)
                _state.update {
                    it.copy(
                        releases = it.releases + (source.id to effective),
                        sources = store.all(),
                    )
                }
                message(R.string.source_refresh_failed, source.name, err.message)
            }
        }
    }

    fun setSourceEnabled(source: PatchSource, enabled: Boolean) {
        store.setEnabled(source.id, enabled)
        refresh()
    }

    fun install(source: PatchSource) {
        val mgr = manager
        _state.update {
            it.copy(
                busySources = it.busySources + source.id,
                sourceDownloadProgress = it.sourceDownloadProgress + (source.id to SourceDownloadProgress(
                    bundleName = "",
                    bundleIndex = 1,
                    totalBundles = 1,
                    progress = -1f,
                    bytesDownloaded = 0L,
                    totalBytes = 0L,
                    isInstalling = false,
                ))
            )
        }
        val before = mgr.installed
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val release = _state.value.releases[source.id]?.getOrNull() ?: SourceClient.latestRelease(source)
                    val totalBundles = release.bundles.size
                    val bundles = release.bundles.mapIndexed { index, (bundleName, url) ->
                        val connection = SourceClient.open(url)
                        val totalBytes = connection.contentLengthLong
                        _state.update {
                            it.copy(
                                sourceDownloadProgress = it.sourceDownloadProgress + (source.id to SourceDownloadProgress(
                                    bundleName = bundleName,
                                    bundleIndex = index + 1,
                                    totalBundles = totalBundles,
                                    progress = if (totalBytes > 0) 0f else -1f,
                                    bytesDownloaded = 0L,
                                    totalBytes = totalBytes,
                                    isInstalling = false,
                                ))
                            )
                        }
                        var lastReportedPct = -1
                        var lastReportedTime = 0L
                        connection.inputStream.use { stream ->
                            mgr.install(stream, source.id, totalBytes) { downloaded, total ->
                                val pct = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else -1f
                                val pctInt = if (pct >= 0f) (pct * 100).toInt() else -1
                                val now = System.currentTimeMillis()
                                if (pctInt != lastReportedPct || now - lastReportedTime > 100L || downloaded == total) {
                                    lastReportedPct = pctInt
                                    lastReportedTime = now
                                    _state.update {
                                        it.copy(
                                            sourceDownloadProgress = it.sourceDownloadProgress + (source.id to SourceDownloadProgress(
                                                bundleName = bundleName,
                                                bundleIndex = index + 1,
                                                totalBundles = totalBundles,
                                                progress = pct,
                                                bytesDownloaded = downloaded,
                                                totalBytes = total,
                                                isInstalling = false,
                                            ))
                                        )
                                    }
                                }
                            }
                        }
                    }
                    _state.update {
                        it.copy(
                            sourceDownloadProgress = it.sourceDownloadProgress + (source.id to SourceDownloadProgress(
                                bundleName = "",
                                bundleIndex = totalBundles,
                                totalBundles = totalBundles,
                                progress = 1f,
                                bytesDownloaded = 0L,
                                totalBytes = 0L,
                                isInstalling = true,
                            ))
                        )
                    }
                    releaseCache.put(source.id, release)
                    store.save(source.copy(installedVersion = release.version, lastRefreshTime = System.currentTimeMillis(), lastError = null))
                    release to bundles
                }
            }
            _state.update {
                it.copy(
                    busySources = it.busySources - source.id,
                    sourceDownloadProgress = it.sourceDownloadProgress - source.id,
                )
            }
            result.onSuccess { (release, bundles) ->
                _state.update { it.copy(releases = it.releases + (source.id to Result.success(release)), sources = store.all()) }
                message(R.string.bundles_installed, bundles.joinToString { "${it.name} ${it.version}" })
                requestMissingScope(bundles.flatMap { newApps(it, before) })
            }.onFailure { err ->
                message(R.string.install_failed, err.message)
            }
            refresh()
        }
    }

    fun updateAll() = _state.value.sourcesWithUpdates.forEach(::install)

    fun addSource(name: String, url: String, prerelease: Boolean) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return
        val provenance = SourceProvenance.fromUrl(trimmed)
        val source = PatchSource.create(
            name = name.trim().ifEmpty { trimmed.substringAfterLast('/') },
            url = trimmed,
            prerelease = prerelease,
            provenance = provenance,
        )
        store.save(source)
        refresh()
        refreshSource(source)
    }

    fun removeSource(source: PatchSource) {
        store.remove(source.id)
        releaseCache.remove(source.id)
        refresh()
    }

    fun importBundle(uri: Uri) {
        val mgr = manager
        val before = mgr.installed
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)!!.use { mgr.install(it, null) } }
            }
            result.onSuccess {
                message(R.string.bundles_installed, "${it.name} ${it.version}")
                requestMissingScope(newApps(it, before))
            }.onFailure { message(R.string.install_failed, it.message) }
            refresh()
        }
    }

    // endregion

    // region Bundles

    fun setEnabled(bundle: InstalledBundle, enabled: Boolean) {
        val mgr = manager
        mgr.setEnabled(bundle, enabled)
        if (enabled) requestMissingScope(bundle.apps.keys)
        refresh()
    }

    fun uninstall(bundle: InstalledBundle) {
        val mgr = manager
        mgr.uninstall(bundle)
        store.all().find { it.id == bundle.sourceId }?.let { source ->
            if (mgr.installed.none { it.sourceId == source.id }) store.save(source.copy(installedVersion = null))
        }
        refresh()
    }

    /** Apps of [bundle] it did not patch before this install, so updates do not ask again for declined apps. */
    private fun newApps(bundle: InstalledBundle, before: List<InstalledBundle>): Set<String> =
        bundle.apps.keys - before.find { it.id == bundle.id }?.apps?.keys.orEmpty()

    // endregion

    // region Scope

    /** Hooked apps only load bundles if the app is in the module scope. */
    fun requestMissingScope(packages: Collection<String> = _state.value.missingScope) {
        val mgr = manager
        val missing = runCatching { mgr.missingScope() }.getOrDefault(emptyList()).filter { it in packages }
        if (missing.isEmpty()) return
        mgr.requestScope(missing) { result ->
            _messages.tryEmit(result)
            viewModelScope.launch { refresh() }
        }
    }

    /** Frameworks only put a legacy module's own app in its scope by themselves, so NexAlloy asks for it. */
    private fun requestOwnScope(service: XposedService) {
        if (ownScopeRequested) return
        ownScopeRequested = true
        val packageName = context.packageName
        if (runCatching { packageName in service.scope }.getOrDefault(true)) return
        runCatching { service.requestScope(listOf(packageName), object : XposedService.OnScopeEventListener {}) }
    }

    // endregion

    // region Patches of an app

    /** Loads the patches enabled bundles provide for [packageName], with offline fallback. */
    suspend fun patches(packageName: String): Result<List<BundlePatches>> = withContext(Dispatchers.IO) {
        runCatching {
            val state = _state.value
            val service = state.service
            val mgr = manager
            val enabledSourceIds = state.sources.filter { it.enabled }.map { it.id }.toSet()
            val prefs = service?.let { runCatching { it.getRemotePreferences(packageName) }.getOrNull() }
            val activeBundles = state.bundles.filter {
                it.enabled && (it.sourceId == null || it.sourceId in enabledSourceIds) && packageName in it.apps
            }
            val bundlePatchLists = activeBundles.map { bundle ->
                val apps: List<AppPatchInfo> = mgr.patches(bundle).filter { it.packageName == packageName }
                val patches = apps.flatMap { it.patches.asList() }
                    .filter { it.name.isNotEmpty() && !it.name.startsWith("<") }
                    .sortedBy { it.name }
                    .map { patch ->
                        val isEnabled = prefs?.getBoolean(patchPreferenceKey(bundle.id, patch.name), patch.use)
                            ?: patch.use
                        patch to isEnabled
                    }
                val sourceName = state.sources.find { it.id == bundle.sourceId }?.name
                Triple(bundle, patches, sourceName)
            }
            val allNames = bundlePatchLists.flatMap { (_, patches, _) -> patches.map { it.first.name } }
            val duplicates = allNames.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
            bundlePatchLists.map { (bundle, patches, sourceName) ->
                BundlePatches(bundle, patches, duplicates, sourceName)
            }
        }
    }

    fun setPatchEnabled(packageName: String, bundle: InstalledBundle, patch: Patch, enabled: Boolean) {
        val service = _state.value.service
        if (service != null) {
            val prefs = service.getRemotePreferences(packageName)
            val committed = prefs.edit().putBoolean(patchPreferenceKey(bundle.id, patch.name), enabled).commit()
            if (!committed) {
                prefs.edit().putBoolean(patchPreferenceKey(bundle.id, patch.name), enabled).apply()
            }
        } else {
            message("Settings will be saved when LSPosed framework is active")
        }
    }

    /** Sets every patch of the app to its default (or to off, if [defaults] is false). */
    fun resetPatches(packageName: String, patches: List<BundlePatches>, defaults: Boolean) {
        val service = _state.value.service ?: run {
            message("LSPosed framework service is currently inactive")
            return
        }
        val prefs = service.getRemotePreferences(packageName)
        val editor = prefs.edit()
        for (group in patches) for ((patch, _) in group.patches) {
            editor.putBoolean(patchPreferenceKey(group.bundle.id, patch.name), defaults && patch.use)
        }
        if (!editor.commit()) {
            editor.apply()
        }
    }

    /** Force stops the target application via root 'am force-stop' with background process kill fallback. */
    fun forceStopApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            var stoppedViaRoot = false
            runCatching {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "am force-stop $packageName"))
                stoppedViaRoot = process.waitFor() == 0
            }
            if (!stoppedViaRoot) {
                runCatching {
                    val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                    am?.killBackgroundProcesses(packageName)
                }
            }
            val appLabel = runCatching {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            }.getOrDefault(packageName)

            withContext(Dispatchers.Main) {
                if (stoppedViaRoot) {
                    message("Force stopped $appLabel")
                } else {
                    message("Terminated background processes for $appLabel. True force-stop requires root or App Info.")
                }
            }
        }
    }

    /** Opens Android System App Info settings page for the specified package. */
    fun openAppInfo(packageName: String) {
        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", packageName, null)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            message("Could not open App Info for $packageName")
        }
    }

    // endregion

    // region Diagnostics & Cache

    fun clearDexKitCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = File(context.codeCacheDir, "nexalloy")
            val count = cacheDir.listFiles()?.count { it.delete() } ?: 0
            message(R.string.cache_cleared, count)
        }
    }

    // endregion

    // region Launcher icon

    private val launcherAlias get() = ComponentName(context, "io.github.nexalloy.ui.MainActivityAlias")

    var launcherIconHidden: Boolean
        get() = context.packageManager.getComponentEnabledSetting(launcherAlias) ==
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        set(hidden) = context.packageManager.setComponentEnabledSetting(
            launcherAlias,
            if (hidden) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )

    // endregion
}
