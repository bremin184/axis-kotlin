package com.axis.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.axis.app.ui.MainViewModel
import com.axis.app.ui.components.*
import com.axis.app.ui.screens.*
import com.axis.app.ui.theme.*

/**
 * Bottom navigation destinations.
 */
enum class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Dashboard("dashboard", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    Transactions("transactions", "History", Icons.Filled.Receipt, Icons.Outlined.Receipt),
    Analytics("analytics", "Analytics", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    Budget("budget", "Budget", Icons.Filled.PieChart, Icons.Outlined.PieChart),
    Goals("goals", "Goals", Icons.Filled.Savings, Icons.Outlined.Savings),
    Settings("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
    FrequentTransactions("frequent_transactions", "Frequent Spenders", Icons.Filled.Repeat, Icons.Outlined.Repeat),
    ProfileDetails("profile_details", "Profile", Icons.Filled.Person, Icons.Outlined.Person),
    Connections("connections", "Connections", Icons.Filled.Link, Icons.Outlined.Link),
    Send("send", "Send Money", Icons.Filled.Send, Icons.Outlined.Send),
    Receive("receive", "Receive Money", Icons.Filled.QrCode, Icons.Outlined.QrCode)
}

@Composable
fun AxisNavHost(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onImportClick: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = viewModel, 
                onNavigate = { navController.navigate(it) },
                onImportClick = onImportClick
            )
        }
        composable(Screen.Transactions.route) {
            TransactionsScreen(viewModel = viewModel)
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen(viewModel = viewModel)
        }
        composable(Screen.Budget.route) {
            BudgetScreen(viewModel = viewModel)
        }
        composable(Screen.Goals.route) {
            GoalsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigate = { navController.navigate(it) }
            )
        }
        composable(Screen.FrequentTransactions.route) {
            FrequentTransactionsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.ProfileDetails.route) {
            ProfileDetailsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Connections.route) {
            ConnectionsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Send.route) {
            SendScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Receive.route) {
            ReceiveScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Neumorphic bottom bar with center FAB cutout.
 */
@Composable
fun AxisBottomBar(
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val morphism = LocalMorphismConfig.current

    val screens = listOf(
        Screen.Dashboard, 
        Screen.Transactions, 
        Screen.Analytics, 
        Screen.Budget, 
        Screen.Goals,
        Screen.Settings
    )

    NeuBottomBar {
        screens.forEach { screen ->
            val selected = currentRoute == screen.route
            NeuBottomBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(if (selected) screen.selectedIcon else screen.unselectedIcon, contentDescription = screen.label) },
                label = {
                    Text(
                        screen.route.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        ),
                        color = if (selected) morphism.primaryColor else morphism.textMuted
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
