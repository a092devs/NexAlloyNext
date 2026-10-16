package io.github.nexalloy.updates

import android.content.Context

/** NexAlloy's own settings, in its private preferences. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    var notifyAppUpdates: Boolean
        get() = prefs.getBoolean("notify_app_updates", true)
        set(value) = prefs.edit().putBoolean("notify_app_updates", value).apply()

    var notifyPatchUpdates: Boolean
        get() = prefs.getBoolean("notify_patch_updates", true)
        set(value) = prefs.edit().putBoolean("notify_patch_updates", value).apply()

    /**
     * Selected colour mode: "system", "light", or "dark".
     */
    var theme: String
        get() = AppPreferenceNames.normalizeTheme(prefs.getString("app_theme", null))
        set(value) = prefs.edit().putString("app_theme", AppPreferenceNames.normalizeTheme(value)).apply()

    /**
     * Selected design system: "material3" or "miuix".
     *
     * The legacy "miuix_glass" value is migrated on read so the selection survives.
     */
    var themeStyle: String
        get() {
            val stored = prefs.getString("app_theme_style", null)
            val normalized = AppPreferenceNames.normalizeStyle(stored)
            if (stored != null && stored != normalized) {
                prefs.edit().putString("app_theme_style", normalized).apply()
            }
            return normalized
        }
        set(value) = prefs.edit().putString("app_theme_style", AppPreferenceNames.normalizeStyle(value)).apply()

    /**
     * Selected colour scheme: "default" or a fixed accent ("violet", "emerald", "amber",
     * "rose", "ocean"). Applied to both design systems.
     */
    var colorScheme: String
        get() = AppPreferenceNames.normalizeColorScheme(prefs.getString("app_color_scheme", null))
        set(value) = prefs.edit()
            .putString("app_color_scheme", AppPreferenceNames.normalizeColorScheme(value))
            .apply()

    /**
     * Miuix-only: show the floating bottom navigation bar instead of the standard navigation bar.
     * Disabled by default; only consulted while the Miuix design system is selected.
     */
    var miuixFloatingNavigation: Boolean
        get() = prefs.getBoolean("miuix_floating_navigation", AppPreferenceNames.DEFAULT_FLOATING_NAVIGATION)
        set(value) = prefs.edit().putBoolean("miuix_floating_navigation", value).apply()

    /** Version last notified about per update key, so every version notifies once. */
    fun lastNotified(key: String): String? = prefs.getString("notified_$key", null)

    fun setLastNotified(key: String, version: String) =
        prefs.edit().putString("notified_$key", version).apply()

    companion object {
        const val THEME_SYSTEM = AppPreferenceNames.THEME_SYSTEM
        const val THEME_LIGHT = AppPreferenceNames.THEME_LIGHT
        const val THEME_DARK = AppPreferenceNames.THEME_DARK

        const val THEME_STYLE_MATERIAL3 = AppPreferenceNames.THEME_STYLE_MATERIAL3
        const val THEME_STYLE_MIUIX = AppPreferenceNames.THEME_STYLE_MIUIX

        const val COLOR_SCHEME_DEFAULT = AppPreferenceNames.COLOR_SCHEME_DEFAULT
    }
}
