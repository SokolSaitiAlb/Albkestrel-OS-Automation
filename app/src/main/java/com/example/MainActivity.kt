package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.preferences.ThemeMode
import com.example.ui.navigation.Screen
import com.example.ui.navigation.bottomNavItems
import com.example.ui.screens.CreateEditRoutineScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RoutinesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AlbkestrelTheme
import com.example.ui.viewmodel.CreateEditRoutineViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.RoutinesViewModel
import com.example.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AlbkestrelApp

        setContent {
            val themeMode by app.preferencesRepository.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            // Runtime permission request for notifications on Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* permission result handled */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!app.permissionManager.hasPostNotificationPermission()) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            AlbkestrelTheme(themeMode = themeMode) {
                AlbkestrelAppRoot(app = app)
            }
        }
    }
}

@Composable
fun AlbkestrelAppRoot(app: AlbkestrelApp) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelDestination = currentRoute in listOf(
        Screen.Home.route,
        Screen.Routines.route,
        Screen.Settings.route
    )

    // ViewModels with constructor injection from AlbkestrelApp
    val homeViewModel: HomeViewModel = viewModel {
        HomeViewModel(
            repository = app.repository,
            systemStateMonitor = app.systemStateMonitor,
            actionExecutor = app.actionExecutor,
            scheduler = app.scheduler
        )
    }

    val routinesViewModel: RoutinesViewModel = viewModel {
        RoutinesViewModel(
            repository = app.repository,
            actionExecutor = app.actionExecutor,
            scheduler = app.scheduler
        )
    }

    val settingsViewModel: SettingsViewModel = viewModel {
        SettingsViewModel(
            preferencesRepository = app.preferencesRepository,
            routineRepository = app.repository,
            permissionManager = app.permissionManager,
            context = app.applicationContext
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = isTopLevelDestination,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    modifier = Modifier.testTag("main_navigation_bar")
                ) {
                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) },
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onCreateRoutineClick = {
                        navController.navigate(Screen.CreateEditRoutine.createRoute())
                    },
                    onRoutineClick = { routineId ->
                        navController.navigate(Screen.CreateEditRoutine.createRoute(routineId))
                    },
                    onViewAllRoutinesClick = {
                        navController.navigate(Screen.Routines.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Routines.route) {
                RoutinesScreen(
                    viewModel = routinesViewModel,
                    onCreateRoutineClick = {
                        navController.navigate(Screen.CreateEditRoutine.createRoute())
                    },
                    onEditRoutineClick = { routineId ->
                        navController.navigate(Screen.CreateEditRoutine.createRoute(routineId))
                    }
                )
            }

            composable(
                route = Screen.CreateEditRoutine.route,
                arguments = listOf(
                    navArgument("routineId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val routineId = backStackEntry.arguments?.getLong("routineId")?.takeIf { it > 0 }
                val createEditViewModel: CreateEditRoutineViewModel = viewModel(
                    viewModelStoreOwner = backStackEntry,
                    key = backStackEntry.id
                ) {
                    CreateEditRoutineViewModel(
                        repository = app.repository,
                        scheduler = app.scheduler,
                        context = app.applicationContext
                    )
                }

                CreateEditRoutineScreen(
                    routineId = routineId,
                    viewModel = createEditViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
