package io.github.nexalloy.ui.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

data class FluidTabItem(
    val route: String,
    val label: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val LocalFluidTabScale = staticCompositionLocalOf { { 1f } }

/**
 * iOS WhatsApp-style liquid glass floating navigation bar with fluid spring physics,
 * continuous touch-and-hold drag navigation, velocity-based liquid deformation,
 * and optional translucent backdrop refraction.
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

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { 5.dp.toPx() }
    val panelOffset by remember(rubberBandPx) {
        derivedStateOf {
            if (totalWidthPx == 0f) {
                0f
            } else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }
    }

    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    val onSelectedUpdated by rememberUpdatedState(onSelected)

    LaunchedEffect(selectedIndex) {
        if (currentIndex != selectedIndex) {
            currentIndex = selectedIndex
        }
    }

    fun indexAt(positionX: Float): Int {
        if (tabWidthPx == 0f) return currentIndex
        val horizontalPaddingPx = with(density) { 6.dp.toPx() }
        val logicalX = if (isLtr) positionX else totalWidthPx - positionX
        return ((logicalX - horizontalPaddingPx) / tabWidthPx)
            .toInt()
            .coerceIn(0, tabsCount - 1)
    }

    val dampedDragAnimation = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.0f,
            canDrag = { offset ->
                offset.x in 0f..totalWidthPx
            },
            onDragStarted = { position ->
                updateValue(indexAt(position.x).toFloat())
            },
            onDragStopped = {
                val targetIndex = targetValue.roundToInt().coerceIn(0, tabsCount - 1)
                if (currentIndex != targetIndex) {
                    currentIndex = targetIndex
                    onSelectedUpdated(targetIndex)
                }
                updateValue(targetIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 350f, 0.5f))
                }
            },
            onDragCancelled = {
                updateValue(currentIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 350f, 0.5f))
                }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0f && dragAmount.x != 0f) {
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .coerceIn(0f, (tabsCount - 1).toFloat()),
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            },
        )
    }

    LaunchedEffect(selectedIndex) {
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

    val barHeight = 62.dp
    val pillHeight = 48.dp

    Box(
        modifier = modifier
            .width(IntrinsicSize.Min)
            .graphicsLayer { translationX = panelOffset }
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
            .padding(horizontal = 6.dp)

        Box(
            modifier = outerModifier,
            contentAlignment = Alignment.CenterStart,
        ) {
            // Fluid moving indicator pill
            if (tabWidthPx > 0f) {
                val tabWidthDp = with(density) { tabWidthPx.toDp() }
                val progressOffset = dampedDragAnimation.value * tabWidthPx
                val pillTranslationX = if (isLtr) progressOffset else -progressOffset
                val velocity = dampedDragAnimation.velocity
                val stretch = (abs(velocity) * 0.10f).coerceIn(0f, 0.16f)

                Box(
                    Modifier
                        .graphicsLayer {
                            translationX = pillTranslationX
                            scaleX = 1f + stretch
                            scaleY = 1f - (stretch * 0.25f)
                        }
                        .padding(horizontal = 4.dp)
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
                        .width(tabWidthDp - 8.dp)
                )
            }

            // Tab items row positioned on top
            Row(
                modifier = Modifier
                    .onGloballyPositioned { coords ->
                        totalWidthPx = coords.size.width.toFloat()
                        val contentWidthPx = totalWidthPx - with(density) { 12.dp.toPx() }
                        tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                    }
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
                            .widthIn(min = 78.dp)
                            .padding(horizontal = 14.dp),
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
