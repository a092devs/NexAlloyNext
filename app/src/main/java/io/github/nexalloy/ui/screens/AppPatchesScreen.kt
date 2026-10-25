package io.github.nexalloy.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import io.github.nexalloy.core.R
import io.github.nexalloy.core.compatibility.CompatibilityEngine
import io.github.nexalloy.core.compatibility.CompatibilityStatus
import io.github.nexalloy.core.discovery.AppDiscovery
import io.github.nexalloy.patchPreferenceKey
import io.github.nexalloy.ui.BundlePatches
import io.github.nexalloy.ui.ManagerViewModel
import io.github.nexalloy.ui.theme.isMiuixStyle
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.preference.SwitchPreference

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppPatchesScreen(viewModel: ManagerViewModel, packageName: String, navController: NavHostController) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val appItem = state.appCatalog.find { it.packageName == packageName }
    val liveInfo = remember(packageName) { AppDiscovery.queryPackage(context, packageName) }
    val appIcon = remember(packageName) { AppDiscovery.loadAppIcon(context, packageName) }
    val appName = appItem?.appName ?: liveInfo.appLabel.ifBlank { packageName }

    var result by remember { mutableStateOf<Result<List<BundlePatches>>?>(null) }
    val enabled = remember { mutableStateMapOf<String, Boolean>() }
    var reload by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var patchSearchQuery by remember { mutableStateOf("") }

    val allMatchingBundles = state.bundles.filter { it.enabled && packageName in it.apps }
    val allTargets = allMatchingBundles.flatMap { it.targets[packageName].orEmpty() }
    val compatibility = remember(liveInfo.versionName, allTargets) {
        CompatibilityEngine.resolve(liveInfo.versionName, allTargets)
    }
    val recommendedVersion = remember(allTargets) {
        CompatibilityEngine.recommendedVersion(allTargets)
    }

    LaunchedEffect(packageName, state.isActive, state.bundles, reload) {
        val loaded = viewModel.patches(packageName)
        enabled.clear()
        loaded.getOrNull()?.forEach { group ->
            group.patches.forEach { (patch, on) -> enabled[patchPreferenceKey(group.bundle.id, patch.name)] = on }
        }
        result = loaded
    }

    val isMiuix = isMiuixStyle
    val icons = appIcons()
    val patchMenuItems: List<ThemedMenuItem> = listOf(
        ThemedMenuItem(
            label = stringResource(R.string.action_restore_defaults),
            onClick = {
                viewModel.resetPatches(packageName, result?.getOrNull().orEmpty(), defaults = true)
                reload++
            },
        ),
        ThemedMenuItem(
            label = stringResource(R.string.action_disable_all),
            onClick = {
                viewModel.resetPatches(packageName, result?.getOrNull().orEmpty(), defaults = false)
                reload++
            },
        ),
        ThemedMenuItem(
            label = "Open App Info",
            onClick = {
                viewModel.openAppInfo(packageName)
            },
        ),
        ThemedMenuItem(
            label = "Force stop",
            onClick = {
                viewModel.forceStopApp(packageName)
            },
        ),
    )

    val contentBody: @Composable (PaddingValues) -> Unit = { padding ->
        val loaded = result
        when {
            loaded == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            loaded.isFailure -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.patches_load_failed, loaded.exceptionOrNull()?.message.orEmpty()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(16.dp))
                ThemedOutlinedButton(
                    label = "Retry",
                    onClick = { reload++ },
                )
            }

            else -> {
                val groups = loaded.getOrThrow()
                LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 24.dp)) {
                    // App Summary Card
                    item {
                        ThemedCard(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (appIcon != null) {
                                        Image(
                                            bitmap = appIcon,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = appName.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(16.dp))

                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            appName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            if (liveInfo.isInstalled) {
                                                stringResource(R.string.installed_version, liveInfo.versionName.orEmpty())
                                            } else {
                                                stringResource(R.string.app_not_installed_short)
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        if (recommendedVersion != null) {
                                            Text(
                                                stringResource(R.string.recommended_version, recommendedVersion),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        } else if (allTargets.isEmpty() && allMatchingBundles.isNotEmpty()) {
                                            Text(
                                                stringResource(R.string.recommended_any_version),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                            )
                                        } else if (allMatchingBundles.isEmpty()) {
                                            Text(
                                                "No official recommendation available (no bundle installed)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline,
                                            )
                                        } else {
                                            Text(
                                                "No official recommendation available (all tested versions are experimental)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline,
                                            )
                                        }

                                        if (allMatchingBundles.isNotEmpty()) {
                                            Text(
                                                "Source: " + allMatchingBundles.joinToString { b -> "${b.name} (${b.author.ifBlank { "NexAlloy" }})" },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 2.dp),
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                CompatibilityChip(compatibility)

                                val explanation = when (compatibility) {
                                    is CompatibilityStatus.Compatible -> if (compatibility.isExperimental) {
                                        "Installed version matches an officially tested experimental target."
                                    } else {
                                        "Installed version matches an officially verified stable target."
                                    }
                                    is CompatibilityStatus.CompatibleUntested -> {
                                        "Installed version is newer than verified targets (highest tested: ${compatibility.highestVerified ?: "none"}). Patches may still work dynamically."
                                    }
                                    is CompatibilityStatus.Incompatible -> compatibility.reason
                                    is CompatibilityStatus.Dynamic -> "Universal bytecode hook — compatible with most versions dynamically."
                                    is CompatibilityStatus.NotInstalled -> "Application is not installed on this device."
                                    is CompatibilityStatus.MissingBundle -> "No active patch bundle installed for this application."
                                    is CompatibilityStatus.Unknown -> compatibility.reason
                                }
                                Text(
                                    text = explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }

                    // Scope Alert Card if installed but not in scope
                    if (liveInfo.isInstalled && packageName in state.missingScope) {
                        item {
                            ActionCard(
                                icon = Icons.Filled.Warning,
                                title = stringResource(R.string.scope_missing_title),
                                text = "This application is not in the LSPosed module scope. Patches will not take effect until added.",
                                primary = stringResource(R.string.action_request) to {
                                    viewModel.requestMissingScope(listOf(packageName))
                                },
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            )
                        }
                    }

                    // Search input within patches
                    item {
                        ThemedSearchField(
                            value = patchSearchQuery,
                            onValueChange = { patchSearchQuery = it },
                            placeholder = stringResource(R.string.search_patches_hint),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.force_stop_to_apply_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                            )
                            // FlowRow: long localized labels wrap onto a second line instead of
                            // squeezing the summary text or overflowing the card.
                            FlowRow(
                                modifier = Modifier.padding(start = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            ) {
                                ThemedOutlinedButton(
                                    label = "App info",
                                    onClick = { viewModel.openAppInfo(packageName) },
                                )
                                ThemedOutlinedButton(
                                    label = "Force stop",
                                    onClick = { viewModel.forceStopApp(packageName) },
                                )
                            }
                        }
                    }

                    // Patch Groups
                    for (group in groups) {
                        val filteredPatches = group.patches.filter { (patch, _) ->
                            if (patchSearchQuery.isBlank()) true else {
                                val q = patchSearchQuery.trim().lowercase()
                                patch.name.lowercase().contains(q) || patch.description.lowercase().contains(q)
                            }
                        }

                        if (filteredPatches.isNotEmpty()) {
                            item(key = group.bundle.id) {
                                val title = if (group.sourceName != null) {
                                    "${group.bundle.name} v${group.bundle.version} • ${group.sourceName}"
                                } else {
                                    "${group.bundle.name} v${group.bundle.version}"
                                }
                                SectionTitle(title)
                            }

                            if (isMiuix) {
                                item(key = "${group.bundle.id}_card") {
                                    MiuixCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp),
                                    ) {
                                        for ((patch, _) in filteredPatches) {
                                            val key = patchPreferenceKey(group.bundle.id, patch.name)
                                            val checked = enabled[key] ?: patch.use
                                            fun toggle(value: Boolean) {
                                                enabled[key] = value
                                                viewModel.setPatchEnabled(packageName, group.bundle, patch, value)
                                            }
                                            val isDuplicate = patch.name in group.duplicateNames
                                            val descriptionText = patch.description.takeIf { it.isNotEmpty() }
                                            val supportingText = if (isDuplicate) {
                                                listOfNotNull(descriptionText, "⚠️ Duplicate: also provided by another active bundle. Handled deterministically by patch engine.").joinToString(" • ")
                                            } else {
                                                descriptionText
                                            }

                                            SwitchPreference(
                                                title = patch.name,
                                                summary = supportingText,
                                                checked = checked,
                                                onCheckedChange = ::toggle,
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(filteredPatches, key = { "${group.bundle.id}/${it.first.name}" }) { (patch, _) ->
                                    val key = patchPreferenceKey(group.bundle.id, patch.name)
                                    val checked = enabled[key] ?: patch.use
                                    fun toggle(value: Boolean) {
                                        enabled[key] = value
                                        viewModel.setPatchEnabled(packageName, group.bundle, patch, value)
                                    }
                                    val isDuplicate = patch.name in group.duplicateNames
                                    val descriptionText = patch.description.takeIf { it.isNotEmpty() }
                                    val supportingText = if (isDuplicate) {
                                        listOfNotNull(descriptionText, "⚠️ Duplicate: also provided by another active bundle. Handled deterministically by patch engine.").joinToString(" • ")
                                    } else {
                                        descriptionText
                                    }

                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                patch.name,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        },
                                        supportingContent = supportingText?.let { { Text(it) } },
                                        trailingContent = { Switch(checked = checked, onCheckedChange = ::toggle) },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                        modifier = Modifier.clickable { toggle(!checked) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isMiuix) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            MiuixTopAppBar(
                title = appName,
                subtitle = packageName,
                color = MaterialTheme.colorScheme.background,
                navigationIcon = {
                    ThemedIconButton(onClick = { navController.popBackStack() }) {
                        Icon(icons.back, contentDescription = null)
                    }
                },
                actions = {
                    if (liveInfo.isInstalled) {
                        ThemedIconButton(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }) { Icon(icons.info, contentDescription = stringResource(R.string.action_app_info)) }
                    }
                    ThemedIconButton(onClick = { menu = true }) { Icon(icons.more, contentDescription = null) }
                    ThemedMenu(
                        expanded = menu,
                        onDismissRequest = { menu = false },
                        items = patchMenuItems,
                    )
                },
            )
            contentBody(PaddingValues(0.dp))
        }
    } else {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(appName)
                            Text(
                                packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    navigationIcon = {
                        ThemedIconButton(onClick = { navController.popBackStack() }) {
                            Icon(icons.back, contentDescription = null)
                        }
                    },
                    actions = {
                        if (liveInfo.isInstalled) {
                            ThemedIconButton(onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }) { Icon(icons.info, contentDescription = stringResource(R.string.action_app_info)) }
                        }
                        ThemedIconButton(onClick = { menu = true }) { Icon(icons.more, contentDescription = null) }
                        ThemedMenu(
                            expanded = menu,
                            onDismissRequest = { menu = false },
                            items = patchMenuItems,
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            contentBody(padding)
        }
    }
}
