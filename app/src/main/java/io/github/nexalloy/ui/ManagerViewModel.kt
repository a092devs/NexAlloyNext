package io.github.nexalloy.ui

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.libxposed.service.XposedService
import io.github.nexalloy.AppPatchInfo
import io.github.nexalloy.Patch
import io.github.nexalloy.activity.SettingApplication
import io.github.nexalloy.core.BuildConfig
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    val installedPatchedApps: List<AppItemState>
        get() = appCatalog.filter { it.isInstalled && it.bundles.isNotEmpty() }
            .filter { matchesSearch(it) }

    val supportedUninstalledApps: List<AppItemState>
        get() = appCatalog.filter { !it.isInstalled && it.bundles.isNotEmpty() }
            .filter { matchesSearch(it) }

    val appsWithConflicts: List<AppItemState>
        get() = appCatalog.filter { it.hasConflicts }.filter { matchesSearch(it) }

    private fun matchesSearch(item: AppItemState): Boolean {
        if (searchQuery.isBlank()) return true
        val query = searchQuery.trim().lowercase()
        return item.appName.lowercase().contains(query) || item.packageName.lowercase().contains(query)
    }
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
        _state.update { it.copy(checkingUpdates = true) }
        viewModelScope.launch {
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
                when {
                    appUpdate.isFailure -> message(R.string.update_check_failed, appUpdate.exceptionOrNull()?.message)
                    state.appUpdate == null && state.sourcesWithUpdates.isEmpty() -> message(R.string.up_to_date)
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
        _state.update { it.copy(busySources = it.busySources + source.id) }
        val before = mgr.installed
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val release = _state.value.releases[source.id]?.getOrNull() ?: SourceClient.latestRelease(source)
                    val bundles = release.bundles.map { (_, url) ->
                        SourceClient.open(url).inputStream.use { mgr.install(it, source.id) }
                    }
                    releaseCache.put(source.id, release)
                    store.save(source.copy(installedVersion = release.version, lastRefreshTime = System.currentTimeMillis(), lastError = null))
                    release to bundles
                }
            }
            _state.update { it.copy(busySources = it.busySources - source.id) }
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
            service.getRemotePreferences(packageName).edit()
                .putBoolean(patchPreferenceKey(bundle.id, patch.name), enabled).apply()
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
        val editor = service.getRemotePreferences(packageName).edit()
        for (group in patches) for ((patch, _) in group.patches) {
            editor.putBoolean(patchPreferenceKey(group.bundle.id, patch.name), defaults && patch.use)
        }
        editor.apply()
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

    fun exportDiagnostics(): String {
        val state = _state.value
        return buildString {
            appendLine("=== NexAlloyNext Diagnostics ===")
            appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Commit: ${BuildConfig.COMMIT_HASH}")
            appendLine("Android OS: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("LSPosed Active: ${state.isActive}")
            appendLine("Framework: ${state.framework ?: "None / Inactive"}")
            appendLine("Missing Scope: ${state.missingScope.joinToString().ifEmpty { "None" }}")
            appendLine("Configured Sources: ${state.sources.size}")
            for (s in state.sources) {
                appendLine(" - ${s.name} [${s.provenance}] (${s.url}): enabled=${s.enabled}, installed=${s.installedVersion ?: "None"}, lastRefresh=${s.lastRefreshTime}, error=${s.lastError ?: "None"}")
            }
            appendLine("Installed Bundles: ${state.bundles.size}")
            for (b in state.bundles) {
                appendLine(" - ${b.name} v${b.version} (${b.id}), source=${b.sourceId ?: "manual"}, enabled=${b.enabled}, apps=${b.apps.keys.joinToString()}")
            }
            appendLine("App Catalog Entries: ${state.appCatalog.size}")
            for (app in state.appCatalog) {
                appendLine(" - ${app.appName} (${app.packageName}): installed=${app.installedVersionName ?: "No"}, compat=${app.compatibility}, sources=${app.sourceNames.joinToString()}, conflicts=${app.hasConflicts}")
            }
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
