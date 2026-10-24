package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Transactions : Screen("transactions", "Transactions", Icons.Default.ReceiptLong)
    object Insights : Screen("insights", "Insights", Icons.Default.Insights)
    object Goals : Screen("goals", "Goals", Icons.Default.Savings)
    object More : Screen("more", "More", Icons.Default.MoreHoriz)
    object Import : Screen("import", "Import Statement", Icons.Default.AccountBalance)

    companion object {
        val bottomNavItems = listOf(
            Dashboard,
            Transactions,
            Insights,
            Goals,
            More
        )
    }
}
