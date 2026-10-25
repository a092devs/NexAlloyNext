package io.github.nexalloy.ui.theme

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeTonalSpot
import io.github.nexalloy.updates.AppPreferenceNames
import io.github.nexalloy.updates.AppSettings
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController


enum class ThemeStyle {
    MATERIAL3,
    MIUIX,
}

val LocalThemeStyle = staticCompositionLocalOf { ThemeStyle.MATERIAL3 }

val isMiuixStyle: Boolean
    @Composable
    get() = LocalThemeStyle.current == ThemeStyle.MIUIX

/**
 * Configuration parameters for NexAlloy's adaptive visual theme.
 */
data class NexAlloyThemeConfig(
    /** True while the official Miuix design system is active. */
    val isMiuix: Boolean = false,
    val cardShape: RoundedCornerShape = RoundedCornerShape(16.dp),
    val cardBorder: BorderStroke? = null,
)

val LocalNexAlloyThemeConfig = staticCompositionLocalOf { NexAlloyThemeConfig() }

@Composable
fun nexAlloyCardShape(): RoundedCornerShape = LocalNexAlloyThemeConfig.current.cardShape

@Composable
fun nexAlloyCardBorder(): BorderStroke? = LocalNexAlloyThemeConfig.current.cardBorder

@Composable
fun nexAlloyCardColors(
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
): CardColors {
    val isMiuix = LocalNexAlloyThemeConfig.current.isMiuix
    val actualContainer = if (isMiuix) {
        containerColor.copy(alpha = 0.82f)
    } else {
        containerColor
    }
    return CardDefaults.cardColors(
        containerColor = actualContainer,
        contentColor = contentColor,
    )
}

// Material 3 standard schemes
private val M3Light = lightColorScheme(
    primary = Color(0xFF3A5BA9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDAE2FF),
    onPrimaryContainer = Color(0xFF001848),
    secondary = Color(0xFF585E71),
    secondaryContainer = Color(0xFFDDE2F9),
    tertiary = Color(0xFF735572),
    tertiaryContainer = Color(0xFFFED7F9),
)

private val M3Dark = darkColorScheme(
    primary = Color(0xFFB1C5FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1F428F),
    onPrimaryContainer = Color(0xFFDAE2FF),
    secondary = Color(0xFFC0C6DC),
    secondaryContainer = Color(0xFF404659),
    tertiary = Color(0xFFE1BBDD),
    tertiaryContainer = Color(0xFF5A3E59),
)

// Miuix: Material 3 equivalent roles re-tinted to the official Miuix palette so that shared
// Material-based building blocks (custom pills, dividers, text) stay consistent inside the
// Miuix design system.
private val MiuixLightScheme = lightColorScheme(
    primary = Color(0xFF0066FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5EFFF),
    onPrimaryContainer = Color(0xFF003399),
    secondary = Color(0xFF4B6079),
    secondaryContainer = Color(0xFFE0EAF5),
    onSecondaryContainer = Color(0xFF10263E),
    tertiary = Color(0xFF00897B),
    tertiaryContainer = Color(0xFFCCF2ED),
    background = Color(0xFFF4F6FB),
    onBackground = Color(0xFF1A1C22),
    surface = Color(0xFFFAFBFE),
    onSurface = Color(0xFF1A1C22),
    surfaceVariant = Color(0xFFE4E8F2),
    onSurfaceVariant = Color(0xFF444852),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F9FC),
    surfaceContainer = Color(0xFFF0F4FA),
    surfaceContainerHigh = Color(0xFFE7EDF6),
    surfaceContainerHighest = Color(0xFFDDE4F0),
    outline = Color(0xFF6B7280),
    outlineVariant = Color(0xFFCBD2DE),
)

private val MiuixDarkScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color(0xFF00224E),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF94A3B8),
    secondaryContainer = Color(0xFF243044),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFF2DD4BF),
    tertiaryContainer = Color(0xFF004D40),
    background = Color(0xFF0B0D14),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF11141D),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E2330),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF0E1119),
    surfaceContainerLow = Color(0xFF141722),
    surfaceContainer = Color(0xFF1A1F2C),
    surfaceContainerHigh = Color(0xFF222838),
    surfaceContainerHighest = Color(0xFF2C3448),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF334155),
)

/**
 * Signature seed for NexAlloy's default theme: Royal Alloy Cobalt.
 * Produces an ultra-clean, modern, deep obsidian canvas in dark mode and crisp porcelain in light mode.
 */
val DefaultAlloySeed = Color(0xFF2563EB)

/** Fixed accent seeds for the named colour schemes. */
private fun schemeSeed(colorScheme: String): Color = when (colorScheme) {
    AppPreferenceNames.COLOR_SCHEME_VIOLET -> Color(0xFF7C5CFF)
    AppPreferenceNames.COLOR_SCHEME_EMERALD -> Color(0xFF10B981)
    AppPreferenceNames.COLOR_SCHEME_AMBER -> Color(0xFFF59E0B)
    AppPreferenceNames.COLOR_SCHEME_ROSE -> Color(0xFFE11D48)
    AppPreferenceNames.COLOR_SCHEME_OCEAN -> Color(0xFF0EA5E9)
    else -> DefaultAlloySeed
}

/**
 * Material 3 scheme generated from a seed colour with the same tonal-spot algorithm the Miuix
 * theme engine uses, so a chosen accent renders consistently on both design systems.
 */
