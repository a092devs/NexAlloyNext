package io.github.nexalloy.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nexalloy.ui.theme.isMiuixStyle
import io.github.nexalloy.ui.theme.nexAlloyCardBorder
import io.github.nexalloy.ui.theme.nexAlloyCardShape
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Checkbox as MiuixCheckbox
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.RadioButton as MiuixRadioButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add as MiuixAddIcon
import top.yukonga.miuix.kmp.icon.extended.Back as MiuixBackIcon
import top.yukonga.miuix.kmp.icon.extended.Clear as MiuixClearIcon
import top.yukonga.miuix.kmp.icon.extended.Delete as MiuixDeleteIcon
import top.yukonga.miuix.kmp.icon.extended.Download as MiuixDownloadIcon
import top.yukonga.miuix.kmp.icon.extended.File as MiuixFileIcon
import top.yukonga.miuix.kmp.icon.extended.Info as MiuixInfoIcon
import top.yukonga.miuix.kmp.icon.extended.Lock as MiuixLockIcon
import top.yukonga.miuix.kmp.icon.extended.More as MiuixMoreIcon
import top.yukonga.miuix.kmp.icon.extended.Ok as MiuixOkIcon
import top.yukonga.miuix.kmp.icon.extended.Refresh as MiuixRefreshIcon
import top.yukonga.miuix.kmp.icon.extended.Search as MiuixSearchIcon
import top.yukonga.miuix.kmp.icon.extended.Update as MiuixUpdateIcon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.window.WindowListPopup
import kotlin.math.roundToInt

/**
 * One deliberate, theme-specific icon set per design system: the official Miuix icons while the
 * Miuix theme is active, Material icons with Material 3. The official Miuix set has no glyph for a
 * few states (e.g. a warning triangle); those keep the Material glyph in both themes.
 */
data class AppIcons(
    val search: ImageVector,
    val clear: ImageVector,
    val add: ImageVector,
    val delete: ImageVector,
    val refresh: ImageVector,
    val more: ImageVector,
    val back: ImageVector,
    val install: ImageVector,
    val update: ImageVector,
    val info: ImageVector,
    val check: ImageVector,
    val bundle: ImageVector,
    val security: ImageVector,
    /** Kept from the Material set in both themes; the official Miuix set has no warning glyph. */
    val warning: ImageVector = Icons.Filled.Warning,
)

@Composable
fun appIcons(): AppIcons {
    val isMiuix = isMiuixStyle
    return remember(isMiuix) {
        if (isMiuix) {
            AppIcons(
                search = MiuixIcons.MiuixSearchIcon,
                clear = MiuixIcons.MiuixClearIcon,
                add = MiuixIcons.MiuixAddIcon,
                delete = MiuixIcons.MiuixDeleteIcon,
                refresh = MiuixIcons.MiuixRefreshIcon,
                more = MiuixIcons.MiuixMoreIcon,
                back = MiuixIcons.MiuixBackIcon,
                install = MiuixIcons.MiuixDownloadIcon,
                update = MiuixIcons.MiuixUpdateIcon,
                info = MiuixIcons.MiuixInfoIcon,
                check = MiuixIcons.MiuixOkIcon,
                bundle = MiuixIcons.MiuixFileIcon,
                security = MiuixIcons.MiuixLockIcon,
            )
        } else {
            AppIcons(
                search = Icons.Filled.Search,
                clear = Icons.Filled.Clear,
                add = Icons.Filled.Add,
                delete = Icons.Filled.Delete,
                refresh = Icons.Filled.Refresh,
                more = Icons.Filled.MoreVert,
                back = Icons.AutoMirrored.Filled.ArrowBack,
                install = Icons.Outlined.CloudDownload,
                update = Icons.Outlined.SystemUpdateAlt,
                info = Icons.Filled.Info,
                check = Icons.Filled.CheckCircle,
                bundle = Icons.Outlined.FolderZip,
                security = Icons.Filled.Security,
            )
        }
    }
}

