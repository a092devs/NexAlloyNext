package io.github.nexalloy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import io.github.nexalloy.core.R
import io.github.nexalloy.core.compatibility.VersionComparator
import io.github.nexalloy.core.discovery.AppItemState
import io.github.nexalloy.ui.MainActivity
import io.github.nexalloy.ui.ManagerState
import io.github.nexalloy.ui.ManagerViewModel
import io.github.nexalloy.ui.navigateTopLevel
import io.github.nexalloy.ui.theme.isMiuixStyle
import io.github.nexalloy.ui.theme.nexAlloyCardBorder
import io.github.nexalloy.ui.theme.nexAlloyCardShape
import io.github.nexalloy.updates.AppInstaller
import io.github.nexalloy.updates.AppRelease
import io.github.nexalloy.updates.UpdateDownloadState
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class AppFilter {
    ALL,
    UPDATES,
    SCOPE,
    CONFLICTS,
    UNINSTALLED,
}

@Composable
fun HomeScreen(viewModel: ManagerViewModel, navController: NavHostController, outerPadding: PaddingValues) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        outerPadding = outerPadding,
        onSearchQueryChange = viewModel::setSearchQuery,
        onCheckUpdates = { viewModel.checkUpdates(userInitiated = true) },
        onUpdateAll = viewModel::updateAll,
        onRequestScope = { viewModel.requestMissingScope() },
        onOpenPatches = { navController.navigateTopLevel(MainActivity.DESTINATION_PATCHES) },
        onOpenApp = { navController.navigate("${MainActivity.DESTINATION_APP}/$it") },
        onDownloadUpdate = viewModel::downloadAndInstallUpdate,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: ManagerState,
    outerPadding: PaddingValues,
    onSearchQueryChange: (String) -> Unit,
    onCheckUpdates: () -> Unit,
    onUpdateAll: () -> Unit,
    onRequestScope: () -> Unit,
    onOpenPatches: () -> Unit,
    onOpenApp: (String) -> Unit,
    onDownloadUpdate: (AppRelease) -> Unit = {},
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var changelog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedFilter by remember { mutableStateOf(AppFilter.ALL) }
    val isMiuix = isMiuixStyle

    val listContent: @Composable (PaddingValues) -> Unit = { padding ->
        PullToRefreshBox(
            isRefreshing = state.checkingUpdates,
            onRefresh = onCheckUpdates,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                // Module Status Card - Placed at the top immediately below top bar
                item {
                    StatusCard(
                        active = state.isActive,
                        title = stringResource(if (state.isActive) R.string.module_active_title else R.string.module_inactive_title),
                        text = if (state.isActive) state.framework.orEmpty() else stringResource(R.string.module_inactive_summary),
                    )
                }

                // Scope Missing Warning
                if (state.missingScope.isNotEmpty()) {
                    item {
                        ActionCard(
                            icon = Icons.Filled.Warning,
                            title = stringResource(R.string.scope_missing_title),
                            text = stringResource(
                                R.string.scope_missing_text,
                                state.missingScope.mapNotNull { state.apps[it] }.joinToString()
                            ),
                            primary = stringResource(R.string.action_request) to onRequestScope,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        )
                    }
                }

                // App Update Notification
                state.appUpdate?.let { release ->
                    item {
                        val downloadState = state.updateDownloadState
                        val isDownloading = downloadState is UpdateDownloadState.Downloading
                        val downloadProgress = if (downloadState is UpdateDownloadState.Downloading && downloadState.progress >= 0f) {
                            (downloadState.progress * 100).toInt()
                        } else null

                        val primaryLabel = when {
                            isDownloading && downloadProgress != null -> stringResource(R.string.downloading_update, downloadProgress)
                            isDownloading -> "Downloading update…"
                            downloadState is UpdateDownloadState.ReadyToInstall -> stringResource(R.string.action_install)
                            else -> stringResource(R.string.action_download)
                        }

                        ActionCard(
                            icon = Icons.Filled.Notifications,
                            title = stringResource(R.string.app_update_title, release.tag),
                            text = if (downloadState is UpdateDownloadState.Error) {
                                stringResource(R.string.download_failed, downloadState.message)
                            } else {
                                stringResource(R.string.app_update_text)
                            },
                            primary = primaryLabel to {
                                if (downloadState is UpdateDownloadState.ReadyToInstall) {
                                    AppInstaller.installApk(context, downloadState.apkFile)
                                } else if (!isDownloading) {
                                    onDownloadUpdate(release)
                                }
                            },
                            secondary = stringResource(R.string.action_changelog) to { changelog = release.tag to release.notes },
                        )
                    }
                }

                // Patch Updates Available
                val updates = state.sourcesWithUpdates
                if (updates.isNotEmpty() && state.isActive) {
                    item {
                        ActionCard(
                            icon = Icons.Filled.Notifications,
                            title = stringResource(R.string.patch_updates_title),
                            text = updates.joinToString { "${it.name} ${state.releases[it.id]?.getOrNull()?.version}" },
                            primary = stringResource(R.string.action_update_all) to onUpdateAll,
                            secondary = stringResource(R.string.nav_patches) to onOpenPatches,
                        )
                    }
                }

                // Empty State when no bundles installed
                if (state.bundles.isEmpty()) {
                    item {
                        ActionCard(
                            title = stringResource(R.string.no_bundles_title),
                            text = stringResource(R.string.no_bundles_text),
                            primary = stringResource(R.string.action_browse_sources) to onOpenPatches,
                        )
                    }
                }

                // Live Search Bar
                item {
                    ThemedSearchField(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = stringResource(R.string.search_apps_hint),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }

                // Filter Chips
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            ThemedChip(
                                label = stringResource(R.string.filter_all),
                                selected = selectedFilter == AppFilter.ALL,
                                onClick = { selectedFilter = AppFilter.ALL },
                            )
                        }
                        item {
                            ThemedChip(
                                label = stringResource(R.string.filter_updates),
                                selected = selectedFilter == AppFilter.UPDATES,
                                onClick = { selectedFilter = AppFilter.UPDATES },
                            )
                        }
                        item {
                            ThemedChip(
                                label = stringResource(R.string.filter_scope),
                                selected = selectedFilter == AppFilter.SCOPE,
                                onClick = { selectedFilter = AppFilter.SCOPE },
                            )
                        }
                        item {
                            ThemedChip(
                                label = stringResource(R.string.filter_conflicts),
                                selected = selectedFilter == AppFilter.CONFLICTS,
                                onClick = { selectedFilter = AppFilter.CONFLICTS },
                            )
                        }
                        item {
                            ThemedChip(
                                label = stringResource(R.string.status_not_installed),
                                selected = selectedFilter == AppFilter.UNINSTALLED,
                                onClick = { selectedFilter = AppFilter.UNINSTALLED },
                            )
                        }
                    }
                }

                // Filter apps
                val allApps = state.appCatalog.filter { item ->
                    if (state.searchQuery.isBlank()) true else {
                        val q = state.searchQuery.trim().lowercase()
                        item.appName.lowercase().contains(q) || item.packageName.lowercase().contains(q)
                    }
                }

                val filteredApps = when (selectedFilter) {
                    AppFilter.ALL -> allApps
                    AppFilter.UPDATES -> allApps.filter { app ->
                        app.isInstalled && app.recommendedVersion != null &&
                            app.installedVersionName != null &&
                            VersionComparator.compare(app.installedVersionName, app.recommendedVersion) < 0
                    }
                    AppFilter.SCOPE -> allApps.filter { it.isInstalled && !it.inScope }
                    AppFilter.CONFLICTS -> allApps.filter { it.hasConflicts }
                    AppFilter.UNINSTALLED -> allApps.filter { !it.isInstalled }
                }

                val installedSection = filteredApps.filter { it.isInstalled }
                val uninstalledSection = filteredApps.filter { !it.isInstalled }

                if (installedSection.isNotEmpty()) {
                    item {
                        SectionTitle(stringResource(R.string.category_installed_apps))
                    }
                    items(installedSection, key = { it.packageName }) { app ->
                        AppCard(app = app, onClick = { onOpenApp(app.packageName) })
                    }
                }

                if (uninstalledSection.isNotEmpty() && selectedFilter != AppFilter.UPDATES && selectedFilter != AppFilter.SCOPE) {
                    item {
                        SectionTitle(stringResource(R.string.category_supported_uninstalled))
                    }
                    items(uninstalledSection, key = { it.packageName }) { app ->
                        AppCard(app = app, onClick = { onOpenApp(app.packageName) })
                    }
                }
            }
        }
    }

    if (isMiuix) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = outerPadding.calculateBottomPadding()),
        ) {
            MiuixTopAppBar(title = stringResource(R.string.app_name))
            listContent(PaddingValues(0.dp))
        }
    } else {
        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(bottom = outerPadding.calculateBottomPadding()),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            listContent(padding)
        }
    }

    changelog?.let { (title, notes) ->
        ThemedDialog(
            show = true,
            title = title,
            onDismissRequest = { changelog = null },
            confirmText = stringResource(android.R.string.ok),
            onConfirm = { changelog = null },
        ) {
            Text(notes.ifBlank { "-" })
        }
    }
}