private fun materialSchemeFromSeed(seed: Color, dark: Boolean): ColorScheme {
    val scheme = SchemeTonalSpot(
        sourceColorHct = Hct.fromInt(seed.toArgb()),
        isDark = dark,
        contrastLevel = 0.0,
        specVersion = ColorSpec.SpecVersion.SPEC_2021,
        platform = DynamicScheme.Platform.PHONE,
    )
    return (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = Color(scheme.primary),
        onPrimary = Color(scheme.onPrimary),
        primaryContainer = Color(scheme.primaryContainer),
        onPrimaryContainer = Color(scheme.onPrimaryContainer),
        secondary = Color(scheme.secondary),
        onSecondary = Color(scheme.onSecondary),
        secondaryContainer = Color(scheme.secondaryContainer),
        onSecondaryContainer = Color(scheme.onSecondaryContainer),
        tertiary = Color(scheme.tertiary),
        onTertiary = Color(scheme.onTertiary),
        tertiaryContainer = Color(scheme.tertiaryContainer),
        onTertiaryContainer = Color(scheme.onTertiaryContainer),
        error = Color(scheme.error),
        onError = Color(scheme.onError),
        errorContainer = Color(scheme.errorContainer),
        onErrorContainer = Color(scheme.onErrorContainer),
        background = Color(scheme.background),
        onBackground = Color(scheme.onBackground),
        surface = Color(scheme.surface),
        onSurface = Color(scheme.onSurface),
        surfaceVariant = Color(scheme.surfaceVariant),
        onSurfaceVariant = Color(scheme.onSurfaceVariant),
        surfaceContainerLowest = Color(scheme.surfaceContainerLowest),
        surfaceContainerLow = Color(scheme.surfaceContainerLow),
        surfaceContainer = Color(scheme.surfaceContainer),
        surfaceContainerHigh = Color(scheme.surfaceContainerHigh),
        surfaceContainerHighest = Color(scheme.surfaceContainerHighest),
        outline = Color(scheme.outline),
        outlineVariant = Color(scheme.outlineVariant),
        inverseSurface = Color(scheme.inverseSurface),
        inverseOnSurface = Color(scheme.inverseOnSurface),
        inversePrimary = Color(scheme.inversePrimary),
    )
}

/**
 * NexAlloy adaptive dual-theme system supporting both standard AndroidX Material 3
 * and official Miuix (Xiaomi HyperOS) design system with squircle and blur support.
 *
 * [colorSchemeRaw] selects the accent palette: "default" keeps each design system's own
 * palette, while a named scheme ("violet", "emerald", "amber", "rose", "ocean") applies a
 * fixed seed to both design systems.
 */
@Composable
fun NexAlloyTheme(
    themeModeRaw: String = AppSettings.THEME_SYSTEM,
    themeStyle: String = AppSettings.THEME_STYLE_MATERIAL3,
    colorSchemeRaw: String = AppSettings.COLOR_SCHEME_DEFAULT,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val themeMode = AppPreferenceNames.normalizeTheme(themeModeRaw)
    val colorScheme = AppPreferenceNames.normalizeColorScheme(colorSchemeRaw)
    val isDark = when (themeMode) {
        AppSettings.THEME_LIGHT -> false
        AppSettings.THEME_DARK -> true
        else -> systemDark
    }

    val style = if (AppPreferenceNames.isMiuix(themeStyle)) ThemeStyle.MIUIX else ThemeStyle.MATERIAL3
    val context = LocalContext.current
    val accent = schemeSeed(colorScheme)

    if (style == ThemeStyle.MIUIX) {
        val colorSchemeMode = if (isDark) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight
        val controller = remember(colorSchemeMode, accent, isDark) {
            ThemeController(
                colorSchemeMode = colorSchemeMode,
                keyColor = accent,
                isDark = isDark,
            )
        }
        val m3Equivalent = materialSchemeFromSeed(accent, isDark)

        val themeConfig = NexAlloyThemeConfig(
            isMiuix = true,
            cardShape = RoundedCornerShape(22.dp),
            cardBorder = null,
        )

        CompositionLocalProvider(
            LocalThemeStyle provides ThemeStyle.MIUIX,
            LocalNexAlloyThemeConfig provides themeConfig,
        ) {
            MiuixTheme(controller = controller) {
                MaterialTheme(colorScheme = m3Equivalent) {
                    // Miuix components only provide their own content colour, so Material
                    // building blocks in this branch (bare Text/Icon without an explicit
                    // colour) would otherwise fall back to Color.Black - unreadable on the
                    // dark Miuix surfaces. Keep AndroidX's content colour aligned with the
                    // Material role equivalent used while Miuix is active.
                    CompositionLocalProvider(
                        LocalContentColor provides m3Equivalent.onBackground,
                    ) {
                        content()
                    }
                }
            }
        }
    } else {
        val m3Colors = materialSchemeFromSeed(accent, isDark)

        val themeConfig = NexAlloyThemeConfig(
            isMiuix = false,
            cardShape = RoundedCornerShape(16.dp),
            cardBorder = null,
        )

        CompositionLocalProvider(
            LocalThemeStyle provides ThemeStyle.MATERIAL3,
            LocalNexAlloyThemeConfig provides themeConfig,
        ) {
            MaterialTheme(colorScheme = m3Colors, content = content)
        }
    }
}
