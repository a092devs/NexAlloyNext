package io.github.nexalloy.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.nexalloy.core.R
import io.github.nexalloy.ui.screens.AppPatchesScreen
import io.github.nexalloy.ui.screens.HomeScreen
import io.github.nexalloy.ui.screens.PatchesScreen
import io.github.nexalloy.ui.screens.SettingsScreen
import io.github.nexalloy.ui.theme.NexAlloyTheme
import io.github.nexalloy.ui.theme.isMiuixStyle
import io.github.nexalloy.updates.AppSettings
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.Settings as MiuixSettings
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : ComponentActivity() {

    /** Destination requested by a notification, consumed by the navigation. */
    private var requestedDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestedDestination = intent.getStringExtra(EXTRA_DESTINATION)
        setContent {
            val viewModel: ManagerViewModel = viewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            NexAlloyTheme(
                themeModeRaw = state.currentTheme,
                themeStyle = state.currentThemeStyle,
                colorSchemeRaw = state.currentColorScheme,
            ) {
                ManagerApp(
                    viewModel = viewModel,
                    requestedDestination = requestedDestination,
                    floatingNavigation = state.floatingNavigation,
                ) { requestedDestination = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        requestedDestination = intent.getStringExtra(EXTRA_DESTINATION)
    }

    companion object {
        const val EXTRA_DESTINATION = "destination"
        const val DESTINATION_HOME = "home"
        const val DESTINATION_PATCHES = "patches"
        const val DESTINATION_SETTINGS = "settings"
        const val DESTINATION_APP = "app"
    }
}

private class TopLevel(
    val route: String,
    val label: Int,
    val m3Icon: ImageVector,
    val miuixIcon: ImageVector,
)

private val topLevel = listOf(
    TopLevel(MainActivity.DESTINATION_HOME, R.string.nav_home, Icons.Filled.Home, MiuixIcons.GridView),
    TopLevel(MainActivity.DESTINATION_PATCHES, R.string.nav_patches, Icons.Filled.Build, MiuixIcons.Layers),
    TopLevel(MainActivity.DESTINATION_SETTINGS, R.string.nav_settings, Icons.Filled.Settings, MiuixIcons.MiuixSettings),
)

/** One floating navigation item; fixed size so the selection pill can slide by index. */
private val FloatingItemWidth = 92.dp
private val FloatingItemHeight = 64.dp

fun NavHostController.navigateTopLevel(route: String) = navigate(route) {
    popUpTo(MainActivity.DESTINATION_HOME) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/**
 * Screen transitions shared by both design systems: a short fade with a slight horizontal
 * slide keeps switching between screens fluid without feeling heavy.
 */
private val navEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(220)) +
        slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { it / 14 }
}

private val navExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(160))
}

private val navPopEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(220)) +
        slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { -it / 14 }
}

private val navPopExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(160))
}

@Composable
private fun ManagerApp(
    viewModel: ManagerViewModel,
    requestedDestination: String?,
    floatingNavigation: Boolean,
    onDestinationHandled: () -> Unit,
) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val isMiuix = isMiuixStyle

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(requestedDestination) {
        if (requestedDestination != null) {
            navController.navigateTopLevel(requestedDestination)
            onDestinationHandled()
        }
    }
    NotificationPermission()

    if (isMiuix) {
        MiuixManagerApp(
            navController = navController,
            viewModel = viewModel,
            route = route,
            floatingNavigation = floatingNavigation,
        )
    } else {
        Material3ManagerApp(
            navController = navController,
            viewModel = viewModel,
            route = route,
            snackbar = snackbar,
        )
    }
}

/**
 * Standard AndroidX Jetpack Compose Material 3 presentation.
 */
