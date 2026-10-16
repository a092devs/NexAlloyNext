package io.github.nexalloy.core.compatibility

import io.github.nexalloy.AppTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class CompatibilityEngineTest {

    @Test
    fun recommendedVersionResolution() {
        // Empty targets -> null
        assertNull(CompatibilityEngine.recommendedVersion(emptyList()))

        // Multiple stable targets -> highest version returned
        val targets = listOf(
            AppTarget("21.16.256", experimental = false),
            AppTarget("21.20.405", experimental = false),
            AppTarget("20.10.100", experimental = false),
        )
        assertEquals("21.20.405", CompatibilityEngine.recommendedVersion(targets))

        // Stable target preferred over newer experimental target
        val targetsWithExp = listOf(
            AppTarget("21.16.256", experimental = false),
            AppTarget("21.20.405", experimental = true),
        )
        assertEquals("21.16.256", CompatibilityEngine.recommendedVersion(targetsWithExp))

        // Only experimental targets -> highest experimental returned
        val allExp = listOf(
            AppTarget("21.16.256", experimental = true),
            AppTarget("21.20.405", experimental = true),
        )
        assertEquals("21.20.405", CompatibilityEngine.recommendedVersion(allExp))
    }

    @Test
    fun notInstalledStatus() {
        val targets = listOf(
            AppTarget("21.16.256", experimental = false),
            AppTarget("21.20.405", experimental = false),
        )
        val statusNull = CompatibilityEngine.resolve(null, targets)
        assertIs<CompatibilityStatus.NotInstalled>(statusNull)
        assertEquals("21.20.405", statusNull.recommendedVersion)

        val statusBlank = CompatibilityEngine.resolve("   ", targets)
        assertIs<CompatibilityStatus.NotInstalled>(statusBlank)
    }

    @Test
    fun dynamicHookingStatus() {
        // Dynamic bytecode hooking when targets list is empty (e.g. Spotify, Google Photos)
        val status = CompatibilityEngine.resolve("8.9.18.512", emptyList())
        assertIs<CompatibilityStatus.Dynamic>(status)
    }

    @Test
    fun exactMatchCompatible() {
        val targets = listOf(
            AppTarget("21.16.256", experimental = false),
            AppTarget("21.20.405", experimental = false),
            AppTarget("21.25.100", experimental = true),
        )

        val statusStable = CompatibilityEngine.resolve("21.20.405", targets)
        assertIs<CompatibilityStatus.Compatible>(statusStable)
        assertEquals("21.20.405", statusStable.targetVersion)
        assertEquals(false, statusStable.isExperimental)

        // Trailing zero equivalence: "21.20.405" should match "21.20.4050" if parsed, but with v prefix:
        val statusPrefix = CompatibilityEngine.resolve("v21.20.405", targets)
        assertIs<CompatibilityStatus.Compatible>(statusPrefix)

        val statusExp = CompatibilityEngine.resolve("21.25.100", targets)
        assertIs<CompatibilityStatus.Compatible>(statusExp)
        assertEquals(true, statusExp.isExperimental)
    }

    @Test
    fun newerThanHighestTargetIsUntested() {
        val targets = listOf(
            AppTarget("21.16.256", experimental = false),
            AppTarget("21.20.405", experimental = false),
        )

        // YouTube 21.41.525 installed when highest tested is 21.20.405
        val status = CompatibilityEngine.resolve("21.41.525", targets)
        assertIs<CompatibilityStatus.CompatibleUntested>(status)
        assertEquals("21.41.525", status.installedVersion)
        assertEquals("21.20.405", status.highestVerified)
    }

    @Test
    fun olderThanLowestTargetIsIncompatible() {
        val targets = listOf(
            AppTarget("20.0.0", experimental = false),
            AppTarget("21.0.0", experimental = false),
        )

        val status = CompatibilityEngine.resolve("19.5.0", targets)
        assertIs<CompatibilityStatus.Incompatible>(status)
    }

    @Test
    fun intermediateUntrackedVersionIsUntested() {
        val targets = listOf(
            AppTarget("20.0.0", experimental = false),
            AppTarget("22.0.0", experimental = false),
        )

        val status = CompatibilityEngine.resolve("21.0.0", targets)
        assertIs<CompatibilityStatus.CompatibleUntested>(status)
        assertEquals("21.0.0", status.installedVersion)
        assertEquals("22.0.0", status.highestVerified)
    }
}

