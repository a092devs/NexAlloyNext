package io.github.nexalloy.core.compatibility

import io.github.nexalloy.core.BundleManifest
import io.github.nexalloy.core.IncompatibleBundleException
import io.github.nexalloy.core.InstalledBundle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BundleCompatibilityTest {

    private fun createManifest(
        version: String = "3.2.0",
        series: String = "",
        minManager: String = "",
        maxManager: String = "",
    ) = BundleManifest(
        id = "io.github.nexalloy.patches.nexfusion",
        name = "NexFusion Patches",
        version = version,
        versionCode = 13L,
        entryClass = "io.github.nexalloy.nexfusion.NexFusionPatches",
        apiVersion = 1,
        author = "NexAlloy",
        description = "Test bundle",
        resourcePackageId = 0x4e,
        compatibilitySeries = series,
        minManagerVersion = minManager,
        maxManagerVersion = maxManager,
    )

    @Test
    fun compatibleSeriesMatches() {
        val manifest = createManifest(series = "3.2", minManager = "3.2.0")
        // Should succeed without throwing
        manifest.validateCompatibility("3.2.0")
        manifest.validateCompatibility("3.2.1")
        manifest.validateCompatibility("3.2.9")
    }

    @Test
    fun incompatibleNewerSeriesThrows() {
        val manifest = createManifest(version = "3.3.0", series = "3.3", minManager = "3.3.0")
        val ex = assertFailsWith<IncompatibleBundleException> {
            manifest.validateCompatibility("3.2.0")
        }
        assert(ex.message?.contains("requires NexAlloyNext series 3.3") == true)
    }

    @Test
    fun minManagerVersionEnforced() {
        val manifest = createManifest(minManager = "3.2.5")
        assertFailsWith<IncompatibleBundleException> {
            manifest.validateCompatibility("3.2.0")
        }
        // Exactly at or above minManager passes
        manifest.validateCompatibility("3.2.5")
        manifest.validateCompatibility("3.2.6")
    }

    @Test
    fun maxManagerVersionEnforced() {
        val manifest = createManifest(maxManager = "3.2.5")
        manifest.validateCompatibility("3.2.0")
        manifest.validateCompatibility("3.2.5")
        assertFailsWith<IncompatibleBundleException> {
            manifest.validateCompatibility("3.2.6")
        }
    }

    @Test
    fun emptyCompatibilityMetadataAlwaysPasses() {
        val legacyManifest = createManifest(series = "", minManager = "", maxManager = "")
        legacyManifest.validateCompatibility("3.2.0")
        legacyManifest.validateCompatibility("1.0.0")
        legacyManifest.validateCompatibility("4.0.0")
    }

    @Test
    fun installedBundleCompatibilitySerialization() {
        val bundle = InstalledBundle(
            id = "io.github.nexalloy.patches.nexfusion",
            name = "NexFusion Patches",
            version = "3.2.0",
            versionCode = 13L,
            author = "NexAlloy",
            description = "Test",
            fileName = "nexfusion-1234.apk",
            enabled = true,
            sourceId = "default",
            apps = mapOf("com.instagram.android" to "Instagram"),
            resourcePackageId = 0x4e,
            compatibilitySeries = "3.2",
            minManagerVersion = "3.2.0",
            maxManagerVersion = "",
        )

        val json = bundle.toJson()
        val restored = InstalledBundle.fromJson(json)

        assertEquals("3.2", restored.compatibilitySeries)
        assertEquals("3.2.0", restored.minManagerVersion)
        assertEquals("", restored.maxManagerVersion)
    }
}

