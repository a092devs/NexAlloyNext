package io.github.nexalloy.core.compatibility

/**
 * Robust, semantic version comparison engine for Android application version strings.
 *
 * Handles:
 * - Dotted numeric versions of arbitrary length (e.g. "21.41.525", "447.0.0.55.81", "2026.41.0")
 * - Trailing zeros equivalence ("20.22.00" == "20.22.0")
 * - Prefixed "v" or "V" ("v21.41.525" == "21.41.525")
 * - Multi-digit segment comparisons numerically (not lexicographically, so 10 > 9)
 * - Suffixes like "-release.0", "-alpha", etc.
 */
object VersionComparator : Comparator<String?> {

    override fun compare(a: String?, b: String?): Int {
        if (a === b) return 0
        if (a == null) return -1
        if (b == null) return 1

        val cleanA = cleanVersion(a)
        val cleanB = cleanVersion(b)
        if (cleanA == cleanB) return 0

        val (segmentsA, suffixA) = parseVersion(cleanA)
        val (segmentsB, suffixB) = parseVersion(cleanB)

        // Compare numeric segments
        val maxLen = maxOf(segmentsA.size, segmentsB.size)
        for (i in 0 until maxLen) {
            val numA = segmentsA.getOrElse(i) { 0L }
            val numB = segmentsB.getOrElse(i) { 0L }
            val cmp = numA.compareTo(numB)
            if (cmp != 0) return cmp
        }

        // Both numeric parts are equal. Handle suffix if present.
        // A release without suffix is considered newer than a prerelease with suffix.
        if (suffixA.isEmpty() && suffixB.isNotEmpty()) return 1
        if (suffixA.isNotEmpty() && suffixB.isEmpty()) return -1
        return suffixA.compareTo(suffixB, ignoreCase = true)
    }

    fun isAtLeast(version: String?, minVersion: String?): Boolean =
        compare(version, minVersion) >= 0

    fun isAtMost(version: String?, maxVersion: String?): Boolean =
        compare(version, maxVersion) <= 0

    fun isEqual(a: String?, b: String?): Boolean =
        compare(a, b) == 0

    private fun cleanVersion(raw: String): String =
        raw.trim().removePrefix("v").removePrefix("V").trim()

    private fun parseVersion(version: String): Pair<List<Long>, String> {
        val dashIndex = version.indexOfAny(charArrayOf('-', '_'))
        val (numericPart, suffix) = if (dashIndex >= 0) {
            version.substring(0, dashIndex) to version.substring(dashIndex)
        } else {
            version to ""
        }

        val segments = numericPart.split('.')
            .mapNotNull { it.trim().toLongOrNull() }
            .toMutableList()

        // Normalize trailing zeros: e.g. [20, 22, 0, 0] -> [20, 22]
        while (segments.isNotEmpty() && segments.last() == 0L) {
            segments.removeAt(segments.lastIndex)
        }

        return segments to suffix
    }
}

