package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.Screen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.goals.GoalsScreen
import com.example.ui.screens.import.TransactionImportScreen
import com.example.ui.screens.insights.InsightsScreen
import com.example.ui.screens.more.MoreScreen
import com.example.ui.screens.onboarding.OnboardingDialog
import com.example.ui.screens.transactions.AddEditTransactionDialog
import com.example.ui.screens.transactions.TransactionsScreen
import com.example.ui.theme.MyFinanceTheme
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel = ViewModelProvider(
            this,
            FinanceViewModel.provideFactory(application)
        )[FinanceViewModel::class.java]

        setContent {
            val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
            val isDark = when (userSettings.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyFinanceTheme(darkTheme = isDark) {
                MyFinanceApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFinanceApp(viewModel: FinanceViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAddTransactionDialog by remember { mutableStateOf(false) }

    // Listen for snackbar events (e.g. undo delete, save alerts)
    LaunchedEffect(Unit) {
        viewModel.messageEvent.collectLatest { msg ->
            if (msg.contains("deleted", ignoreCase = true)) {
                val result = snackbarHostState.showSnackbar(
                    message = msg,
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoDeleteTransaction()
                }
            } else {
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // First time onboarding modal
    var showOnboarding by remember { mutableStateOf(false) }
    LaunchedEffect(userSettings.isOnboardingCompleted, allTransactions.size) {
        if (!userSettings.isOnboardingCompleted && allTransactions.isEmpty()) {
            showOnboarding = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Finance",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            navController.navigate(Screen.Import.route) {
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.testTag("top_bar_import_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = "Import Statement",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("app_top_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
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
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTransactionDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("quick_add_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick Add Transaction",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Transactions.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToGoals = {
                        navController.navigate(Screen.Goals.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToInsights = {
                        navController.navigate(Screen.Insights.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onQuickAddTransaction = { showAddTransactionDialog = true }
                )
            }

            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    viewModel = viewModel,
                    onAddTransaction = { showAddTransactionDialog = true },
                    onImportStatement = {
                        navController.navigate(Screen.Import.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Insights.route) {
                InsightsScreen(
                    viewModel = viewModel
                )
            }

            composable(Screen.Goals.route) {
                GoalsScreen(
                    viewModel = viewModel
                )
            }

            composable(Screen.More.route) {
                MoreScreen(
                    viewModel = viewModel,
                    onNavigateToImport = {
                        navController.navigate(Screen.Import.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Import.route) {
                TransactionImportScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // Add Transaction Dialog
        if (showAddTransactionDialog) {
            AddEditTransactionDialog(
                initialTransaction = null,
                currency = userSettings.currency,
                onDismiss = { showAddTransactionDialog = false },
                onSave = { tx ->
                    viewModel.addTransaction(tx)
                    showAddTransactionDialog = false
                }
            )
        }

        // Onboarding Dialog
        if (showOnboarding) {
            OnboardingDialog(
                currency = userSettings.currency,
                onDismiss = { showOnboarding = false },
                onComplete = { balance, income, budget ->
                    viewModel.completeOnboarding(balance, income, budget)
                    showOnboarding = false
                },
                onLoadDemoData = {
                    viewModel.seedSampleData()
                }
            )
        }
    }
}
