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

    data class KnownSupportedApp(
        val packageName: String,
        val appName: String,
        val defaultBundleName: String,
        val appIconColor: Int?,
        val targets: List<AppTarget> = emptyList(),
    )

    val KNOWN_SUPPORTED_APPS = listOf(
        KnownSupportedApp(
            packageName = "com.google.android.youtube",
            appName = "YouTube",
            defaultBundleName = "NexFusion Patches",
            appIconColor = 0xFF0033,
            targets = listOf(
                AppTarget("21.43.523"),
                AppTarget("21.42.243"),
                AppTarget("21.41.525"),
                AppTarget("21.40.161"),
                AppTarget("21.39.522"),
            ),
        ),
        KnownSupportedApp(
            packageName = "com.google.android.apps.youtube.music",
            appName = "YouTube Music",
            defaultBundleName = "NexFusion Patches",
            appIconColor = 0xFF0000,
            targets = listOf(
                AppTarget("9.41.52"),
                AppTarget("9.40.51"),
                AppTarget("9.39.52"),
                AppTarget("9.33.00"),
            ),
        ),
        KnownSupportedApp(
            packageName = "com.reddit.frontpage",
            appName = "Reddit",
            defaultBundleName = "NexFusion Patches",
            appIconColor = 0xFF4500,
            targets = listOf(
                AppTarget("2026.43.1"),
                AppTarget("2026.43.0"),
                AppTarget("2026.42.0"),
                AppTarget("2026.41.0"),
                AppTarget("2026.38.0"),
                AppTarget("2026.37.0"),
                AppTarget("2026.36.0"),
                AppTarget("2026.24.0"),
                AppTarget("2026.14.0"),
            ),
        ),
        KnownSupportedApp(
            packageName = "com.google.android.apps.photos",
            appName = "Google Photos",
            defaultBundleName = "NexFusion Patches",
            appIconColor = 0x4285F4,
        ),
        KnownSupportedApp(
            packageName = "ch.protonvpn.android",
            appName = "Proton VPN",
            defaultBundleName = "NexFusion Patches",
            appIconColor = 0x6D4AFF,
            targets = listOf(
                AppTarget("5.20.98.0"),
                AppTarget("5.20.74.0"),
                AppTarget("5.20.57.0"),
                AppTarget("5.20.39.0"),
                AppTarget("5.19.72.0"),
                AppTarget("5.19.43.0"),
            ),
        ),
        KnownSupportedApp(
            packageName = "com.google.android.inputmethod.latin",
            appName = "Gboard",
            defaultBundleName = "NexFusion Patches",
            appIconColor = 0x4285F4,
            targets = listOf(
                AppTarget("18.5.4.993608717-release-arm64-v8a"),
                AppTarget("18.5.3.993608717-release-arm64-v8a"),
                AppTarget("18.4.1.985164140-release-arm64-v8a"),
                AppTarget("18.4.1.985164140-beta-arm64-v8a"),
                AppTarget("18.3.1.977415014-release-arm64-v8a"),
            ),
        ),
        KnownSupportedApp(
            packageName = "com.google.android.gm",
            appName = "Gmail",
            defaultBundleName = "NexFusion Patches",
            appIconColor = 0xEA4335,
        ),
        KnownSupportedApp(
            packageName = "com.google.android.googlequicksearchbox",
            appName = "Google (Discover)",
            defaultBundleName = "NexFusion Patches",
            appIconColor = 0x4285F4,
        ),
        KnownSupportedApp(
            packageName = "com.facebook.katana",
            appName = "Facebook",
            defaultBundleName = "NexFusion Patches",
            appIconColor = 0x0866FF,
            targets = listOf(
                AppTarget("584.0.0.51.56"),
                AppTarget("583.0.0.39.55"),
                AppTarget("582.0.0.50.54"),
                AppTarget("581.0.0.45.54"),
            ),
        ),
        KnownSupportedApp(
            packageName = "com.discord",
            appName = "Discord",
            defaultBundleName = "NexFusion Patches",
            appIconColor = 0x5865F2,
            targets = listOf(
                AppTarget("351.11 - Stable"),
                AppTarget("351.11"),
                AppTarget("350.8 - Stable"),
                AppTarget("350.8"),
                AppTarget("349.9 - Stable"),
                AppTarget("349.9"),
                AppTarget("348.10"),
            ),
        ),
        KnownSupportedApp(
            packageName = "com.linkedin.android",
            appName = "LinkedIn",
            defaultBundleName = "NexFusion Patches",
            appIconColor = 0x0A66C2,
            targets = listOf(
                AppTarget("4.1.1266"),
                AppTarget("4.1.1264"),
                AppTarget("4.1.1263"),
                AppTarget("4.1.1261"),
                AppTarget("4.1.1259"),
                AppTarget("4.1.1255.1"),
            ),
        ),
        KnownSupportedApp(
            packageName = "com.mixplorer",
            appName = "MiXplorer",
            defaultBundleName = "NexFusion Patches",
            appIconColor = null,
        ),
        KnownSupportedApp(
            packageName = "com.spotify.music",
            appName = "Spotify",
            defaultBundleName = "NexFusion Patches",
            appIconColor = 0x1ED760,
        ),
        KnownSupportedApp(
            packageName = "com.instagram.android",
            appName = "Instagram",
            defaultBundleName = "NexFusion Patches",
            appIconColor = 0xE1306C,
            targets = listOf(
                AppTarget("453.0.0.57.79"),
                AppTarget("453.0.0.55.79"),
                AppTarget("452.1.0.53.64"),
                AppTarget("452.0.0.48.64"),
                AppTarget("451.0.0.53.80"),
                AppTarget("450.0.0.46.79"),
                AppTarget("447.0.0.55.81"),
            ),
        ),
        KnownSupportedApp(
            packageName = "com.twitter.android",
            appName = "X",
            defaultBundleName = "NexFusion Patches",
            appIconColor = 0x000000,
            targets = listOf(
                AppTarget("12.38.0-prod.01"),
                AppTarget("12.37.2-prod.01"),
                AppTarget("12.36.1-prod.01"),
                AppTarget("12.35.0-prod.01"),
            ),
        ),
        KnownSupportedApp(
            packageName = "org.telegram.messenger",
            appName = "Telegram",
            defaultBundleName = "NexFusion Patches",
            appIconColor = 0x2AABEE,
            targets = listOf(
                AppTarget("13.1.1"),
                AppTarget("13.1.0"),
                AppTarget("12.10.6"),
            ),
        ),
        KnownSupportedApp(
            packageName = "org.telegram.messenger.web",
            appName = "Telegram (Web)",
            defaultBundleName = "NexFusion Patches",
            appIconColor = 0x2AABEE,
            targets = listOf(
                AppTarget("13.1.1"),
                AppTarget("13.1.0"),
                AppTarget("12.10.6"),
            ),
        ),
    )

    /**
     * The display names of [apps] (package name to display name) whose packages are present on
     * this device, sorted for display. Cards describing a bundle use this, so a bundle stops
     * naming apps that are not installed; [isInstalled] is a parameter so the rule is testable.
     */
    fun installedAppNames(apps: Map<String, String>, isInstalled: (String) -> Boolean): List<String> =
        apps.filterKeys { isInstalled(it) }.values.distinct().sortedBy { it.lowercase() }

    /**
     * Discovers all target applications from enabled bundles across enabled sources,
     * combining them with live package information and the supported app catalog.
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

        // Also ensure all known supported applications are present in the catalog
        for (known in KNOWN_SUPPORTED_APPS) {
            if (!packageMap.containsKey(known.packageName)) {
                packageMap[known.packageName] = known.appName
            }
        }

        return packageMap.map { (packageName, declaredName) ->
            val live = queryPackage(context, packageName)
            val matchingBundles = activeBundles.filter { packageName in it.apps }
            val known = KNOWN_SUPPORTED_APPS.find { it.packageName == packageName }

            val allTargets: List<AppTarget> = if (matchingBundles.isNotEmpty()) {
                matchingBundles.flatMap { it.targets[packageName].orEmpty() }
            } else {
                known?.targets.orEmpty()
            }

            val compatibility = if (matchingBundles.isNotEmpty()) {
                CompatibilityEngine.resolve(live.versionName, allTargets)
            } else if (!live.isInstalled) {
                CompatibilityStatus.NotInstalled(CompatibilityEngine.recommendedVersion(allTargets))
            } else {
                CompatibilityStatus.MissingBundle
            }

            val recommended = CompatibilityEngine.recommendedVersion(allTargets)
            val brandColor = matchingBundles.firstNotNullOfOrNull { it.appIconColors[packageName] } ?: known?.appIconColor

            val sourceNames = matchingBundles.mapNotNull { b ->
                sources.find { it.id == b.sourceId }?.name ?: b.sourceId
            }.distinct().ifEmpty {
                listOfNotNull(known?.defaultBundleName)
            }

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