/** Section header: Miuix [SmallTitle] while Miuix is active, the shared M3 style otherwise. */
@Composable
fun SectionTitle(text: String) {
    if (isMiuixStyle) {
        // The stock onBackgroundVariant sits at ~4.2:1 on the dark window background; the
        // summary role keeps the section label legible while staying secondary in hierarchy.
        SmallTitle(
            text = text,
            textColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            insideMargin = MiuixSectionTitleMargin,
        )
    } else {
        SectionHeader(text)
    }
}

/**
 * Miuix section titles default to a 28dp start inset while the row text inside cards starts at
 * 36dp; this margin lines the heading up with the row text column for a tidy layout.
 */
val MiuixSectionTitleMargin = PaddingValues(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 8.dp)

/**
 * Miuix segmented control for in-page tabs: one stadium container with a selection pill that
 * slides between equal segments, sharing the floating navigation bar's motion. Unlike the stock
 * Miuix TabRow the pill is inset with concentric corners and the container draws no stroke, so no
 * edge slivers or stray border lines appear at the corners.
 */
@Composable
fun MiuixSegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val corner = 24.dp
    val gap = 3.dp
    val animatedIndex = remember { Animatable(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex) {
        animatedIndex.animateTo(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        )
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(corner * 2)
            .clip(RoundedCornerShape(corner))
            .background(MiuixTheme.colorScheme.surfaceContainer),
    ) {
        val segmentWidth = (maxWidth - gap * 2) / tabs.size
        val slidePx = with(LocalDensity.current) { segmentWidth.toPx() }
        val indicatorColor = MiuixTheme.colorScheme.primary.copy(alpha = if (isDark) 0.26f else 0.16f)
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (gap.toPx() + animatedIndex.value * slidePx).roundToInt(),
                        y = gap.toPx().roundToInt(),
                    )
                }
                .size(width = segmentWidth, height = corner * 2 - gap * 2)
                .clip(RoundedCornerShape(corner - gap))
                .background(indicatorColor),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(index) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) {
                            MiuixTheme.colorScheme.onSurfaceContainer
                        } else {
                            MiuixTheme.colorScheme.onSurfaceSecondary
                        },
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Card surface on the active design system: the official Miuix card (which uses the Miuix surface
 * roles) or the Material 3 card with the shared shape, border, and container colour.
 */
@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (isMiuixStyle) {
        MiuixCard(modifier = modifier, onClick = onClick) { content() }
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = nexAlloyCardShape(),
            border = nexAlloyCardBorder(),
            modifier = modifier,
        ) { content() }
    }
}

/** Switch: the official Miuix switch or the Material 3 switch, matching the active theme. */
@Composable
fun ThemedSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (isMiuixStyle) {
        MiuixSwitch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled)
    } else {
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled)
    }
}

/** Checkbox: the official Miuix checkbox or the Material 3 checkbox. */
@Composable
fun ThemedCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isMiuixStyle) {
        MiuixCheckbox(
            state = ToggleableState(checked),
            onClick = { onCheckedChange(!checked) },
            modifier = modifier,
        )
    } else {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
    }
}

/** Icon-only button on the active theme's press semantics. */
@Composable
fun ThemedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (isMiuixStyle) {
        MiuixIconButton(onClick = onClick, modifier = modifier, enabled = enabled) { content() }
    } else {
        IconButton(onClick = onClick, modifier = modifier, enabled = enabled) { content() }
    }
}

/** Text-only action button. */
@Composable
fun ThemedTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (isMiuixStyle) {
        MiuixTextButton(text = label, onClick = onClick, modifier = modifier, enabled = enabled)
    } else {
        TextButton(onClick = onClick, modifier = modifier, enabled = enabled) { Text(label) }
    }
}

/** Emphasised action button, optionally with a leading icon. */
@Composable
fun ThemedFilledButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    if (isMiuixStyle) {
        MiuixButton(onClick = onClick, modifier = modifier, enabled = enabled) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            }
            Text(label)
        }
    } else {
        FilledTonalButton(onClick = onClick, modifier = modifier, enabled = enabled) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            }
            Text(label)
        }
    }
}

/** Secondary (outlined) action button. */
@Composable
fun ThemedOutlinedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (isMiuixStyle) {
        MiuixTextButton(text = label, onClick = onClick, modifier = modifier, enabled = enabled)
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled) { Text(label) }
    }
}

