package io.github.nexalloy.manager

import android.content.Context
import io.github.libxposed.service.XposedService
import io.github.nexalloy.AppPatchInfo
import io.github.nexalloy.core.BundleClassLoader
import io.github.nexalloy.core.BundleManifest
import io.github.nexalloy.core.BundleParentClassLoader
import io.github.nexalloy.core.BundleRegistry
import io.github.nexalloy.core.InstalledBundle
import io.github.nexalloy.core.InvalidBundleException
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Installs, updates and removes patch bundles.
 *
 * Bundle APKs are stored twice: as a remote file of the Xposed service, which hooked apps read,
 * and in the manager's own storage, from which the manager lists their patches.
 *
 * Offline resilience: reads from local preferences when the XposedService is inactive.
 */
class BundleManager(private val context: Context, private val service: XposedService? = null) {
    private val localPrefs get() = context.getSharedPreferences(BundleRegistry.PREFS_GROUP, Context.MODE_PRIVATE)
    private val registryPrefs get() = service?.takeIf {
        runCatching { it.getRemotePreferences(BundleRegistry.PREFS_GROUP) }.isSuccess
    }?.getRemotePreferences(BundleRegistry.PREFS_GROUP)

    private val localDir get() = File(context.filesDir, "bundles").apply { mkdirs() }

    val installed: List<InstalledBundle>
        get() {
            val remote = registryPrefs?.let { runCatching { BundleRegistry.read(it) }.getOrNull() }
            if (!remote.isNullOrEmpty()) {
                // Sync to local preferences for offline access
                BundleRegistry.write(localPrefs, remote)
                return remote
            }
            val local = BundleRegistry.read(localPrefs)
            if (local.isNotEmpty()) {
                // If local has bundles but remote was empty/unpopulated, sync to remote
                registryPrefs?.let { runCatching { BundleRegistry.write(it, local) } }
            }
            return local
        }

