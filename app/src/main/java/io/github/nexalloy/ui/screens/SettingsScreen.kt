package io.github.nexalloy.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.nexalloy.core.BuildConfig
import io.github.nexalloy.core.R
import io.github.nexalloy.ui.ManagerViewModel
import io.github.nexalloy.ui.theme.isMiuixStyle
import io.github.nexalloy.updates.AppPreferenceNames
import io.github.nexalloy.updates.AppSettings
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

private val licenses = listOf(
    Triple("compose-miuix-ui", "Apache License 2.0, YuKongA", "https://github.com/compose-miuix-ui/miuix"),
    Triple("Morphe Patcher", "GNU General Public License v3.0", "https://github.com/MorpheApp/morphe-patcher"),
    Triple("DexKit", "LGPL-3.0, LuckyPray", "https://github.com/LuckyPray/DexKit"),
    Triple("smali / dexlib2", "BSD 3-Clause, Google", "https://github.com/google/smali"),
    Triple("Kotlin", "Apache License 2.0, JetBrains", "https://kotlinlang.org"),
    Triple("AndroidX, Jetpack Compose", "Apache License 2.0, Google", "https://developer.android.com/jetpack/androidx"),
    Triple("libxposed", "Apache License 2.0", "https://github.com/libxposed"),
    Triple("LSPosed", "GNU General Public License v3.0", "https://github.com/LSPosed/LSPosed"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ManagerViewModel, outerPadding: PaddingValues) {
    val context = LocalContext.current
    val settings = viewModel.settings
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val isMiuix = isMiuixStyle

    var notifyApp by remember { mutableStateOf(settings.notifyAppUpdates) }
    var notifyPatches by remember { mutableStateOf(settings.notifyPatchUpdates) }
    var iconHidden by remember { mutableStateOf(viewModel.launcherIconHidden) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showThemeStyleDialog by remember { mutableStateOf(false) }
    var showColorSchemeDialog by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }

    val transparent = ListItemDefaults.colors(containerColor = Color.Transparent)

    val currentThemeLabel = when (state.currentTheme) {
        AppSettings.THEME_LIGHT -> stringResource(R.string.theme_light)
        AppSettings.THEME_DARK -> stringResource(R.string.theme_dark)
        else -> stringResource(R.string.theme_system)
    }

    val currentThemeStyleLabel = when (state.currentThemeStyle) {
        AppSettings.THEME_STYLE_MIUIX -> stringResource(R.string.theme_style_miuix)
        else -> stringResource(R.string.theme_style_material3)
    }

    val currentColorSchemeLabel = when (state.currentColorScheme) {
        AppPreferenceNames.COLOR_SCHEME_VIOLET -> stringResource(R.string.color_scheme_violet)
        AppPreferenceNames.COLOR_SCHEME_EMERALD -> stringResource(R.string.color_scheme_emerald)
        AppPreferenceNames.COLOR_SCHEME_AMBER -> stringResource(R.string.color_scheme_amber)
        AppPreferenceNames.COLOR_SCHEME_ROSE -> stringResource(R.string.color_scheme_rose)
        AppPreferenceNames.COLOR_SCHEME_OCEAN -> stringResource(R.string.color_scheme_ocean)
        else -> stringResource(R.string.color_scheme_default)
    }

    if (isMiuix) {
        // Genuine Miuix layout with Card containers, SmallTitle headers, and native Miuix preferences
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            MiuixTopAppBar(
                title = stringResource(R.string.nav_settings),
                color = MaterialTheme.colorScheme.background,
            )

            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp + outerPadding.calculateBottomPadding()),
                modifier = Modifier.fillMaxSize(),
            ) {
                item { SmallTitle(insideMargin = MiuixSectionTitleMargin, text = stringResource(R.string.settings_updates)) }
                item {
                    MiuixCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        SwitchPreference(
                            title = stringResource(R.string.setting_notify_app),
                            summary = stringResource(R.string.setting_notify_app_summary),
                            checked = notifyApp,
                            onCheckedChange = { notifyApp = it; settings.notifyAppUpdates = it },
                        )
                        SwitchPreference(
                            title = stringResource(R.string.setting_notify_patches),
                            summary = stringResource(R.string.setting_notify_patches_summary),
                            checked = notifyPatches,
                            onCheckedChange = { notifyPatches = it; settings.notifyPatchUpdates = it },
                        )
                        ArrowPreference(
                            title = stringResource(R.string.check_for_update_title),
                            summary = if (state.checkingUpdates) stringResource(R.string.source_checking) else null,
                            onClick = { if (!state.checkingUpdates) viewModel.checkUpdates(userInitiated = true) },
                        )
                    }
                }

                item { SmallTitle(insideMargin = MiuixSectionTitleMargin, text = stringResource(R.string.settings_appearance)) }
                item {
                    MiuixCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        ArrowPreference(
                            title = stringResource(R.string.theme_title),
                            summary = currentThemeLabel,
                            onClick = { showThemeDialog = true },
                        )
                        ArrowPreference(
                            title = stringResource(R.string.theme_style_title),
                            summary = currentThemeStyleLabel,
                            onClick = { showThemeStyleDialog = true },
                        )
                        ArrowPreference(
                            title = stringResource(R.string.color_scheme_title),
                            summary = currentColorSchemeLabel,
                            onClick = { showColorSchemeDialog = true },
                        )
                        // Floating navigation is a Miuix-only arrangement, so this row only exists in
                        // the Miuix presentation. It is off by default and can be enabled here.
                        SwitchPreference(
                            title = stringResource(R.string.miuix_floating_nav_title),
                            summary = stringResource(R.string.miuix_floating_nav_summary),
                            checked = state.floatingNavigation,
                            onCheckedChange = { viewModel.setFloatingNavigation(it) },
                        )
                        if (state.floatingNavigation) {
                            SwitchPreference(
                                title = stringResource(R.string.miuix_floating_nav_translucent_title),
                                summary = stringResource(R.string.miuix_floating_nav_translucent_summary),
                                checked = state.floatingBarTranslucent,
                                onCheckedChange = { viewModel.setFloatingBarTranslucent(it) },
                            )
                        }
                    }
                }

                item { SmallTitle(insideMargin = MiuixSectionTitleMargin, text = stringResource(R.string.settings_general)) }
                item {
                    MiuixCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        SwitchPreference(
                            title = stringResource(R.string.hide_icon_title),
                            summary = stringResource(R.string.hide_icon_summary),
                            checked = iconHidden,
                            onCheckedChange = { iconHidden = it; viewModel.launcherIconHidden = it },
                        )
                        ArrowPreference(
                            title = stringResource(R.string.action_clear_cache),
                            summary = stringResource(R.string.action_clear_cache_summary),
                            onClick = { viewModel.clearDexKitCache() },
                        )
                    }
                }

                item { SmallTitle(insideMargin = MiuixSectionTitleMargin, text = stringResource(R.string.settings_about)) }
                item {
                    MiuixCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        ArrowPreference(
                            title = "${stringResource(R.string.app_name)} ${BuildConfig.VERSION_NAME}",
                            summary = "${BuildConfig.COMMIT_HASH} ${BuildConfig.BUILD_TYPE} · ${DateUtils.getRelativeTimeSpanString(BuildConfig.COMMIT_DATE * 1000)}",
                        )
                        ArrowPreference(
                            title = stringResource(R.string.source_code),
                            summary = "github.com/${BuildConfig.UPDATE_REPOSITORY}",
                            onClick = { openUrl(context, "https://github.com/${BuildConfig.UPDATE_REPOSITORY}") },
                        )
                        ArrowPreference(
                            title = stringResource(R.string.open_source_license),
                            onClick = { showLicenses = true },
                        )
                    }
                }

                item {
                    Text(
                        stringResource(R.string.slogan_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        }
    } else {
        // Standard Material 3 layout
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.nav_settings)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 24.dp + outerPadding.calculateBottomPadding()),
                modifier = Modifier.fillMaxSize(),
            ) {
                item { SectionHeader(stringResource(R.string.settings_updates)) }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.setting_notify_app)) },
                        supportingContent = { Text(stringResource(R.string.setting_notify_app_summary)) },
                        trailingContent = {
                            Switch(notifyApp, { notifyApp = it; settings.notifyAppUpdates = it })
                        },
                        colors = transparent,
                        modifier = Modifier.clickable { notifyApp = !notifyApp; settings.notifyAppUpdates = notifyApp },
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.setting_notify_patches)) },
                        supportingContent = { Text(stringResource(R.string.setting_notify_patches_summary)) },
                        trailingContent = {
                            Switch(notifyPatches, { notifyPatches = it; settings.notifyPatchUpdates = it })
                        },
                        colors = transparent,
                        modifier = Modifier.clickable { notifyPatches = !notifyPatches; settings.notifyPatchUpdates = notifyPatches },
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.check_for_update_title)) },
                        supportingContent = if (state.checkingUpdates) {
                            { Text(stringResource(R.string.source_checking)) }
                        } else null,
                        colors = transparent,
                        modifier = Modifier.clickable {
                            if (!state.checkingUpdates) viewModel.checkUpdates(userInitiated = true)
                        },
                    )
                }

                item { SectionHeader(stringResource(R.string.settings_appearance)) }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.theme_title)) },
                        supportingContent = { Text(currentThemeLabel) },
                        colors = transparent,
                        modifier = Modifier.clickable { showThemeDialog = true },
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.theme_style_title)) },
                        supportingContent = { Text(currentThemeStyleLabel) },
                        colors = transparent,
                        modifier = Modifier.clickable { showThemeStyleDialog = true },
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.color_scheme_title)) },
                        supportingContent = { Text(currentColorSchemeLabel) },
                        colors = transparent,
                        modifier = Modifier.clickable { showColorSchemeDialog = true },
                    )
                }

                item { SectionHeader(stringResource(R.string.settings_general)) }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.hide_icon_title)) },
                        supportingContent = { Text(stringResource(R.string.hide_icon_summary)) },
                        trailingContent = { Switch(iconHidden, { iconHidden = it; viewModel.launcherIconHidden = it }) },
                        colors = transparent,
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.action_clear_cache)) },
                        supportingContent = { Text(stringResource(R.string.action_clear_cache_summary)) },
                        colors = transparent,
                        modifier = Modifier.clickable { viewModel.clearDexKitCache() },
                    )
                }

                item { SectionHeader(stringResource(R.string.settings_about)) }
                item {
                    ListItem(
                        headlineContent = { Text("${stringResource(R.string.app_name)} ${BuildConfig.VERSION_NAME}") },
                        supportingContent = {
                            Text(
                                "${BuildConfig.COMMIT_HASH} ${BuildConfig.BUILD_TYPE} · " +
                                    DateUtils.getRelativeTimeSpanString(BuildConfig.COMMIT_DATE * 1000)
                            )
                        },
                        colors = transparent,
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.source_code)) },
                        supportingContent = { Text("github.com/${BuildConfig.UPDATE_REPOSITORY}") },
                        colors = transparent,
                        modifier = Modifier.clickable { openUrl(context, "https://github.com/${BuildConfig.UPDATE_REPOSITORY}") },
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.open_source_license)) },
                        colors = transparent,
                        modifier = Modifier.clickable { showLicenses = true },
                    )
                }
                item {
                    Text(
                        stringResource(R.string.slogan_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        ThemedRadioDialog(
            show = true,
            title = stringResource(R.string.theme_title),
            options = listOf(
                AppSettings.THEME_SYSTEM to stringResource(R.string.theme_system),
                AppSettings.THEME_LIGHT to stringResource(R.string.theme_light),
                AppSettings.THEME_DARK to stringResource(R.string.theme_dark),
            ),
            selected = state.currentTheme,
            onSelect = { viewModel.setTheme(it) },
            onDismissRequest = { showThemeDialog = false },
        )
    }

    if (showThemeStyleDialog) {
        ThemedRadioDialog(
            show = true,
            title = stringResource(R.string.theme_style_title),
            options = listOf(
                AppSettings.THEME_STYLE_MATERIAL3 to stringResource(R.string.theme_style_material3),
                AppSettings.THEME_STYLE_MIUIX to stringResource(R.string.theme_style_miuix),
            ),
            selected = state.currentThemeStyle,
            onSelect = { viewModel.setThemeStyle(it) },
            onDismissRequest = { showThemeStyleDialog = false },
        )
    }

    if (showColorSchemeDialog) {
        ThemedRadioDialog(
            show = true,
            title = stringResource(R.string.color_scheme_title),
            options = listOf(
                AppPreferenceNames.COLOR_SCHEME_DEFAULT to stringResource(R.string.color_scheme_default),
                AppPreferenceNames.COLOR_SCHEME_VIOLET to stringResource(R.string.color_scheme_violet),
                AppPreferenceNames.COLOR_SCHEME_EMERALD to stringResource(R.string.color_scheme_emerald),
                AppPreferenceNames.COLOR_SCHEME_AMBER to stringResource(R.string.color_scheme_amber),
                AppPreferenceNames.COLOR_SCHEME_ROSE to stringResource(R.string.color_scheme_rose),
                AppPreferenceNames.COLOR_SCHEME_OCEAN to stringResource(R.string.color_scheme_ocean),
            ),
            selected = state.currentColorScheme,
            onSelect = { viewModel.setColorScheme(it) },
            onDismissRequest = { showColorSchemeDialog = false },
        )
    }

    if (showLicenses) {
        ThemedDialog(
            show = true,
            title = stringResource(R.string.open_source_license),
            onDismissRequest = { showLicenses = false },
            confirmText = stringResource(android.R.string.ok),
            onConfirm = { showLicenses = false },
        ) {
            Column {
                licenses.forEach { (name, license, url) ->
                    ListItem(
                        headlineContent = { Text(name) },
                        supportingContent = { Text(license) },
                        colors = transparent,
                        modifier = Modifier.clickable { openUrl(context, url) },
                    )
                }
            }
        }
    }
}