/** Selectable filter pill: a Miuix-styled capsule or the Material 3 filter chip. */
@Composable
fun ThemedChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isMiuixStyle) {
        val background = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.secondaryContainer
        // The stock dark pairing onSecondaryContainer (#7C7C7C) on secondaryContainer (#434343)
        // measures ~2.6:1; the surface-container content role keeps the capsule label readable.
        val content = if (selected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurfaceContainer
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            modifier = modifier
                .clip(CircleShape)
                .background(background)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 7.dp),
        )
    } else {
        FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, modifier = modifier)
    }
}

/**
 * Compact filled action button sized specifically for cards, preventing
 * oversized buttons while maintaining a comfortable touch target.
 */
@Composable
fun ThemedCompactFilledButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val isMiuix = isMiuixStyle
    val shape = RoundedCornerShape(17.dp)
    if (isMiuix) {
        val bg = if (enabled) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.disabledPrimaryButton
        val fg = if (enabled) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.disabledOnPrimaryButton
        Row(
            modifier = modifier
                .height(34.dp)
                .clip(shape)
                .background(bg)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier.height(34.dp),
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            shape = shape,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Compact secondary action button (e.g. Changelog).
 */
@Composable
fun ThemedCompactTonalButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val isMiuix = isMiuixStyle
    val shape = RoundedCornerShape(17.dp)
    if (isMiuix) {
        val bg = MiuixTheme.colorScheme.surfaceContainerHighest
        val fg = MiuixTheme.colorScheme.onSurfaceContainerHighest
        Row(
            modifier = modifier
                .height(34.dp)
                .clip(shape)
                .background(bg)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(34.dp),
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            shape = shape,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Single-line search field with a leading search glyph and a clear action.
 * Uses a centered capsule layout with precise vertical alignment between
 * the leading search glyph and the placeholder/input text on both MIUIX and Material 3.
 */
@Composable
fun ThemedSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val icons = appIcons()
    val isMiuix = isMiuixStyle
    val shape = RoundedCornerShape(24.dp)
    val backgroundColor = if (isMiuix) {
        MiuixTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (isMiuix) {
        MiuixTheme.colorScheme.onSurfaceContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val placeholderColor = if (isMiuix) {
        MiuixTheme.colorScheme.onSurfaceSecondary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconTint = if (isMiuix) {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .background(backgroundColor)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icons.search,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = placeholderColor,
                    fontSize = 15.sp,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = contentColor,
                    fontSize = 15.sp,
                ),
                cursorBrush = SolidColor(
                    if (isMiuix) MiuixTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Spacer(modifier = Modifier.width(6.dp))
            ThemedIconButton(
                onClick = { onValueChange("") },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = icons.clear,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Labelled single-line text input on the active theme. */
@Composable
fun ThemedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    if (isMiuixStyle) {
        MiuixTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            label = label,
            // See ThemedSearchField: keep the label above the stock low-contrast pairing.
            labelColor = MiuixTheme.colorScheme.onSurfaceSecondary,
            singleLine = singleLine,
        )
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            label = { Text(label) },
            singleLine = singleLine,
        )
    }
}

/**
 * Dialog on the active theme: the official Miuix [WindowDialog] with Miuix text buttons, or the
 * Material 3 [AlertDialog].
 */
@Composable
fun ThemedDialog(
    show: Boolean,
    title: String?,
    onDismissRequest: () -> Unit,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (!show) return

    if (isMiuixStyle) {
        WindowDialog(show = true, title = title, onDismissRequest = onDismissRequest) {
            Column {
                content()
                if (confirmText != null || dismissText != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (dismissText != null) {
                            MiuixTextButton(
                                text = dismissText,
                                onClick = { onDismiss?.invoke() ?: onDismissRequest() },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (confirmText != null) {
                            MiuixTextButton(
                                text = confirmText,
                                onClick = { onConfirm?.invoke() ?: onDismissRequest() },
                                enabled = confirmEnabled,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    } else {
        val titleValue = title
        val confirmValue = confirmText
        val dismissValue = dismissText
        val titleContent: (@Composable () -> Unit)? = if (titleValue != null) {
            { Text(titleValue) }
        } else {
            null
        }
        val confirmButton: (@Composable () -> Unit)? = if (confirmValue != null) {
            {
                TextButton(onClick = { onConfirm?.invoke() ?: onDismissRequest() }, enabled = confirmEnabled) {
                    Text(confirmValue)
                }
            }
        } else {
            null
        }
        val dismissButton: (@Composable () -> Unit)? = if (dismissValue != null) {
            {
                TextButton(onClick = { onDismiss?.invoke() ?: onDismissRequest() }) {
                    Text(dismissValue)
                }
            }
        } else {
            null
        }
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = titleContent,
            text = { Column { content() } },
            confirmButton = confirmButton ?: {},
            dismissButton = dismissButton,
        )
    }
}

/**
 * Single-choice dialog built on [ThemedDialog]: used for the design-system and colour-mode pickers
 * so those pages stay inside the active theme as well.
 */
@Composable
fun ThemedRadioDialog(
    show: Boolean,
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ThemedDialog(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        confirmText = stringResource(android.R.string.cancel),
        onConfirm = onDismissRequest,
    ) {
        Column {
            options.forEach { (value, label) ->
                val select = {
                    onSelect(value)
                    onDismissRequest()
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = select)
                        .padding(vertical = 8.dp),
                ) {
                    if (isMiuixStyle) {
                        MiuixRadioButton(selected = selected == value, onClick = select)
                    } else {
                        RadioButton(selected = selected == value, onClick = select)
                    }
                    Text(text = label, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

/**
 * Clean, readable, scrollable changelog view with markdown section headers,
 * clean bullet points, and comfortable typography.
 */
@Composable
fun ChangelogText(
    notes: String,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val isMiuix = isMiuixStyle
    val textColor = if (isMiuix) MiuixTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface
    val subtextColor = if (isMiuix) MiuixTheme.colorScheme.onSurfaceVariantSummary else MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = if (isMiuix) MiuixTheme.colorScheme.primary else MaterialTheme.colorScheme.primary

    if (notes.isBlank()) {
        Text(
            text = "No changelog details provided.",
            style = MaterialTheme.typography.bodyMedium,
            color = subtextColor,
            modifier = modifier.padding(vertical = 12.dp),
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .verticalScroll(scrollState)
            .padding(vertical = 4.dp),
    ) {
        val lines = notes.lines()
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("### ") || trimmed.startsWith("## ") || trimmed.startsWith("# ") -> {
                    val headerText = trimmed.removePrefix("### ").removePrefix("## ").removePrefix("# ")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = headerText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val bulletText = trimmed.substring(2).trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "•",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(start = 2.dp, end = 8.dp),
                        )
                        Text(
                            text = bulletText,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = textColor,
                        )
                    }
                }
                trimmed.isEmpty() -> {
                    Spacer(Modifier.height(6.dp))
                }
                else -> {
                    Text(
                        text = trimmed,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = textColor,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

/** One entry of a [ThemedMenu]. */
data class ThemedMenuItem(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
)

/** Overflow menu on the active theme: official Miuix popup or the Material 3 dropdown. */
@Composable
fun ThemedMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<ThemedMenuItem>,
    modifier: Modifier = Modifier,
) {
    if (isMiuixStyle) {
        if (!expanded) return
        WindowListPopup(show = true, popupModifier = modifier, onDismissRequest = onDismissRequest) {
            ListPopupColumn {
                items.forEachIndexed { index, item ->
                    DropdownImpl(
                        text = item.label,
                        optionSize = items.size,
                        isSelected = false,
                        index = index,
                        onSelectedIndexChange = {
                            onDismissRequest()
                            item.onClick()
                        },
                    )
                }
            }
        }
    } else {
        DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, modifier = modifier) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    leadingIcon = item.icon?.let { icon -> { Icon(icon, contentDescription = null) } },
                    onClick = {
                        onDismissRequest()
                        item.onClick()
                    },
                )
            }
        }
    }
}
