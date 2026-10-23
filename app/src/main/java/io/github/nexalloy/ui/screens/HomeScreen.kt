package io.github.nexalloy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import io.github.nexalloy.core.BuildConfig
import io.github.nexalloy.core.R
import io.github.nexalloy.core.compatibility.VersionComparator
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
                        text = if (state.isActive) stringResource(R.string.module_active_summary, BuildConfig.VERSION_NAME) else stringResource(R.string.module_inactive_summary),
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
                        val isDownloadingPatches = state.sourceDownloadProgress.isNotEmpty()
                        val activeProgress = state.sourceDownloadProgress.values.firstOrNull()
                        val primaryLabel = when {
                            activeProgress != null && activeProgress.isInstalling -> stringResource(R.string.source_installing)
                            activeProgress != null && activeProgress.progress >= 0f -> {
                                val pct = (activeProgress.progress * 100).toInt()
                                if (activeProgress.totalBundles > 1) {
                                    "Downloading (${activeProgress.bundleIndex}/${activeProgress.totalBundles})... $pct%"
                                } else {
                                    "Downloading update... $pct%"
                                }
                            }
                            activeProgress != null -> "Downloading..."
                            state.busySources.isNotEmpty() -> stringResource(R.string.source_installing)
                            else -> stringResource(R.string.action_update_all)
                        }
                        val primaryAction = if (isDownloadingPatches || state.busySources.isNotEmpty()) {
                            {}
                        } else onUpdateAll

                        ActionCard(
                            icon = Icons.Filled.Notifications,
                            title = stringResource(R.string.patch_updates_title),
                            text = updates.joinToString { "${it.name} ${state.releases[it.id]?.getOrNull()?.version}" },
                            primary = primaryLabel to primaryAction,
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
                    }
                }

                // Only apps installed on this device are listed.
                val installedApps = state.appCatalog.filter { item ->
                    if (!item.isInstalled) false
                    else if (state.searchQuery.isBlank()) true
                    else {
                        val q = state.searchQuery.trim().lowercase()
                        item.appName.lowercase().contains(q) || item.packageName.lowercase().contains(q)
                    }
                }

                val filteredApps = when (selectedFilter) {
                    AppFilter.ALL -> installedApps
                    AppFilter.UPDATES -> installedApps.filter { app ->
                        app.recommendedVersion != null &&
                            app.installedVersionName != null &&
                            VersionComparator.compare(app.installedVersionName, app.recommendedVersion) < 0
                    }
                    AppFilter.SCOPE -> installedApps.filter { !it.inScope }
                    AppFilter.CONFLICTS -> installedApps.filter { it.hasConflicts }
                }

                if (filteredApps.isNotEmpty()) {
                    item {
                        SectionTitle(stringResource(R.string.category_installed_apps))
                    }
                    items(filteredApps, key = { it.packageName }) { app ->
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
            ChangelogText(notes)
        }
    }
}

private data class StatusPalette(
    val bg: Color,
    val border: Color,
    val badge: Color,
    val icon: Color,
    val title: Color,
    val desc: Color,
)

@Composable
private fun StatusCard(active: Boolean, title: String, text: String) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(18.dp)

    val palette = if (active) {
        if (isDark) {
            StatusPalette(
                bg = Color(0xFF0D3320),
                border = Color(0xFF1B6B40),
                badge = Color(0xFF165331),
                icon = Color(0xFF4CAF50),
                title = Color(0xFFE8F5E9),
                desc = Color(0xFFA5D6A7),
            )
        } else {
            StatusPalette(
                bg = Color(0xFFE8F5E9),
                border = Color(0xFFA5D6A7),
                badge = Color(0xFFC8E6C9),
                icon = Color(0xFF2E7D32),
                title = Color(0xFF1B5E20),
                desc = Color(0xFF2E7D32),
            )
        }
    } else {
        if (isDark) {
            StatusPalette(
                bg = Color(0xFF381414),
                border = Color(0xFF6B2424),
                badge = Color(0xFF521C1C),
                icon = Color(0xFFEF5350),
                title = Color(0xFFFFEBEE),
                desc = Color(0xFFFFCDD2),
            )
        } else {
            StatusPalette(
                bg = Color(0xFFFFEBEE),
                border = Color(0xFFFFCDD2),
                badge = Color(0xFFFFCDD2),
                icon = Color(0xFFC62828),
                title = Color(0xFFB71C1C),
                desc = Color(0xFFC62828),
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(shape)
            .background(palette.bg)
            .border(1.dp, palette.border, shape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(palette.badge),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (active) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = palette.icon,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.title,
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(palette.icon.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = if (active) "Active" else "Inactive",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.icon,
                        )
                    }
                }
                if (text.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.desc,
                        maxLines = 2,
                    )
                }
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

@OptIn(ExperimentalLayoutApi::class)
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
            // FlowRow so two long (localized) labels wrap and stay tappable instead of squeezing.
            FlowRow(
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
