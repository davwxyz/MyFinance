package com.example.ui.screens.insights

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.analytics.InsightLevel
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.FinancialProgressBar
import com.example.ui.components.MetricCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SpendingTrendBarChart
import com.example.ui.components.formatAmount
import com.example.ui.theme.FinanceAmber
import com.example.ui.theme.FinanceAmberContainer
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceGreenContainer
import com.example.ui.theme.FinanceRed
import com.example.ui.theme.FinanceRedContainer
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun InsightsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val badges by viewModel.badges.collectAsStateWithLifecycle()
    val insights by viewModel.insights.collectAsStateWithLifecycle()
    val budgetUtilisation by viewModel.budgetUtilisation.collectAsStateWithLifecycle()
    val recurringPayments by viewModel.recurringPayments.collectAsStateWithLifecycle()
    val savingsRecommendations by viewModel.savingsRecommendations.collectAsStateWithLifecycle()

    val currency = userSettings.currency
    val projection = dashboardState.projection

    // Calculate Financial Health Score (0 to 100)
    val healthScore = calculateHealthScore(
        budgetProgress = dashboardState.budgetProgress,
        streak = dashboardState.streakDays,
        netCashFlow = dashboardState.periodNetCashFlow
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("insights_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Financial Health Score Hero
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("health_score_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Financial Health Score",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val statusSummary = when {
                            healthScore >= 80 -> "Excellent balance! Spending is well controlled and savings are strong."
                            healthScore >= 60 -> "Good discipline. Stay mindful of discretionary daily purchases."
                            else -> "Action advised: Spending rate is currently outpacing recommended targets."
                        }

                        Text(
                            text = statusSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Circular Score Badge
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    healthScore >= 80 -> FinanceGreenContainer
                                    healthScore >= 60 -> FinanceAmberContainer
                                    else -> FinanceRedContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$healthScore",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = when {
                                    healthScore >= 80 -> FinanceGreen
                                    healthScore >= 60 -> FinanceAmber
                                    else -> FinanceRed
                                }
                            )
                            Text(
                                text = "/ 100",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Predictive Burn Rate & Forecast
        if (projection != null) {
            item {
                SectionHeader(title = "Predictive Budget Forecast")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forecast_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (projection.isProjectedToExceed) FinanceRed.copy(alpha = 0.5f) else FinanceGreen.copy(alpha = 0.5f)
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (projection.isProjectedToExceed) "Projected Overrun" else "Projected Under Budget",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (projection.isProjectedToExceed) FinanceRed else FinanceGreen
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${projection.daysRemaining} days left in month",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Current Burn Rate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$currency${formatAmount(projection.avgDailySpend)}/day",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column {
                                Text(
                                    text = "Safe Daily Target",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$currency${formatAmount(projection.safeDailySpendLimit)}/day",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column {
                                Text(
                                    text = "Projected End Spend",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$currency${formatAmount(projection.projectedMonthEndSpend)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (projection.isProjectedToExceed) FinanceRed else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Category Donut & Daily Trend
        item {
            CategoryDonutChart(
                categorySpending = dashboardState.categoryBreakdown,
                currency = currency
            )
        }

        item {
            SpendingTrendBarChart(
                dailyData = dashboardState.dailyTrend,
                currency = currency
            )
        }

        // 3B. Budget Utilisation Breakdown
        if (budgetUtilisation.isNotEmpty()) {
            item {
                SectionHeader(title = "Category Budget Utilisation")

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    budgetUtilisation.forEach { util ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("budget_util_${util.category}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (util.isOverBudget) FinanceRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = util.category,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    val pct = (util.percentageUsed * 100).toInt()
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (util.isOverBudget) FinanceRedContainer else FinanceGreenContainer
                                    ) {
                                        Text(
                                            text = "$pct% Used",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (util.isOverBudget) FinanceRed else FinanceGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                FinancialProgressBar(
                                    progress = util.percentageUsed,
                                    color = if (util.isOverBudget) FinanceRed else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.height(6.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Spent: $currency${formatAmount(util.actualSpent)} / $currency${formatAmount(util.budgetLimit)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (util.isOverBudget) "Over by $currency${formatAmount(util.actualSpent - util.budgetLimit)}" else "Left: $currency${formatAmount(util.remaining)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (util.isOverBudget) FinanceRed else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3C. Recurring Payment Analysis
        if (recurringPayments.isNotEmpty()) {
            item {
                SectionHeader(title = "Recurring Payment Analysis")

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recurringPayments.forEach { rec ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recurring_item_${rec.title}"),
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
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = rec.title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Around day ${rec.estimatedDayOfMonth} of month • ${rec.occurrencesCount} cycles detected",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = "$currency${formatAmount(rec.estimatedAmount)}/mo",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3D. Savings Recommendations
        if (savingsRecommendations.isNotEmpty()) {
            item {
                SectionHeader(title = "Tailored Savings Recommendations")

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    savingsRecommendations.forEach { rec ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("savings_rec_${rec.category}"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(FinanceGreen.copy(alpha = 0.4f)))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = rec.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = FinanceGreenContainer
                                    ) {
                                        Text(
                                            text = rec.impact,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = FinanceGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = rec.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "Potential Savings: +$currency${formatAmount(rec.potentialMonthlySavings)} / month",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Personalized Insights
        item {
            SectionHeader(title = "AI & Analytics Insights")

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                insights.forEach { insight ->
                    val (icon, tint) = when (insight.level) {
                        InsightLevel.WARNING -> Pair(Icons.Default.Warning, FinanceRed)
                        InsightLevel.POSITIVE -> Pair(Icons.Default.CheckCircle, FinanceGreen)
                        InsightLevel.NEUTRAL -> Pair(Icons.Default.Psychology, MaterialTheme.colorScheme.primary)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("full_insight_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(tint.copy(alpha = 0.3f)))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(tint.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = insight.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = insight.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (insight.actionSuggestion.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tip: ${insight.actionSuggestion}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = tint
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Badges & Gamification Trophy Case
        item {
            SectionHeader(title = "Achievements & Badges")

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                badges.forEach { badge ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("full_badge_${badge.id}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (badge.isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (badge.isUnlocked) Color(0xFFFFD54F) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (badge.isUnlocked) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = badge.title,
                                    tint = if (badge.isUnlocked) Color(0xFFFFA000) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = badge.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (badge.isUnlocked) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFFFF8E1)
                                        ) {
                                            Text(
                                                text = "UNLOCKED",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFFFFA000),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "${(badge.progress * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = badge.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                FinancialProgressBar(
                                    progress = badge.progress,
                                    color = if (badge.isUnlocked) Color(0xFFFFA000) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.height(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun calculateHealthScore(
    budgetProgress: Float,
    streak: Int,
    netCashFlow: Double
): Int {
    var score = 50

    // Budget discipline (max +30)
    if (budgetProgress <= 0.8f) {
        score += 30
    } else if (budgetProgress <= 1.0f) {
        score += 15
    } else {
        score -= 20
    }

    // Cash flow (max +15)
    if (netCashFlow > 0) {
        score += 15
    } else if (netCashFlow < 0) {
        score -= 10
    }

    // Streak bonus (max +10)
    score += (streak * 2).coerceAtMost(10)

    return score.coerceIn(10, 100)
}