@Composable
private fun Material3ManagerApp(
    navController: NavHostController,
    viewModel: ManagerViewModel,
    route: String?,
    snackbar: SnackbarHostState,
) {
    val isTopLevel = topLevel.any { it.route == route }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    topLevel.forEach { item ->
                        NavigationBarItem(
                            selected = route == item.route,
                            onClick = { navController.navigateTopLevel(item.route) },
                            icon = { Icon(item.m3Icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController,
            startDestination = MainActivity.DESTINATION_HOME,
            enterTransition = navEnterTransition,
            exitTransition = navExitTransition,
            popEnterTransition = navPopEnterTransition,
            popExitTransition = navPopExitTransition,
        ) {
            composable(MainActivity.DESTINATION_HOME) {
                HomeScreen(viewModel, navController, padding)
            }
            composable(MainActivity.DESTINATION_PATCHES) {
                PatchesScreen(viewModel, padding)
            }
            composable(MainActivity.DESTINATION_SETTINGS) {
                SettingsScreen(viewModel, padding)
            }
            composable(
                "${MainActivity.DESTINATION_APP}/{packageName}",
                arguments = listOf(navArgument("packageName") { type = NavType.StringType }),
            ) { entry ->
                AppPatchesScreen(viewModel, entry.arguments!!.getString("packageName")!!, navController)
            }
        }
    }
}

/**
 * Official Miuix (Xiaomi HyperOS) presentation.
 *
 * The floating navigation bar is an opt-in preference and disabled by default; the standard
 * Miuix [NavigationBar] is the default arrangement. Blur is only used when the runtime shader
 * support check passes, with an opaque surface fallback otherwise.
 */
@Composable
private fun MiuixManagerApp(
    navController: NavHostController,
    viewModel: ManagerViewModel,
    route: String?,
    floatingNavigation: Boolean,
) {
    val isTopLevel = topLevel.any { it.route == route }

    // The window background follows the *system* night mode while the app can force its own
    // light/dark preference, so the Miuix presentation paints the page itself: the same
    // charcoal/off-white tones the window otherwise provides (see values/themes.xml and
    // values-night/themes.xml), resolved from the app's own mode.
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isDark = when (state.currentTheme) {
        AppSettings.THEME_LIGHT -> false
        AppSettings.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
    ) {
        if (floatingNavigation) {
            MiuixFloatingNavigationApp(navController, viewModel, isTopLevel)
        } else {
            MiuixStandardNavigationApp(navController, viewModel, route, isTopLevel)
        }
    }
}

@Composable
private fun MiuixNavHost(
    navController: NavHostController,
    viewModel: ManagerViewModel,
    contentPadding: PaddingValues,
) {
    NavHost(navController, startDestination = MainActivity.DESTINATION_HOME) {
        composable(MainActivity.DESTINATION_HOME) {
            HomeScreen(viewModel, navController, contentPadding)
        }
        composable(MainActivity.DESTINATION_PATCHES) {
            PatchesScreen(viewModel, contentPadding)
        }
        composable(MainActivity.DESTINATION_SETTINGS) {
            SettingsScreen(viewModel, contentPadding)
        }
        composable(
            "${MainActivity.DESTINATION_APP}/{packageName}",
            arguments = listOf(navArgument("packageName") { type = NavType.StringType }),
        ) { entry ->
            AppPatchesScreen(viewModel, entry.arguments!!.getString("packageName")!!, navController)
        }
    }
}

/**
 * Default Miuix arrangement: a standard navigation bar that occupies layout space, so no
 * content is ever hidden behind it.
 */
@Composable
private fun MiuixStandardNavigationApp(
    navController: NavHostController,
    viewModel: ManagerViewModel,
    route: String?,
    isTopLevel: Boolean,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            MiuixNavHost(
                navController = navController,
                viewModel = viewModel,
                contentPadding = PaddingValues(bottom = 16.dp),
            )
        }
        if (isTopLevel) {
            MiuixNavigationBar {
                topLevel.forEach { item ->
                    MiuixNavigationBarItem(
                        selected = route == item.route,
                        onClick = { navController.navigateTopLevel(item.route) },
                        icon = item.miuixIcon,
                        label = stringResource(item.label),
                    )
                }
            }
        }
    }
}

/**
 * Opt-in floating navigation bar with an official blur backdrop when runtime shader support is
 * available, and an opaque surface otherwise.
 */
