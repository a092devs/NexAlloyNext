package io.github.nexalloy.updates

/**
 * Pure preference-name helpers, kept separate from [AppSettings] so they can be unit tested
 * without an Android Context.
 */
object AppPreferenceNames {
    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    /** Design system options: AndroidX Material 3 and the official Miuix library. */
    const val THEME_STYLE_MATERIAL3 = "material3"
    const val THEME_STYLE_MIUIX = "miuix"

    /**
     * Historical value of the Miuix style. Early builds stored "miuix_glass"; it is migrated on
     * read so users keep their selected design system.
     */
    const val LEGACY_THEME_STYLE_MIUIX = "miuix_glass"

    /** The Miuix floating navigation bar is opt-in and disabled by default. */
    const val DEFAULT_FLOATING_NAVIGATION = false

    /**
     * Colour scheme options. "default" keeps each design system's own palette (platform dynamic
     * colour on Material 3 where supported, the official Miuix palette on Miuix); the remaining
     * values are fixed accent seeds applied to both design systems.
     */
    const val COLOR_SCHEME_DEFAULT = "default"
    const val COLOR_SCHEME_VIOLET = "violet"
    const val COLOR_SCHEME_EMERALD = "emerald"
    const val COLOR_SCHEME_AMBER = "amber"
    const val COLOR_SCHEME_ROSE = "rose"
    const val COLOR_SCHEME_OCEAN = "ocean"

    /** Preference order of the colour-scheme dialog. */
    val COLOR_SCHEMES = listOf(
        COLOR_SCHEME_DEFAULT,
        COLOR_SCHEME_VIOLET,
        COLOR_SCHEME_EMERALD,
        COLOR_SCHEME_AMBER,
        COLOR_SCHEME_ROSE,
        COLOR_SCHEME_OCEAN,
    )

    /** Maps any stored colour-scheme value to a supported one. */
    fun normalizeColorScheme(raw: String?): String =
        if (raw != null && raw in COLOR_SCHEMES) raw else COLOR_SCHEME_DEFAULT

    /** Maps any stored theme-style value to a supported one. */
    fun normalizeStyle(raw: String?): String = when (raw) {
        THEME_STYLE_MIUIX, LEGACY_THEME_STYLE_MIUIX -> THEME_STYLE_MIUIX
        else -> THEME_STYLE_MATERIAL3
    }

    /** Maps any stored colour-mode value to a supported one. */
    fun normalizeTheme(raw: String?): String = when (raw) {
        THEME_LIGHT -> THEME_LIGHT
        THEME_DARK -> THEME_DARK
        else -> THEME_SYSTEM
    }

    fun isMiuix(style: String?): Boolean = normalizeStyle(style) == THEME_STYLE_MIUIX
}
