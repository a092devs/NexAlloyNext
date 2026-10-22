package io.github.nexalloy.updates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure behaviour of the persisted appearance preferences, including the legacy Miuix style value
 * migration and the floating-navigation default.
 */
class AppPreferenceNamesTest {

    @Test
    fun supportedStyleValuesArePreserved() {
        assertEquals(AppPreferenceNames.THEME_STYLE_MIUIX, AppPreferenceNames.normalizeStyle("miuix"))
        assertEquals(AppPreferenceNames.THEME_STYLE_MATERIAL3, AppPreferenceNames.normalizeStyle("material3"))
    }

    @Test
    fun legacyMiuixStyleMigrates() {
        // Early builds stored the Miuix selection as "miuix_glass".
        assertEquals(AppPreferenceNames.THEME_STYLE_MIUIX, AppPreferenceNames.normalizeStyle("miuix_glass"))
        assertTrue(AppPreferenceNames.isMiuix("miuix_glass"))
    }

    @Test
    fun unknownStyleValuesFallBackToMiuix() {
        assertEquals(AppPreferenceNames.THEME_STYLE_MIUIX, AppPreferenceNames.normalizeStyle(null))
        assertEquals(AppPreferenceNames.THEME_STYLE_MIUIX, AppPreferenceNames.normalizeStyle(""))
        assertEquals(AppPreferenceNames.THEME_STYLE_MIUIX, AppPreferenceNames.normalizeStyle("glass"))
        assertEquals(AppPreferenceNames.THEME_STYLE_MIUIX, AppPreferenceNames.normalizeStyle("MIUIX"))
    }

    @Test
    fun themeModeValuesArePreserved() {
        assertEquals(AppPreferenceNames.THEME_SYSTEM, AppPreferenceNames.normalizeTheme("system"))
        assertEquals(AppPreferenceNames.THEME_LIGHT, AppPreferenceNames.normalizeTheme("light"))
        assertEquals(AppPreferenceNames.THEME_DARK, AppPreferenceNames.normalizeTheme("dark"))
    }

    @Test
    fun unknownThemeModeFallsBackToSystem() {
        assertEquals(AppPreferenceNames.THEME_SYSTEM, AppPreferenceNames.normalizeTheme(null))
        assertEquals(AppPreferenceNames.THEME_SYSTEM, AppPreferenceNames.normalizeTheme(""))
        assertEquals(AppPreferenceNames.THEME_SYSTEM, AppPreferenceNames.normalizeTheme("Light"))
        assertEquals(AppPreferenceNames.THEME_SYSTEM, AppPreferenceNames.normalizeTheme("auto"))
    }

    @Test
    fun isMiuixMatchesTheSelectedDesignSystem() {
        assertTrue(AppPreferenceNames.isMiuix("miuix"))
        assertFalse(AppPreferenceNames.isMiuix("material3"))
        assertTrue(AppPreferenceNames.isMiuix(null))
    }

    @Test
    fun floatingNavigationDefaultsToEnabled() {
        assertTrue(AppPreferenceNames.DEFAULT_FLOATING_NAVIGATION)
    }

    @Test
    fun persistedStyleIdentifiersAreStable() {
        // These literals are persisted in SharedPreferences; changing them would silently reset
        // the user's selection unless a migration is added.
        assertEquals("miuix", AppPreferenceNames.THEME_STYLE_MIUIX)
        assertEquals("material3", AppPreferenceNames.THEME_STYLE_MATERIAL3)
        assertEquals("miuix_glass", AppPreferenceNames.LEGACY_THEME_STYLE_MIUIX)
    }
}
