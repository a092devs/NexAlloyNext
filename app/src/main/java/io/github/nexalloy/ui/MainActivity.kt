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
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nexalloy.ui.liquid.FluidFloatingBottomBar
import io.github.nexalloy.ui.liquid.FluidTabItem
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
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.basic.SnackbarHost as MiuixSnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState as MiuixSnackbarHostState
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
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
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val topLevel = listOf(
    TopLevel(
        route = MainActivity.DESTINATION_HOME,
        label = R.string.nav_home,
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    TopLevel(
        route = MainActivity.DESTINATION_PATCHES,
        label = R.string.nav_patches,
        selectedIcon = Icons.Rounded.Extension,
        unselectedIcon = Icons.Outlined.Extension,
    ),
    TopLevel(
        route = MainActivity.DESTINATION_SETTINGS,
        label = R.string.nav_settings,
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings,
    ),
)

/** One floating navigation item; sized by content with a comfortable touch-target minimum. */
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
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val isMiuix = isMiuixStyle

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
) {
    val isTopLevel = topLevel.any { it.route == route }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (isTopLevel) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    topLevel.forEach { item ->
                        val selected = route == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateTopLevel(item.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = null,
                                )
                            },
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

    val snackbar = remember { MiuixSnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

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
    val canvasBg = MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasBg),
    ) {
        if (floatingNavigation) {
            MiuixFloatingNavigationApp(navController, viewModel, isTopLevel, snackbar)
        } else {
            MiuixStandardNavigationApp(navController, viewModel, route, isTopLevel, snackbar)
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
    snackbar: MiuixSnackbarHostState,
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
            MiuixSnackbarHost(
                state = snackbar,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (isTopLevel) {
            MiuixNavigationBar {
                topLevel.forEach { item ->
                    val selected = route == item.route
                    MiuixNavigationBarItem(
                        selected = selected,
                        onClick = { navController.navigateTopLevel(item.route) },
                        icon = if (selected) item.selectedIcon else item.unselectedIcon,
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
    snackbar: MiuixSnackbarHostState,
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
    val canvasBg = MaterialTheme.colorScheme.background
    val backdrop = if (blurSupported) {
        rememberLayerBackdrop {
            drawRect(canvasBg)
            drawContent()
        }
    } else null
    val blurActive = backdrop != null

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content container with layerBackdrop attached to capture scrolling content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(canvasBg)
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
        ) {
            // Give 96dp bottom clearance on top-level screens so the floating bar never covers content
            val bottomPadding = if (isTopLevel) 96.dp else 16.dp
            MiuixNavHost(
                navController = navController,
                viewModel = viewModel,
                contentPadding = PaddingValues(bottom = bottomPadding),
            )
            MiuixSnackbarHost(
                state = snackbar,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomPadding),
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
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        ) {
            val fluidTabs = remember {
                topLevel.map {
                    FluidTabItem(
                        route = it.route,
                        label = it.label,
                        selectedIcon = it.selectedIcon,
                        unselectedIcon = it.unselectedIcon,
                    )
                }
            }
            val selectedIndex = topLevel.indexOfFirst { it.route == route }.coerceAtLeast(0)
            FluidFloatingBottomBar(
                selectedIndex = selectedIndex,
                onSelected = { index ->
                    val target = topLevel.getOrNull(index) ?: topLevel[0]
                    navController.navigateTopLevel(target.route)
                },
                tabs = fluidTabs,
                isTranslucent = state.floatingBarTranslucent,
                isDark = isDark,
                backdrop = backdrop,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
        }
    }
}

/**
 * Items of the floating navigation bar. Each item handles its own selection state
 * without an awkward horizontal slide effect across tabs.
 */
@Composable
private fun MiuixFloatingNavItems(
    selectedRoute: String?,
    isDark: Boolean,
    onSelect: (String) -> Unit,
) {
    Row {
        topLevel.forEach { item ->
            MiuixFloatingNavItem(
                selected = selectedRoute == item.route,
                onClick = { onSelect(item.route) },
                item = item,
                isDark = isDark,
            )
        }
    }
}

/**
 * Item of the floating navigation bar: the official icon treatment plus the label under the
 * glyph, sized for a comfortable touch target. Selection uses an in-place pill indicator
 * without sliding across the bar.
 */
@Composable
private fun MiuixFloatingNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    item: TopLevel,
    isDark: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val baseColor = MiuixTheme.colorScheme.onSurfaceContainer
    val primaryColor = MiuixTheme.colorScheme.primary
    val indicatorColor = primaryColor.copy(alpha = if (isDark) 0.22f else 0.14f)

    val targetAlpha = when {
        pressed -> if (selected) 0.70f else 0.85f
        selected -> 1f
        else -> 0.65f
    }
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(150),
        label = "floatingNavItemAlpha",
    )
    val pillAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(150),
        label = "floatingNavItemPillAlpha",
    )
    val tint = if (selected) primaryColor else baseColor.copy(alpha = alpha)

    Box(
        modifier = Modifier
            .size(width = FloatingItemWidth, height = FloatingItemHeight)
            .clip(RoundedCornerShape(18.dp))
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (pillAlpha > 0f) {
            Box(
                modifier = Modifier
                    .size(width = FloatingItemWidth - 14.dp, height = FloatingItemHeight - 12.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(indicatorColor.copy(alpha = indicatorColor.alpha * pillAlpha)),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = stringResource(item.label),
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