@Composable
private fun MiuixFloatingNavigationApp(
    navController: NavHostController,
    viewModel: ManagerViewModel,
    isTopLevel: Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val isDark = when (state.currentTheme) {
        AppSettings.THEME_LIGHT -> false
        AppSettings.THEME_DARK -> true
        else -> systemDark
    }
    val route = navController.currentBackStackEntryAsState().value?.destination?.route

    val blurSupported = isRuntimeShaderSupported()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = if (blurSupported) {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    } else null
    val blurActive = backdrop != null

    val floatingBarShape = RoundedCornerShape(FloatingToolbarDefaults.CornerRadius)
    val floatingBarColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer
    val floatingHighlight = remember(isDark) {
        if (isDark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content container with layerBackdrop attached to capture scrolling content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
        ) {
            // Give 96dp bottom clearance on top-level screens so the floating bar never covers content
            val bottomPadding = if (isTopLevel) 96.dp else 16.dp
            MiuixNavHost(
                navController = navController,
                viewModel = viewModel,
                contentPadding = PaddingValues(bottom = bottomPadding),
            )
        }

        // Official Miuix FloatingNavigationBar with backdrop blur; the bar reserves the gesture
        // inset itself, so only a small extra margin is added here. It slides in and out while
        // the selection pill animates between the items.
        AnimatedVisibility(
            visible = isTopLevel,
            enter = slideInVertically(
                animationSpec = tween(320, easing = FastOutSlowInEasing),
                initialOffsetY = { it },
            ) + fadeIn(animationSpec = tween(220)),
            exit = slideOutVertically(
                animationSpec = tween(220),
                targetOffsetY = { it / 2 },
            ) + fadeOut(animationSpec = tween(160)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                FloatingNavigationBar(
                    modifier = if (blurActive) {
                        Modifier.textureBlur(
                            backdrop = backdrop,
                            shape = floatingBarShape,
                            blurRadius = 25f,
                            colors = BlurDefaults.blurColors(
                                blendColors = listOf(
                                    BlendColorEntry(color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f)),
                                ),
                            ),
                            highlight = floatingHighlight,
                        )
                    } else {
                        Modifier
                    },
                    color = floatingBarColor,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    MiuixFloatingNavItems(
                        selectedRoute = route,
                        isDark = isDark,
                        onSelect = { navController.navigateTopLevel(it) },
                    )
                }
            }
        }
    }
}

/**
 * Items of the floating navigation bar with a selection pill that slides between the entries.
 * The pill position derives from the fixed item width, so the slide stays in sync without any
 * extra measurement.
 */
@Composable
private fun MiuixFloatingNavItems(
    selectedRoute: String?,
    isDark: Boolean,
    onSelect: (String) -> Unit,
) {
    val selectedIndex = topLevel
        .indexOfFirst { it.route == selectedRoute }
        .coerceAtLeast(0)
    val animatedIndex = remember { Animatable(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex) {
        animatedIndex.animateTo(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        )
    }
    val itemWidthPx = with(LocalDensity.current) { FloatingItemWidth.toPx() }
    val indicatorColor = MiuixTheme.colorScheme.primary.copy(alpha = if (isDark) 0.26f else 0.16f)

    Box {
        // Selection pill: inset inside the item so its stadium corners stay clear of the bar's
        // rounded ends while it slides, and stay concentric with the item slot.
        Box(
            modifier = Modifier
                .offset { IntOffset((animatedIndex.value * itemWidthPx).roundToInt(), 0) }
                .size(width = FloatingItemWidth, height = FloatingItemHeight),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = FloatingItemWidth - 12.dp, height = FloatingItemHeight - 12.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(indicatorColor),
            )
        }
        Row {
            topLevel.forEach { item ->
                MiuixFloatingNavItem(
                    selected = selectedRoute == item.route,
                    onClick = { onSelect(item.route) },
                    icon = item.miuixIcon,
                    label = stringResource(item.label),
                )
            }
        }
    }
}

/**
 * Item of the floating navigation bar: the official icon treatment plus the label under the
 * glyph, sized for a comfortable touch target. Selection animates the tint and a subtle scale.
 */
@Composable
private fun MiuixFloatingNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val baseColor = MiuixTheme.colorScheme.onSurfaceContainer
    val targetAlpha = when {
        pressed -> if (selected) 0.55f else 0.75f
        selected -> 1f
        else -> 0.62f
    }
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(180),
        label = "floatingNavItemAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.95f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "floatingNavItemScale",
    )
    val tint = baseColor.copy(alpha = alpha)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(width = FloatingItemWidth, height = FloatingItemHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(17.dp))
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = tint,
            maxLines = 1,
        )
    }
}

/** Update notifications need the permission on Android 13 and newer; asked for once. */
@Composable
private fun NotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE)
        val granted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted && !prefs.getBoolean("notification_permission_asked", false)) {
            prefs.edit().putBoolean("notification_permission_asked", true).apply()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
