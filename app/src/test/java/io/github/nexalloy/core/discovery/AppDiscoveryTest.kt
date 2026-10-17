package io.github.nexalloy.core.discovery

import kotlin.test.Test
import kotlin.test.assertEquals

class AppDiscoveryTest {

    @Test
    fun bundleCardsListOnlyAppsInstalledOnThisDevice() {
        val apps = mapOf(
            "com.instagram.android" to "Instagram",
            "com.spotify.music" to "Spotify",
            "com.google.android.youtube" to "YouTube",
        )
        assertEquals(
            listOf("Instagram", "YouTube"),
            AppDiscovery.installedAppNames(apps) { it != "com.spotify.music" },
        )
    }

    @Test
    fun namesAreSortedAndDeduplicated() {
        val apps = mapOf(
            "zeta.pkg" to "Zeta",
            "alpha.pkg" to "alpha",
            "alpha.other" to "alpha",
        )
        assertEquals(listOf("alpha", "Zeta"), AppDiscovery.installedAppNames(apps) { true })
    }

    @Test
    fun aBundleWithNoInstalledAppsHasNoNames() {
        assertEquals(
            emptyList(),
            AppDiscovery.installedAppNames(mapOf("com.spotify.music" to "Spotify")) { false },
        )
    }
}
