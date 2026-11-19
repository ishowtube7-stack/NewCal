package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.NutritionAiRepository
import com.example.data.NutritionRepository
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.authStateFlow
import com.example.firebase.signOut
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.state.DashboardViewModel
import com.example.ui.state.MealScanViewModel
import com.example.ui.state.ProgressGoalsViewModel
import com.example.ui.theme.CalorieCoral
import com.example.ui.theme.CalorieCoralSoft
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_TODAY = "today"
const val ROUTE_SCAN = "scan"
const val ROUTE_PROGRESS = "progress"
const val ROUTE_GOALS = "goals"

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val CalSnapDestinations = listOf(
    NavDestinationItem(ROUTE_TODAY, "Today", Icons.Filled.Home, Icons.Outlined.Home),
    NavDestinationItem(ROUTE_SCAN, "Scan", Icons.Filled.CameraAlt, Icons.Outlined.CameraAlt),
    NavDestinationItem(ROUTE_PROGRESS, "Progress", Icons.Filled.Insights, Icons.Outlined.Insights),
    NavDestinationItem(ROUTE_GOALS, "Goals", Icons.Filled.Tune, Icons.Outlined.Tune)
)

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val uid = user.uid
        val displayName = user.displayName ?: user.email?.substringBefore("@") ?: "CalSnap Athlete"
        val userEmail = user.email
        key(uid) {
            CalSnapAuthenticatedShell(
                currentUserId = uid,
                userDisplayName = displayName,
                userEmail = userEmail,
                onSignOut = {
                    signOut(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
        }
    }
}

@Composable
private fun CalSnapAuthenticatedShell(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String?,
    onSignOut: () -> Unit
) {
    val dashboardViewModel: DashboardViewModel = viewModel(
        key = "dashboard_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                DashboardViewModel(NutritionRepository(db), currentUserId, userDisplayName)
            }
        }
    )

    val scanViewModel: MealScanViewModel = viewModel(
        key = "scan_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                MealScanViewModel(NutritionAiRepository(), NutritionRepository(db), currentUserId)
            }
        }
    )

    val progressGoalsViewModel: ProgressGoalsViewModel = viewModel(
        key = "progress_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                ProgressGoalsViewModel(NutritionRepository(db), currentUserId, userDisplayName)
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_TODAY
    val snackbarHostState = remember { SnackbarHostState() }

    val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val selectedDateKey by dashboardViewModel.selectedDateKey.collectAsStateWithLifecycle()
    val scanState by scanViewModel.uiState.collectAsStateWithLifecycle()
    val progressGoalsState by progressGoalsViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(dashboardViewModel) {
        dashboardViewModel.feedbackMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(progressGoalsViewModel) {
        progressGoalsViewModel.feedbackMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = SlateSurface,
                        tonalElevation = 0.dp
                    ) {
                        CalSnapDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CalorieCoral,
                                    selectedTextColor = CalorieCoral,
                                    indicatorColor = CalorieCoralSoft,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = SlateSurface,
                        contentColor = TextPrimary
                    ) {
                        CalSnapDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CalorieCoral,
                                    selectedTextColor = CalorieCoral,
                                    indicatorColor = CalorieCoralSoft,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_TODAY
                    ) {
                        composable(ROUTE_TODAY) {
                            TodayScreen(
                                uiState = dashboardState,
                                onSelectDate = dashboardViewModel::selectDate,
                                onAdjustWater = dashboardViewModel::adjustWater,
                                onOpenScan = { navigateToTab(ROUTE_SCAN) },
                                onOpenGoals = { navigateToTab(ROUTE_GOALS) },
                                onUpdateMeal = dashboardViewModel::updateMeal,
                                onDeleteMeal = dashboardViewModel::deleteMeal
                            )
                        }
                        composable(ROUTE_SCAN) {
                            BackHandler { navigateToTab(ROUTE_TODAY) }
                            ScanScreen(
                                uiState = scanState,
                                selectedDateKey = selectedDateKey,
                                onAnalyzePhoto = scanViewModel::analyzePhoto,
                                onAnalyzeDescription = scanViewModel::analyzeDescription,
                                onOpenManualEntry = scanViewModel::openManualEntry,
                                onUpdateName = scanViewModel::updateDraftName,
                                onUpdateMealType = scanViewModel::updateDraftMealType,
                                onUpdateMultiplier = scanViewModel::updateServingMultiplier,
                                onUpdateBaseMacros = scanViewModel::updateDraftBaseMacros,
                                onSaveMeal = scanViewModel::saveMeal,
                                onReset = scanViewModel::reset,
                                onMealSavedNavigateToday = { navigateToTab(ROUTE_TODAY) }
                            )
                        }
                        composable(ROUTE_PROGRESS) {
                            BackHandler { navigateToTab(ROUTE_TODAY) }
                            ProgressScreen(
                                uiState = progressGoalsState,
                                onLogWeight = progressGoalsViewModel::logWeight,
                                onDeleteWeight = progressGoalsViewModel::deleteWeight
                            )
                        }
                        composable(ROUTE_GOALS) {
                            BackHandler { navigateToTab(ROUTE_TODAY) }
                            GoalsScreen(
                                uiState = progressGoalsState,
                                userEmail = userEmail,
                                onSaveProfile = progressGoalsViewModel::saveProfile,
                                onSignOut = onSignOut
                            )
                        }
                    }
                }
            }
        }
    }
}
