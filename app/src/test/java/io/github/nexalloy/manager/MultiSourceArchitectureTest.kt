package io.github.nexalloy.manager

import io.github.nexalloy.AppTarget
import io.github.nexalloy.core.InstalledBundle
import io.github.nexalloy.core.discovery.AppDiscovery
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MultiSourceArchitectureTest {

    @Test
    fun sourceProvenanceDetection() {
        assertEquals(SourceProvenance.OFFICIAL, SourceProvenance.fromUrl("MorpheApp/morphe-patches"))
        assertEquals(SourceProvenance.OFFICIAL, SourceProvenance.fromUrl("https://github.com/MorpheApp/morphe-manager"))
        assertEquals(SourceProvenance.NEXALLOY, SourceProvenance.fromUrl("a092devs/NexAlloy-Patches"))
        assertEquals(SourceProvenance.NEXALLOY, SourceProvenance.fromUrl("a092devs/NexAlloyNext-Patches"))
        assertEquals(SourceProvenance.NEXALLOY, SourceProvenance.fromUrl("https://github.com/a092devs/NexAlloyNext"))
        assertEquals(SourceProvenance.COMMUNITY, SourceProvenance.fromUrl("2eno/NexAlloyNext-Patches"))
        assertEquals(SourceProvenance.COMMUNITY, SourceProvenance.fromUrl("https://github.com/crimera/piko"))
        assertEquals(SourceProvenance.COMMUNITY, SourceProvenance.fromUrl("hoo-dles/morphe-patches"))
        assertEquals(SourceProvenance.CUSTOM, SourceProvenance.fromUrl("random-dev/my-android-patches"))
        assertEquals(SourceProvenance.CUSTOM, SourceProvenance.fromUrl("https://example.com/custom-bundle.json"))
    }

    @Test
    fun patchSourceSerializationWithNewFields() {
        val original = PatchSource(
            id = "src_123",
            name = "Community Piko",
            url = "crimera/piko",
            prerelease = true,
            enabled = false,
            provenance = SourceProvenance.COMMUNITY,
            installedVersion = "1.1.7",
            lastRefreshTime = 1729000000000L,
            lastError = "HTTP 403 Rate limited",
        )

        val json = original.toJson()
        val restored = PatchSource.fromJson(json)

        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.url, restored.url)
        assertEquals(original.prerelease, restored.prerelease)
        assertEquals(original.enabled, restored.enabled)
        assertEquals(original.provenance, restored.provenance)
        assertEquals(original.installedVersion, restored.installedVersion)
        assertEquals(original.lastRefreshTime, restored.lastRefreshTime)
        assertEquals(original.lastError, restored.lastError)
    }

    @Test
    fun sourceReleaseSerialization() {
        val release = SourceRelease(
            version = "1.0.5",
            changelog = "Added YouTube fixes and hardened hooks",
            pageUrl = "https://github.com/a092devs/NexAlloyNext-Patches/releases/tag/v1.0.5",
            bundles = listOf(
                "nexalloy-1.0.5.nxb" to "https://github.com/download/nexalloy.nxb",
                "nexfusion-3.2.0.nxb" to "https://github.com/download/nexfusion.nxb",
            ),
        )

        val json = release.toJson()
        val restored = SourceRelease.fromJson(json)

        assertEquals("1.0.5", restored.version)
        assertEquals("Added YouTube fixes and hardened hooks", restored.changelog)
        assertEquals("https://github.com/a092devs/NexAlloyNext-Patches/releases/tag/v1.0.5", restored.pageUrl)
        assertEquals(2, restored.bundles.size)
        assertEquals("nexalloy-1.0.5.nxb", restored.bundles[0].first)
        assertEquals("https://github.com/download/nexalloy.nxb", restored.bundles[0].second)
    }

    @Test
    fun curatedPresetSourcesIntegrity() {
        val presets = PatchSource.PRESET_SOURCES
        assertTrue(presets.isNotEmpty())

        // Check for unique IDs
        val ids = presets.map { it.id }
        assertEquals(ids.size, ids.distinct().size)

        // Ensure default NexAlloy is in presets
        assertNotNull(presets.find { it.id == "default" })
        assertEquals(SourceProvenance.NEXALLOY, PatchSource.default.provenance)
    }

    @Test
    fun disabledSourceExcludesBundlesInCatalog() {
        val source1 = PatchSource("src1", "NexAlloy", "a092devs/NexAlloyNext-Patches", enabled = true)
        val source2 = PatchSource("src2", "Disabled Source", "custom/repo", enabled = false)
        val sources = listOf(source1, source2)

        val bundle1 = InstalledBundle(
            id = "bundle.active",
            name = "Active Bundle",
            version = "1.0.0",
            versionCode = 1L,
            author = "NexAlloy",
            description = "Active",
            fileName = "bundle1.apk",
            enabled = true,
            sourceId = "src1",
            apps = mapOf("com.google.android.youtube" to "YouTube"),
            targets = mapOf("com.google.android.youtube" to listOf(AppTarget("21.20.405", false))),
        )

        val bundle2 = InstalledBundle(
            id = "bundle.disabled_source",
            name = "Disabled Source Bundle",
            version = "1.0.0",
            versionCode = 1L,
            author = "Other",
            description = "From disabled source",
            fileName = "bundle2.apk",
            enabled = true,
            sourceId = "src2",
            apps = mapOf("com.spotify.music" to "Spotify"),
        )

        val enabledSourceIds = sources.filter { it.enabled }.map { it.id }.toSet()
        val activeBundles = listOf(bundle1, bundle2).filter { bundle ->
            bundle.enabled && (bundle.sourceId == null || bundle.sourceId in enabledSourceIds)
        }

        assertEquals(1, activeBundles.size)
        assertEquals("bundle.active", activeBundles.first().id)
        assertFalse(activeBundles.any { it.id == "bundle.disabled_source" })
    }

    @Test
    fun multiSourceConflictDetection() {
        val bundleA = InstalledBundle(
            id = "bundle.a",
            name = "NexAlloy YouTube",
            version = "1.0.5",
            versionCode = 105L,
            author = "Author A",
            description = "Source A YouTube",
            fileName = "bundleA.apk",
            enabled = true,
            sourceId = "srcA",
            apps = mapOf("com.google.android.youtube" to "YouTube"),
        )

        val bundleB = InstalledBundle(
            id = "bundle.b",
            name = "Community YouTube",
            version = "2.0.0",
            versionCode = 200L,
            author = "Author B",
            description = "Source B YouTube",
            fileName = "bundleB.apk",
            enabled = true,
            sourceId = "srcB",
            apps = mapOf("com.google.android.youtube" to "YouTube"),
        )

        val bundlesForApp = listOf(bundleA, bundleB).filter { "com.google.android.youtube" in it.apps }
        val hasConflicts = bundlesForApp.size > 1
        assertTrue(hasConflicts)

        val singleBundleApp = listOf(bundleA).filter { "com.google.android.youtube" in it.apps }
        assertFalse(singleBundleApp.size > 1)
    }
}

