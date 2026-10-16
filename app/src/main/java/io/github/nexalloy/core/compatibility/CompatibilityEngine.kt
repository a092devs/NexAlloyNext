package io.github.nexalloy.core.compatibility

import io.github.nexalloy.AppTarget

/**
 * Authoritative compatibility status of an application relative to available patch bundles.
 */
sealed class CompatibilityStatus {
    /** Pinned, tested, and verified compatible version. */
    data class Compatible(
        val targetVersion: String,
        val isExperimental: Boolean = false,
    ) : CompatibilityStatus()

    /** Newer version than tested, or between tested ranges with no known breakages. */
    data class CompatibleUntested(
        val installedVersion: String,
        val highestVerified: String?,
    ) : CompatibilityStatus()

    /** Version-agnostic dynamic bytecode matching (e.g. Spotify, Google Photos). */
    data class Dynamic(
        val note: String = "Universal dynamic hook",
    ) : CompatibilityStatus()

    /** Known incompatible, deprecated, or below minimum threshold. */
    data class Incompatible(
        val reason: String,
    ) : CompatibilityStatus()

    /** Target application is supported, but no patch bundle is installed/enabled. */
    data object MissingBundle : CompatibilityStatus()

    /** Application is supported by bundles, but not currently installed on the device. */
    data class NotInstalled(
        val recommendedVersion: String?,
    ) : CompatibilityStatus()

    /** Metadata insufficient to determine compatibility. */
    data class Unknown(
        val reason: String,
    ) : CompatibilityStatus()
}

/**
 * Centralized engine that evaluates compatibility between installed apps and patch bundles.
 */
object CompatibilityEngine {

    /**
     * Resolves the authoritative compatibility status for an application.
     *
     * @param installedVersion Version name of the installed app, or null if not installed.
     * @param targets The list of tested [AppTarget] entries from all applicable bundles.
     */
    fun resolve(
        installedVersion: String?,
        targets: List<AppTarget>,
    ): CompatibilityStatus {
        if (installedVersion.isNullOrBlank()) {
            return CompatibilityStatus.NotInstalled(recommendedVersion(targets))
        }

        // Apps like Spotify or Google Photos that declare no specific targets hook dynamically.
        if (targets.isEmpty()) {
            return CompatibilityStatus.Dynamic()
        }

        // Exact match with an officially verified target (stable or experimental).
        val exactMatch = targets.firstOrNull { VersionComparator.isEqual(it.version, installedVersion) }
        if (exactMatch != null) {
            return CompatibilityStatus.Compatible(exactMatch.version, exactMatch.experimental)
        }

        val highest = targets.maxWithOrNull { a, b -> VersionComparator.compare(a.version, b.version) }
        val lowest = targets.minWithOrNull { a, b -> VersionComparator.compare(a.version, b.version) }

        // If newer than the highest tested target, it is untested on this specific build.
        if (highest != null && VersionComparator.compare(installedVersion, highest.version) > 0) {
            return CompatibilityStatus.CompatibleUntested(installedVersion, highest.version)
        }

        // If older than the lowest tested target, it lacks required features or symbols.
        if (lowest != null && VersionComparator.compare(installedVersion, lowest.version) < 0) {
            return CompatibilityStatus.Incompatible(
                "Installed version $installedVersion is older than minimum supported target ${lowest.version}"
            )
        }

        // Fallback for intermediate untracked versions between lowest and highest.
        return CompatibilityStatus.CompatibleUntested(installedVersion, highest?.version)
    }

    /**
     * Computes the recommended target version from a list of targets.
     * Always prefers verified stable versions, sorted by numeric segment comparison.
     * Falls back to the newest experimental target if no stable targets exist.
     */
    fun recommendedVersion(targets: List<AppTarget>): String? {
        if (targets.isEmpty()) return null
        val stable = targets.filter { !it.experimental }
        val pool = stable.ifEmpty { targets }
        return pool.maxWithOrNull { a, b -> VersionComparator.compare(a.version, b.version) }?.version
    }
}

