package io.github.nexalloy.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.nexalloy.core.InstalledBundle
import io.github.nexalloy.core.R
import io.github.nexalloy.core.discovery.AppDiscovery
import io.github.nexalloy.manager.PatchSource
import io.github.nexalloy.manager.SourceProvenance
import io.github.nexalloy.ui.ManagerState
import io.github.nexalloy.ui.ManagerViewModel
import io.github.nexalloy.ui.theme.isMiuixStyle
import io.github.nexalloy.updates.AppSettings
import top.yukonga.miuix.kmp.basic.FloatingActionButton as MiuixFloatingActionButton
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar

@Composable
fun PatchesScreen(viewModel: ManagerViewModel, outerPadding: PaddingValues) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PatchesContent(
        state = state,
        outerPadding = outerPadding,
        onCheckUpdates = { viewModel.checkUpdates(userInitiated = true) },
        onImport = viewModel::importBundle,
        onInstall = viewModel::install,
        onRefreshSource = viewModel::refreshSource,
        onSetSourceEnabled = viewModel::setSourceEnabled,
        onRemoveSource = viewModel::removeSource,
        onAddSource = viewModel::addSource,
        onSetEnabled = viewModel::setEnabled,
        onUninstall = viewModel::uninstall,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchesContent(
    state: ManagerState,
    outerPadding: PaddingValues,
    onCheckUpdates: () -> Unit,
    onImport: (Uri) -> Unit,
    onInstall: (PatchSource) -> Unit,
    onRefreshSource: (PatchSource) -> Unit,
    onSetSourceEnabled: (PatchSource, Boolean) -> Unit,
    onRemoveSource: (PatchSource) -> Unit,
    onAddSource: (String, String, Boolean) -> Unit,
    onSetEnabled: (InstalledBundle, Boolean) -> Unit,
    onUninstall: (InstalledBundle) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var selectedTab by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var addSource by remember { mutableStateOf(false) }
    var changelog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImport(uri)
    }

    val isMiuix = isMiuixStyle
    val icons = appIcons()

    val bodyContent: @Composable (PaddingValues) -> Unit = { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            if (isMiuix) {
                MiuixSegmentedTabs(
                    tabs = listOf(
                        "${stringResource(R.string.tab_sources)} (${state.sources.size})",
                        "${stringResource(R.string.tab_bundles)} (${state.bundles.size})",
                    ),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                    isDark = when (state.currentTheme) {
                        AppSettings.THEME_LIGHT -> false
                        AppSettings.THEME_DARK -> true
                        else -> isSystemInDarkTheme()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            } else {
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("${stringResource(R.string.tab_sources)} (${state.sources.size})") },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("${stringResource(R.string.tab_bundles)} (${state.bundles.size})") },
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = state.checkingUpdates,
                onRefresh = onCheckUpdates,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 96.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (selectedTab == 0) {
                        // Sources Tab
                        if (!state.isActive) {
                            item {
                                ActionCard(
                                    title = stringResource(R.string.module_inactive_title),
                                    text = stringResource(R.string.module_inactive_summary),
                                    icon = Icons.Filled.Warning,
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                )
                            }
                        }

                        items(state.sources, key = { it.id }) { source ->
                            SourceCard(
                                source = source,
                                state = state,
                                onInstall = { onInstall(source) },
                                onRefresh = { onRefreshSource(source) },
                                onEnabledChange = { onSetSourceEnabled(source, it) },
                                onChangelog = { release -> changelog = "${source.name} ${release.first}" to release.second },
                                onRemove = { confirm = source.name to { onRemoveSource(source) } },
                            )
                        }

                        item {
                            Text(
                                stringResource(R.string.patch_sources_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    } else {
                        // Installed Bundles Tab
                        if (state.bundles.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        Icons.Outlined.FolderZip,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.outline,
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        stringResource(R.string.no_bundles_text),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            items(state.bundles.sortedBy { it.name.lowercase() }, key = { it.id }) { bundle ->
                                BundleCard(
                                    bundle = bundle,
                                    sources = state.sources,
                                    onEnabled = { onSetEnabled(bundle, it) },
                                    onUninstall = { confirm = bundle.name to { onUninstall(bundle) } },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isMiuix) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(bottom = outerPadding.calculateBottomPadding()),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                MiuixTopAppBar(
                    title = stringResource(R.string.nav_patches),
                    color = MaterialTheme.colorScheme.background,
                    actions = {
                        ThemedIconButton(onClick = { menu = true }) {
                            Icon(icons.more, contentDescription = null)
                        }
                        ThemedMenu(
                            expanded = menu,
                            onDismissRequest = { menu = false },
                            items = listOf(
                                ThemedMenuItem(
                                    label = stringResource(R.string.import_bundle_title),
                                    icon = icons.bundle,
                                    onClick = { importer.launch(arrayOf("*/*")) },
                                ),
                            ),
                        )
                    },
                )
                bodyContent(PaddingValues(0.dp))
            }
            if (selectedTab == 0) {
                MiuixFloatingActionButton(
                    onClick = { addSource = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                ) {
                    Icon(icons.add, contentDescription = stringResource(R.string.add_source_title))
                }
            }
        }
    } else {
        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(bottom = outerPadding.calculateBottomPadding()),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.nav_patches)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    actions = {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = null) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.import_bundle_title)) },
                                leadingIcon = { Icon(Icons.Outlined.FolderZip, contentDescription = null) },
                                onClick = { menu = false; importer.launch(arrayOf("*/*")) },
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    ExtendedFloatingActionButton(
                        onClick = { addSource = true },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.add_source_title)) },
                    )
                }
            },
        ) { padding ->
            bodyContent(padding)
        }
    }

    if (addSource) {
        AddSourceDialog(
            existingSources = state.sources,
            onDismiss = { addSource = false },
        ) { name, url, prerelease ->
            addSource = false
            onAddSource(name, url, prerelease)
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

    confirm?.let { (name, action) ->
        ThemedDialog(
            show = true,
            title = stringResource(R.string.remove_title, name),
            onDismissRequest = { confirm = null },
            confirmText = stringResource(R.string.action_remove),
            onConfirm = {
                confirm = null
                action()
            },
            dismissText = stringResource(android.R.string.cancel),
            onDismiss = { confirm = null },
        ) {}
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    if (timestamp <= 0L) return "Never"
    val diff = System.currentTimeMillis() - timestamp
    val mins = diff / (60 * 1000)
    val hours = mins / 60
    val days = hours / 24
    return when {
        mins < 1 -> "Just now"
        mins < 60 -> "${mins}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SourceCard(
    source: PatchSource,
    state: ManagerState,
    onInstall: () -> Unit,
    onRefresh: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onChangelog: (Pair<String, String>) -> Unit,
    onRemove: () -> Unit,
) {
    val release = state.releases[source.id]
    val busy = source.id in state.busySources
    val hasUpdate = state.hasUpdate(source)

    val isMiuix = isMiuixStyle
    val icons = appIcons()
    val baseContainer = when {
        !source.enabled -> MaterialTheme.colorScheme.surfaceContainerLowest
        hasUpdate -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val containerColor = if (isMiuix) baseContainer.copy(alpha = 0.82f) else baseContainer

    ThemedCard(
        containerColor = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    FlowRow(
                        verticalArrangement = Arrangement.Center,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            source.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (source.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                        )
                        // Provenance Badge
                        ProvenanceBadge(source.provenance)
                        if (source.prerelease) {
                            Text(
                                "Pre-release",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        source.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ThemedSwitch(
                    checked = source.enabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                )
                ThemedIconButton(onClick = onRemove) {
                    Icon(icons.delete, contentDescription = stringResource(R.string.action_remove))
                }
            }

            Spacer(Modifier.height(8.dp))

            // Refresh status & relative time
            val downloadProgress = state.sourceDownloadProgress[source.id]
            val statusText = when {
                downloadProgress != null -> {
                    if (downloadProgress.isInstalling) {
                        stringResource(R.string.source_installing)
                    } else if (downloadProgress.progress >= 0f) {
                        val pct = (downloadProgress.progress * 100).toInt()
                        if (downloadProgress.totalBundles > 1) {
                            "Downloading (${downloadProgress.bundleIndex}/${downloadProgress.totalBundles}) ${downloadProgress.bundleName}: $pct%"
                        } else {
                            "Downloading update... $pct%"
                        }
                    } else {
                        if (downloadProgress.bundleName.isNotBlank()) {
                            "Downloading ${downloadProgress.bundleName}..."
                        } else {
                            "Downloading update..."
                        }
                    }
                }
                busy -> "Downloading update..."
                release == null -> stringResource(R.string.source_checking)
                release.isFailure -> stringResource(R.string.source_error, release.exceptionOrNull()?.message.orEmpty())
                source.installedVersion == null -> stringResource(R.string.source_available, release.getOrThrow().version)
                hasUpdate -> stringResource(R.string.source_update_available, source.installedVersion.orEmpty(), release.getOrThrow().version)
                else -> stringResource(R.string.source_up_to_date, source.installedVersion.orEmpty())
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (release?.isFailure == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (source.lastRefreshTime > 0L) {
                    Text(
                        stringResource(R.string.last_refreshed, formatRelativeTime(source.lastRefreshTime)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            if ((downloadProgress != null && !downloadProgress.isInstalling) || (busy && downloadProgress == null)) {
                Spacer(Modifier.height(6.dp))
                if (downloadProgress != null && downloadProgress.progress >= 0f) {
                    LinearProgressIndicator(
                        progress = { downloadProgress.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    )
                }
            }

            // Error notice banner if last attempt failed but cached release is kept
            if (source.lastError != null && release?.isSuccess == true) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Refresh error: ${source.lastError} (using cached release)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            // Show individual bundle assets in the release if available
            val latest = release?.getOrNull()
            if (latest != null && latest.bundles.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    latest.bundles.forEach { (bundleFileName, _) ->
                        Text(
                            text = bundleFileName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ThemedIconButton(
                    onClick = onRefresh,
                    enabled = !busy,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = icons.refresh,
                        contentDescription = stringResource(R.string.action_refresh),
                        modifier = Modifier.size(18.dp),
                    )
                }

                if (latest != null && latest.changelog.isNotBlank()) {
                    ThemedCompactTonalButton(
                        label = stringResource(R.string.action_changelog),
                        onClick = { onChangelog(latest.version to latest.changelog) },
                    )
                }
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else if (source.enabled) {
                    ThemedCompactFilledButton(
                        label = stringResource(
                            when {
                                source.installedVersion == null -> R.string.action_install
                                hasUpdate -> R.string.action_update
                                else -> R.string.action_reinstall
                            }
                        ),
                        icon = if (hasUpdate) icons.update else icons.install,
                        onClick = onInstall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProvenanceBadge(provenance: SourceProvenance) {
    val (label, bg, fg) = when (provenance) {
        SourceProvenance.OFFICIAL -> Triple(
            stringResource(R.string.provenance_official),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        SourceProvenance.NEXALLOY -> Triple(
            stringResource(R.string.provenance_nexalloy),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        SourceProvenance.COMMUNITY -> Triple(
            stringResource(R.string.provenance_community),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        SourceProvenance.CUSTOM -> Triple(
            stringResource(R.string.provenance_custom),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BundleCard(
    bundle: InstalledBundle,
    sources: List<PatchSource>,
    onEnabled: (Boolean) -> Unit,
    onUninstall: () -> Unit,
) {
    val source = sources.find { it.id == bundle.sourceId }

    val isMiuix = isMiuixStyle
    val baseContainer = if (bundle.enabled) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLowest
    val containerColor = if (isMiuix) baseContainer.copy(alpha = 0.82f) else baseContainer

    ThemedCard(
        containerColor = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${bundle.name} v${bundle.version}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (bundle.author.isNotEmpty()) {
                            Text(
                                "by ${bundle.author}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (source != null) {
                            Text(
                                "• ${source.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                ThemedSwitch(checked = bundle.enabled, onCheckedChange = onEnabled)
            }

            if (bundle.description.isNotEmpty()) {
                Text(
                    bundle.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            // Targeted apps chips: only the apps present on this device, so a bundle stops
            // naming apps the user has uninstalled.
            val context = LocalContext.current
            val appNames = AppDiscovery.installedAppNames(bundle.apps) { isInstalled(context, it) }
            if (appNames.isEmpty()) {
                Text(
                    stringResource(R.string.bundle_no_installed_apps),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    appNames.forEach { appName ->
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (isMiuix) {
                    ThemedTextButton(
                        label = stringResource(R.string.action_uninstall),
                        onClick = onUninstall,
                    )
                } else {
                    TextButton(onClick = onUninstall) {
                        Text(
                            stringResource(R.string.action_uninstall),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddSourceDialog(
    existingSources: List<PatchSource>,
    onDismiss: () -> Unit,
    onAdd: (String, String, Boolean) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var prerelease by remember { mutableStateOf(false) }

    val isValid = url.trim().isNotBlank()
    val isMiuix = isMiuixStyle

    ThemedDialog(
        show = true,
        title = stringResource(R.string.add_source_title),
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.action_add),
        confirmEnabled = isValid,
        onConfirm = { onAdd(name, url, prerelease) },
        dismissText = stringResource(android.R.string.cancel),
        onDismiss = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Trust Warning Alert
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    appIcons().security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.add_source_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            // Curated Presets Quick-Add
            val availablePresets = PatchSource.PRESET_SOURCES.filter { preset ->
                existingSources.none { it.url.equals(preset.url, ignoreCase = true) }
            }
            if (availablePresets.isNotEmpty()) {
                Text(
                    stringResource(R.string.preset_sources_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    availablePresets.forEach { preset ->
                        if (isMiuix) {
                            ThemedChip(
                                label = preset.name,
                                selected = false,
                                onClick = {
                                    name = preset.name
                                    url = preset.url
                                },
                            )
                        } else {
                            SuggestionChip(
                                onClick = {
                                    name = preset.name
                                    url = preset.url
                                },
                                label = { Text(preset.name, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                }
            }

            ThemedTextField(
                value = url,
                onValueChange = { url = it },
                label = stringResource(R.string.source_url_hint),
                modifier = Modifier.fillMaxWidth(),
            )
            ThemedTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.source_name_hint),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                ThemedCheckbox(checked = prerelease, onCheckedChange = { prerelease = it })
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.source_prerelease))
            }
        }
    }
}