    /**
     * Synchronizes local bundles and their APK files to the XposedService remote storage.
     * Ensures target hooked processes can access bundle APKs and registry metadata.
     */
    fun syncToService() {
        val svc = service ?: return
        val current = installed
        if (current.isNotEmpty()) {
            registryPrefs?.let { runCatching { BundleRegistry.write(it, current) } }
            for (bundle in current) {
                val localFile = File(localDir, bundle.fileName)
                if (localFile.exists()) {
                    runCatching {
                        svc.openRemoteFile(bundle.fileName).use { pfd ->
                            if (pfd.statSize == 0L || pfd.statSize != localFile.length()) {
                                FileOutputStream(pfd.fileDescriptor).use { output ->
                                    output.channel.truncate(0)
                                    localFile.inputStream().use { it.copyTo(output) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** Installs or updates a bundle. Blocking; call off the main thread. */
    fun install(
        input: InputStream,
        sourceId: String?,
        totalBytes: Long = -1L,
        onProgress: ((downloaded: Long, total: Long) -> Unit)? = null,
    ): InstalledBundle {
        val tmp = File.createTempFile("bundle", ".apk", context.cacheDir)
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            tmp.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var downloaded = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)
                    downloaded += read
                    onProgress?.invoke(downloaded, totalBytes)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }.take(16)

            val manifest = BundleManifest.read(context.packageManager, tmp)
            manifest.validateCompatibility()

            var clash = installed.find {
                it.id != manifest.id && manifest.resourcePackageId != 0 && it.resourcePackageId == manifest.resourcePackageId
            }
            var migratedClash: InstalledBundle? = null
            // A source that renamed its bundle, or seamless migration from a092devs to nexfusion:
            val isRenamedMigration = clash != null && (
                (clash.id == "io.github.nexalloy.patches.a092devs" && manifest.id == "io.github.nexalloy.patches.nexfusion") ||
                (sourceId != null && clash.sourceId == sourceId)
            )
            if (isRenamedMigration) {
                migratedClash = clash
                uninstall(clash!!)
                clash = null
            }
            if (clash != null) throw InvalidBundleException(
                "${manifest.name} uses the resource package id 0x%02x of ${clash.name}; both cannot be installed."
                    .format(manifest.resourcePackageId)
            )
            val fileName = "${manifest.id}-$hash.apk"
            val local = File(localDir, fileName)
            if (!local.exists()) {
                tmp.copyTo(local, overwrite = true)
            }
            // Fails here, before anything is published, if the bundle's code cannot be loaded.
            val apps = loadApps(local, manifest.entryClass)

            service?.let { svc ->
                svc.openRemoteFile(fileName).use { pfd ->
                    FileOutputStream(pfd.fileDescriptor).use { output ->
                        output.channel.truncate(0)
                        local.inputStream().use { it.copyTo(output) }
                    }
                }
            }

            val previous = installed.find { it.id == manifest.id } ?: migratedClash
            val bundle = InstalledBundle(
                id = manifest.id,
                name = manifest.name,
                version = manifest.version,
                versionCode = manifest.versionCode,
                author = manifest.author,
                description = manifest.description,
                fileName = fileName,
                enabled = previous?.enabled ?: true,
                sourceId = sourceId ?: previous?.sourceId,
                apps = apps.associate { it.packageName to it.appName },
                resourcePackageId = manifest.resourcePackageId,
                compatibilitySeries = manifest.compatibilitySeries,
                minManagerVersion = manifest.minManagerVersion,
                maxManagerVersion = manifest.maxManagerVersion,
                targets = apps.filter { it.targets.isNotEmpty() }.associate { it.packageName to it.targets },
                appIconColors = apps.mapNotNull { app -> app.appIconColor?.let { app.packageName to it } }.toMap(),
            )
            val updated = installed.filter { it.id != bundle.id } + bundle

            registryPrefs?.let { BundleRegistry.write(it, updated) }
            BundleRegistry.write(localPrefs, updated)

            if (previous != null && previous.fileName != fileName) deleteFiles(previous.fileName)
            return bundle
        } finally {
            tmp.delete()
        }
    }

    fun uninstall(bundle: InstalledBundle) {
        val updated = installed.filter { it.id != bundle.id }
        registryPrefs?.let { BundleRegistry.write(it, updated) }
        BundleRegistry.write(localPrefs, updated)
        deleteFiles(bundle.fileName)
    }

    fun setEnabled(bundle: InstalledBundle, enabled: Boolean) {
        val updated = installed.map {
            if (it.id == bundle.id) it.copy(enabled = enabled) else it
        }
        registryPrefs?.let { BundleRegistry.write(it, updated) }
        BundleRegistry.write(localPrefs, updated)
    }

    /**
     * Installed apps patched by enabled bundles that are not yet in the module scope.
     *
     * Apps that are not installed are left out: the framework asks the user about every requested package.
     */
    fun missingScope(): List<String> {
        val svc = service ?: return emptyList()
        val scope = runCatching { svc.scope.toSet() }.getOrDefault(emptySet())
        return installed.filter { it.enabled }.flatMap { it.apps.keys }.distinct()
            .filter { it !in scope && isInstalled(it) }
    }

    private fun isInstalled(packageName: String) =
        runCatching { context.packageManager.getPackageInfo(packageName, 0) }.isSuccess

    fun requestScope(packages: List<String>, onResult: (String) -> Unit) {
        val svc = service ?: run {
            onResult("LSPosed framework service is currently inactive")
            return
        }
        svc.requestScope(packages, object : XposedService.OnScopeEventListener {
            override fun onScopeRequestApproved(approved: List<String>) =
                onResult("Added to scope: ${approved.joinToString()}")

            override fun onScopeRequestFailed(message: String) = onResult(message)
        })
    }

    /** The patches of an installed bundle, loaded from the manager's copy of the bundle. */
    fun patches(bundle: InstalledBundle): List<AppPatchInfo> {
        val local = File(localDir, bundle.fileName)
        if (!local.exists()) {
            val svc = service ?: throw FileNotFoundException("Bundle ${bundle.name} file missing and service inactive")
            svc.openRemoteFile(bundle.fileName).use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { input ->
                    local.outputStream().use { input.copyTo(it) }
                }
            }
        }
        val manifest = BundleManifest.read(context.packageManager, local)
        return loadApps(local, manifest.entryClass)
    }

    private fun loadApps(apk: File, entryClass: String): List<AppPatchInfo> =
        loadedApps.getOrPut(apk.name) {
            BundleClassLoader.load(apk, entryClass, BundleParentClassLoader(BundleManager::class.java.classLoader!!)).second
        }

    private fun deleteFiles(fileName: String) {
        service?.let { runCatching { it.deleteRemoteFile(fileName) } }
        File(localDir, fileName).delete()
        loadedApps.remove(fileName)
    }

    private companion object {
        /** Bundles are loaded at most once per manager process; their file names change with every update. */
        val loadedApps = mutableMapOf<String, List<AppPatchInfo>>()
    }
}
