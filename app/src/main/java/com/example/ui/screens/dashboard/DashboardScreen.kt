package com.example.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.domain.analytics.FinancialInsight
import com.example.domain.analytics.InsightLevel
import com.example.domain.analytics.TimePeriod
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FinancialProgressBar
import com.example.ui.components.MetricCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SpendingTrendBarChart
import com.example.ui.components.formatAmount
import com.example.ui.components.resolveCategoryColor
import com.example.ui.components.resolveCategoryIcon
import com.example.ui.theme.FinanceAmber
import com.example.ui.theme.FinanceAmberContainer
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceGreenContainer
import com.example.ui.theme.FinanceRed
import com.example.ui.theme.FinanceRedContainer
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onQuickAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val badges by viewModel.badges.collectAsStateWithLifecycle()
    val insights by viewModel.insights.collectAsStateWithLifecycle()

    val currency = userSettings.currency

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Net Balance & Financial Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("balance_hero_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    Color(0xFF8E0000)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Net Balance",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$currency${formatAmount(dashboardState.netBalance)}",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                            }

                            // Streak Badge
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.testTag("streak_pill")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = "Streak",
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${dashboardState.streakDays}d Streak",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Quick stats row inside card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Expenses Today",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = "$currency${formatAmount(dashboardState.totalExpensesToday)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )

                            Column {
                                Text(
                                    text = "This Week",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = "$currency${formatAmount(dashboardState.totalExpensesThisWeek)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )

                            Column {
                                Text(
                                    text = "This Month",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = "$currency${formatAmount(dashboardState.totalExpensesThisMonth)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Period Selector Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimePeriod.values().forEach { period ->
                    val isSelected = selectedPeriod == period
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setPeriod(period) },
                        label = { Text(period.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("period_chip_${period.name.lowercase()}")
                    )
                }
            }
        }

        // 3. Cash Flow Tri-Cards for Selected Period
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Income",
                    amount = dashboardState.periodIncome,
                    currency = currency,
                    icon = Icons.Default.ArrowDownward,
                    iconBgColor = FinanceGreenContainer,
                    iconColor = FinanceGreen,
                    modifier = Modifier.weight(1f),
                    testTag = "metric_period_income"
                )

                MetricCard(
                    title = "Expenses",
                    amount = dashboardState.periodExpenses,
                    currency = currency,
                    icon = Icons.Default.ArrowUpward,
                    iconBgColor = FinanceRedContainer,
                    iconColor = FinanceRed,
                    modifier = Modifier.weight(1f),
                    testTag = "metric_period_expense"
                )

                MetricCard(
                    title = "Net Flow",
                    amount = dashboardState.periodNetCashFlow,
                    currency = currency,
                    icon = Icons.Default.SwapHoriz,
                    iconBgColor = if (dashboardState.periodNetCashFlow >= 0) FinanceGreenContainer else FinanceAmberContainer,
                    iconColor = if (dashboardState.periodNetCashFlow >= 0) FinanceGreen else FinanceAmber,
                    modifier = Modifier.weight(1f),
                    testTag = "metric_period_net"
                )
            }
        }

        // 4. Monthly Budget & Safe Spend Target
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("monthly_budget_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Monthly Budget Pace",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Limit: $currency${formatAmount(dashboardState.monthlyBudget)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val progressPct = (dashboardState.budgetProgress * 100).toInt()
                        val statusColor = when {
                            progressPct >= 100 -> FinanceRed
                            progressPct >= 80 -> FinanceAmber
                            else -> FinanceGreen
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = statusColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "$progressPct% Used",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val barColor = when {
                        dashboardState.budgetProgress >= 1f -> FinanceRed
                        dashboardState.budgetProgress >= 0.8f -> FinanceAmber
                        else -> MaterialTheme.colorScheme.primary
                    }

                    FinancialProgressBar(
                        progress = dashboardState.budgetProgress,
                        color = barColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Remaining: $currency${formatAmount(dashboardState.budgetRemaining)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        dashboardState.projection?.let { proj ->
                            Text(
                                text = "Safe daily: $currency${formatAmount(proj.safeDailySpendLimit)}/d",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // 5. Daily Spending Trend Chart
        item {
            SpendingTrendBarChart(
                dailyData = dashboardState.dailyTrend,
                currency = currency
            )
        }

        // 6. Category Spending Donut Chart
        item {
            CategoryDonutChart(
                categorySpending = dashboardState.categoryBreakdown,
                currency = currency
            )
        }

        // 7. Personalized Financial Insights
        if (insights.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Smart Insights",
                    actionLabel = "All Insights",
                    onActionClick = onNavigateToInsights
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    insights.take(2).forEach { insight ->
                        val (borderCol, icon, tint) = when (insight.level) {
                            InsightLevel.WARNING -> Triple(FinanceRed, Icons.Default.Warning, FinanceRed)
                            InsightLevel.POSITIVE -> Triple(FinanceGreen, Icons.Default.CheckCircle, FinanceGreen)
                            InsightLevel.NEUTRAL -> Triple(MaterialTheme.colorScheme.primary, Icons.Default.ReceiptLong, MaterialTheme.colorScheme.primary)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("insight_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(borderCol.copy(alpha = 0.4f)))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(tint.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = tint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = insight.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = insight.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (insight.actionSuggestion.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = insight.actionSuggestion,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = tint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Badges / Gamification Bar
        item {
            SectionHeader(
                title = "Badges & Milestones",
                actionLabel = "View All",
                onActionClick = onNavigateToInsights
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(badges) { badge ->
                    Card(
                        modifier = Modifier
                            .width(140.dp)
                            .testTag("badge_card_${badge.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (badge.isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (badge.isUnlocked) Color(0xFFFFD54F) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (badge.isUnlocked) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = badge.title,
                                    tint = if (badge.isUnlocked) Color(0xFFFFA000) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = badge.title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            FinancialProgressBar(
                                progress = badge.progress,
                                color = if (badge.isUnlocked) Color(0xFFFFA000) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.height(4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 9. Upcoming Bills & Scheduled Commitments
        if (dashboardState.upcomingBills.isNotEmpty()) {
            item {
                SectionHeader(title = "Upcoming Bills & Commitments")

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dashboardState.upcomingBills.forEach { bill ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_bill_${bill.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(FinanceAmber.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = FinanceAmber,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = bill.title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Due on the ${bill.dueDateDay}th (${bill.frequency})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$currency${formatAmount(bill.amount)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Icon(
                                        imageVector = Icons.Default.CheckCircleOutline,
                                        contentDescription = "Mark Paid",
                                        tint = FinanceGreen,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .clickable { viewModel.toggleBillPaid(bill) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 10. Recent Transactions
        item {
            SectionHeader(
                title = "Recent Transactions",
                actionLabel = "View All",
                onActionClick = onNavigateToTransactions
            )

            if (dashboardState.recentTransactions.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.ReceiptLong,
                    title = "No Transactions Recorded",
                    message = "Tap the quick-add button below or sample data in Settings to get started.",
                    actionButtonText = "Quick Add Transaction",
                    onActionClick = onQuickAddTransaction,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dashboardState.recentTransactions.forEach { tx ->
                        RecentTransactionRow(
                            transaction = tx,
                            currency = currency
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentTransactionRow(
    transaction: Transaction,
    currency: String,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }
    val isExpense = transaction.type == TransactionType.EXPENSE
    val isIncome = transaction.type == TransactionType.INCOME

    val amountColor = when {
        isIncome -> FinanceGreen
        isExpense -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.primary
    }

    val sign = when {
        isIncome -> "+"
        isExpense -> "-"
        else -> ""
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recent_tx_row_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(resolveCategoryColor(transaction.category).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = resolveCategoryIcon(transaction.category),
                        contentDescription = transaction.category,
                        tint = resolveCategoryColor(transaction.category),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.description.ifBlank { transaction.category },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${transaction.category} • ${dateFormat.format(Date(transaction.timestamp))} • ${transaction.account}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "$sign$currency${formatAmount(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = amountColor
            )
        }
    }
}