@Composable
private fun StatusCard(active: Boolean, title: String, text: String) {
    val isMiuix = isMiuixStyle
    val baseContainer = if (active) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val containerColor = if (isMiuix) baseContainer.copy(alpha = 0.85f) else baseContainer
    val border = if (isMiuix) {
        BorderStroke(
            1.dp,
            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
        )
    } else null

    if (isMiuix) {
        MiuixCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            StatusCardContent(active = active, title = title, text = text)
        }
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = nexAlloyCardShape(),
            border = border,
        ) {
            StatusCardContent(active = active, title = title, text = text)
        }
    }
}

@Composable
private fun StatusCardContent(active: Boolean, title: String, text: String) {
    val icons = appIcons()
    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (active) icons.check else icons.warning,
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (text.isNotEmpty()) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    text: String,
    icon: ImageVector? = null,
    primary: Pair<String, () -> Unit>? = null,
    secondary: Pair<String, () -> Unit>? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    val isMiuix = isMiuixStyle
    val actualContainer = if (isMiuix) containerColor.copy(alpha = 0.82f) else containerColor

    if (isMiuix) {
        MiuixCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            ActionCardContent(title, text, icon, primary, secondary, containerColor, isMiuix = true)
        }
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = actualContainer),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = nexAlloyCardShape(),
            border = nexAlloyCardBorder(),
        ) {
            ActionCardContent(title, text, icon, primary, secondary, containerColor, isMiuix = false)
        }
    }
}

@Composable
private fun ActionCardContent(
    title: String,
    text: String,
    icon: ImageVector?,
    primary: Pair<String, () -> Unit>?,
    secondary: Pair<String, () -> Unit>?,
    containerColor: Color,
    isMiuix: Boolean,
) {
    Column(Modifier.padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = LocalContentColor.current.takeIf {
                        containerColor != MaterialTheme.colorScheme.surfaceContainerHigh
                    } ?: MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (text.isNotEmpty()) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
        }
        if (primary != null || secondary != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            ) {
                secondary?.let { (label, action) ->
                    ThemedOutlinedButton(label = label, onClick = action)
                }
                primary?.let { (label, action) ->
                    ThemedFilledButton(label = label, onClick = action)
                }
            }
        }
    }
}
