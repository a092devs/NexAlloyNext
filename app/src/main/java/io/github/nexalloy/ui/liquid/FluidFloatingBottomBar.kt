package io.github.nexalloy.ui.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.roundToInt

data class FluidTabItem(
    val route: String,
    val label: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val LocalFluidTabScale = staticCompositionLocalOf { { 1f } }

/**
 * Liquid glass floating navigation bar with fluid spring physics,
 * continuous touch drag navigation, velocity-based liquid deformation,
 * and measured layout bounds ensuring perfect alignment across all screen sizes.
 */
@Composable
fun FluidFloatingBottomBar(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    tabs: List<FluidTabItem>,
    isTranslucent: Boolean,
    isDark: Boolean,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
) {
    val tabsCount = tabs.size
    val pillShape = remember { CircleShape }
    val primaryColor = MiuixTheme.colorScheme.primary
    val baseColor = MiuixTheme.colorScheme.onSurfaceContainer
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainer

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()

    val barHeight = 62.dp
    val pillHeight = 48.dp
    val tabWidth = 86.dp
    val outerHorizontalPadding = 6.dp

    // Measured layout bounds for each tab relative to the inner content box
    val tabLefts = remember(tabsCount) { mutableStateListOf<Float>().apply { repeat(tabsCount) { add(0f) } } }
    val tabWidths = remember(tabsCount) { mutableStateListOf<Float>().apply { repeat(tabsCount) { add(0f) } } }

    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    val onSelectedUpdated by rememberUpdatedState(onSelected)

    fun indexAt(positionX: Float): Int {
        val hasMeasured = tabWidths.isNotEmpty() && tabWidths[0] > 0f
        if (!hasMeasured) return currentIndex
        val paddingPx = with(density) { outerHorizontalPadding.toPx() }
        val contentX = if (isLtr) positionX - paddingPx else (tabWidths.sum() - (positionX - paddingPx))
        for (i in 0 until tabsCount) {
            val left = tabLefts.getOrElse(i) { 0f }
            val width = tabWidths.getOrElse(i) { 0f }
            if (contentX in left..(left + width)) {
                return i
            }
        }
        val firstLeft = tabLefts.getOrElse(0) { 0f }
        if (contentX < firstLeft) return 0
        return tabsCount - 1
    }

    val dampedDragAnimation = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = -0.3f..(tabsCount - 1 + 0.3f),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.0f,
            canDrag = { true },
            onDragStarted = { position ->
                updateValue(indexAt(position.x).toFloat())
            },
            onDragStopped = {
                val targetIndex = targetValue.roundToInt().coerceIn(0, tabsCount - 1)
                if (currentIndex != targetIndex) {
                    currentIndex = targetIndex
                    onSelectedUpdated(targetIndex)
                }
                animateToValue(targetIndex.toFloat())
            },
            onDragCancelled = {
                animateToValue(currentIndex.toFloat())
            },
            onDrag = { _, dragAmount ->
                val avgWidth = tabWidths.firstOrNull { it > 0f } ?: with(density) { tabWidth.toPx() }
                if (avgWidth > 0f && dragAmount.x != 0f) {
                    val delta = (dragAmount.x / avgWidth) * if (isLtr) 1f else -1f
                    val newTarget = (targetValue + delta).coerceIn(-0.3f, (tabsCount - 1) + 0.3f)
                    updateValue(newTarget)
                }
            },
        )
    }

    LaunchedEffect(selectedIndex) {
        if (currentIndex != selectedIndex) {
            currentIndex = selectedIndex
        }
        if (dampedDragAnimation.targetValue.roundToInt() != selectedIndex) {
            dampedDragAnimation.animateToValue(selectedIndex.toFloat())
        }
    }

    fun activateTab(index: Int) {
        if (index !in 0 until tabsCount) return
        if (currentIndex != index) {
            currentIndex = index
            onSelectedUpdated(index)
        }
        dampedDragAnimation.animateToValue(index.toFloat())
    }

    val glassHighlight = remember(isDark) {
        if (isDark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight
    }

    // Precise indicator coordinates with dynamic edge dampening
    val currentProgress = dampedDragAnimation.value
    val hasMeasured = tabWidths.isNotEmpty() && tabWidths[0] > 0f

    val rawLeftPx: Float
    val rawWidthPx: Float

    if (hasMeasured) {
        val lastIndex = tabsCount - 1
        if (currentProgress <= 0f) {
            val left0 = tabLefts[0]
            val width0 = tabWidths[0]
            rawLeftPx = left0 + currentProgress * (width0 * 0.25f)
            rawWidthPx = width0
        } else if (currentProgress >= lastIndex.toFloat()) {
            val leftLast = tabLefts[lastIndex]
            val widthLast = tabWidths[lastIndex]
            val over = currentProgress - lastIndex.toFloat()
            rawLeftPx = leftLast + over * (widthLast * 0.25f)
            rawWidthPx = widthLast
        } else {
            val baseIndex = currentProgress.toInt().coerceIn(0, lastIndex - 1)
            val nextIndex = baseIndex + 1
            val fraction = currentProgress - baseIndex
            val leftA = tabLefts[baseIndex]
            val widthA = tabWidths[baseIndex]
            val leftB = tabLefts[nextIndex]
            val widthB = tabWidths[nextIndex]
            rawLeftPx = leftA + (leftB - leftA) * fraction
            rawWidthPx = widthA + (widthB - widthA) * fraction
        }
    } else {
        val fallbackWidthPx = with(density) { tabWidth.toPx() }
        rawLeftPx = currentProgress * fallbackWidthPx
        rawWidthPx = fallbackWidthPx
    }

    val horizontalInsetPx = with(density) { 4.dp.toPx() }
    val pillLeftPx = rawLeftPx + horizontalInsetPx
    val pillWidthPx = (rawWidthPx - horizontalInsetPx * 2f).coerceAtLeast(0f)
    val pillWidthDp = with(density) { pillWidthPx.toDp() }

    val velocity = dampedDragAnimation.velocity
    val stretch = (abs(velocity) * 0.08f).coerceIn(0f, 0.14f)

    Box(
        modifier = modifier
            .width(IntrinsicSize.Min)
            .then(dampedDragAnimation.modifier),
        contentAlignment = Alignment.CenterStart,
    ) {
        // Outer Bar Container Background
        val outerModifier = Modifier
            .shadow(
                elevation = 8.dp,
                shape = pillShape,
                spotColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.18f),
            )
            .then(
                if (isTranslucent && backdrop != null) {
                    Modifier
                        .textureBlur(
                            backdrop = backdrop,
                            shape = pillShape,
                            blurRadius = 25f,
                            colors = BlurDefaults.blurColors(
                                blendColors = listOf(
                                    BlendColorEntry(color = surfaceContainer.copy(alpha = if (isDark) 0.30f else 0.45f)),
                                ),
                            ),
                            highlight = glassHighlight,
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.40f),
                            shape = pillShape,
                        )
                } else {
                    Modifier
                        .clip(pillShape)
                        .background(surfaceContainer)
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                            shape = pillShape,
                        )
                }
            )
            .height(barHeight)
            .padding(horizontal = outerHorizontalPadding)

        Box(
            modifier = outerModifier,
            contentAlignment = Alignment.CenterStart,
        ) {
            // Fluid moving indicator pill
            Box(
                Modifier
                    .graphicsLayer {
                        translationX = pillLeftPx
                        scaleX = 1f + stretch
                        scaleY = 1f - (stretch * 0.25f)
                    }
                    .clip(pillShape)
                    .background(
                        color = primaryColor.copy(alpha = if (isDark) 0.26f else 0.18f),
                        shape = pillShape,
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.30f),
                        shape = pillShape,
                    )
                    .height(pillHeight)
                    .width(pillWidthDp)
            )

            // Tab items row positioned on top
            Row(
                modifier = Modifier
                    .selectableGroup()
                    .height(barHeight),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                tabs.forEachIndexed { index, item ->
                    val isTabSelected = index == currentIndex
                    val distance = abs(dampedDragAnimation.value - index)
                    val proximity = (1f - distance).coerceIn(0f, 1f)
                    val tabTint = lerp(baseColor.copy(alpha = 0.65f), primaryColor, proximity)

                    Column(
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                val parent = coords.parentCoordinates
                                if (parent != null) {
                                    val pos = parent.localPositionOf(coords, Offset.Zero)
                                    if (index < tabLefts.size) {
                                        tabLefts[index] = pos.x
                                        tabWidths[index] = coords.size.width.toFloat()
                                    }
                                }
                            }
                            .semantics(mergeDescendants = true) {
                                this.selected = isTabSelected
                                role = Role.Tab
                                onClick {
                                    activateTab(index)
                                    true
                                }
                            }
                            .focusable()
                            .fillMaxHeight()
                            .width(tabWidth)
                            .padding(horizontal = 6.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = if (distance < 0.45f) item.selectedIcon else item.unselectedIcon,
                            contentDescription = stringResource(item.label),
                            tint = tabTint,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    val scale = 1f + proximity * 0.08f
                                    scaleX = scale
                                    scaleY = scale
                                },
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(item.label),
                            fontSize = 11.sp,
                            fontWeight = if (distance < 0.45f) FontWeight.Bold else FontWeight.Normal,
                            color = tabTint,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
