package io.github.nexalloy.core.discovery

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.nexalloy.AppTarget
import io.github.nexalloy.core.InstalledBundle
import io.github.nexalloy.core.compatibility.CompatibilityEngine
import io.github.nexalloy.core.compatibility.CompatibilityStatus
import io.github.nexalloy.manager.PatchSource

/**
 * Package metadata queried live from Android's PackageManager.
 */
data class PackageLiveInfo(
    val packageName: String,
    val appLabel: String,
    val versionName: String?,
    val versionCode: Long,
    val isInstalled: Boolean,
)

/**
 * Consolidated UI and business model representing an application in NexAlloy.
 */
data class AppItemState(
    val packageName: String,
    val appName: String,
    val isInstalled: Boolean,
    val installedVersionName: String?,
    val installedVersionCode: Long?,
    val recommendedVersion: String?,
    val appIconColor: Int?,
    val compatibility: CompatibilityStatus,
    val inScope: Boolean,
    val bundles: List<InstalledBundle>,
    /** Names of configured sources providing bundles for this application. */
    val sourceNames: List<String> = emptyList(),
    /** Indicates whether multiple bundles provide patches for this application, causing potential conflicts. */
    val hasConflicts: Boolean = false,
)

object AppDiscovery {

    /**
     * Inspects the device PackageManager to retrieve live package details.
     */
    fun queryPackage(context: Context, packageName: String): PackageLiveInfo {
        val pm = context.packageManager
        return try {
            val info = pm.getPackageInfo(packageName, 0)
            val appInfo = info.applicationInfo
            val label = appInfo?.loadLabel(pm)?.toString() ?: packageName
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION") info.versionCode.toLong()
            }
            PackageLiveInfo(
                packageName = packageName,
                appLabel = label,
                versionName = info.versionName,
                versionCode = versionCode,
                isInstalled = true,
            )
        } catch (_: PackageManager.NameNotFoundException) {
            PackageLiveInfo(
                packageName = packageName,
                appLabel = packageName,
                versionName = null,
                versionCode = 0L,
                isInstalled = false,
            )
        }
    }

    /**
     * Loads the application icon as an ImageBitmap, or null if uninstalled/unavailable.
     */
    fun loadAppIcon(context: Context, packageName: String): ImageBitmap? = runCatching {
        val pm = context.packageManager
        val drawable = pm.getApplicationIcon(packageName)
        val bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, 192, 192)
        drawable.draw(canvas)
        bitmap.asImageBitmap()
    }.getOrNull()

    /**
     * Discovers all target applications from enabled bundles across enabled sources,
     * combining them with live package information.
     */
    fun buildCatalog(
        context: Context,
        bundles: List<InstalledBundle>,
        missingScope: Set<String>,
        sources: List<PatchSource> = emptyList(),
    ): List<AppItemState> {
        val enabledSourceIds = sources.filter { it.enabled }.map { it.id }.toSet()
        // A bundle is active only if enabled AND its source (if assigned) is enabled
        val activeBundles = bundles.filter { bundle ->
            bundle.enabled && (bundle.sourceId == null || sources.isEmpty() || bundle.sourceId in enabledSourceIds)
        }

        // Aggregate all target package names declared by active bundles
        val packageMap = mutableMapOf<String, String>()
        for (bundle in activeBundles) {
            for ((pkg, name) in bundle.apps) {
                if (!packageMap.containsKey(pkg) || packageMap[pkg] == pkg) {
                    packageMap[pkg] = name
                }
            }
        }

        return packageMap.map { (packageName, declaredName) ->
            val live = queryPackage(context, packageName)
            val matchingBundles = activeBundles.filter { packageName in it.apps }
            val allTargets: List<AppTarget> = matchingBundles.flatMap { it.targets[packageName].orEmpty() }

            val compatibility = CompatibilityEngine.resolve(live.versionName, allTargets)
            val recommended = CompatibilityEngine.recommendedVersion(allTargets)
            val brandColor = matchingBundles.firstNotNullOfOrNull { it.appIconColors[packageName] }

            val sourceNames = matchingBundles.mapNotNull { b ->
                sources.find { it.id == b.sourceId }?.name ?: b.sourceId
            }.distinct()

            val hasConflicts = matchingBundles.size > 1

            AppItemState(
                packageName = packageName,
                appName = if (live.isInstalled && live.appLabel.isNotBlank() && live.appLabel != packageName) {
                    live.appLabel
                } else {
                    declaredName
                },
                isInstalled = live.isInstalled,
                installedVersionName = live.versionName,
                installedVersionCode = live.versionCode,
                recommendedVersion = recommended,
                appIconColor = brandColor,
                compatibility = compatibility,
                inScope = !missingScope.contains(packageName),
                bundles = matchingBundles,
                sourceNames = sourceNames,
                hasConflicts = hasConflicts,
            )
        }.sortedWith(compareBy({ !it.isInstalled }, { it.appName.lowercase() }))
    }
}
