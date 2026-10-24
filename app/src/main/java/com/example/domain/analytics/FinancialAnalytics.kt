package com.example.domain.analytics

import com.example.data.model.FinancialBadge
import com.example.data.model.Goal
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TimePeriod(val label: String) {
    TODAY("Today"),
    WEEK("This Week"),
    MONTH("This Month"),
    ALL("All Time")
}

data class CategorySpending(
    val category: String,
    val amount: Double,
    val percentage: Float, // 0.0 to 100.0
    val count: Int
)

data class DailySpending(
    val dateLabel: String,
    val dayNumber: Int,
    val amount: Double,
    val isToday: Boolean
)

data class FinancialInsight(
    val title: String,
    val message: String,
    val level: InsightLevel, // POSITIVE, WARNING, NEUTRAL
    val actionSuggestion: String = ""
)

enum class InsightLevel {
    POSITIVE,
    WARNING,
    NEUTRAL
}

object FinancialAnalytics {

    fun filterTransactionsByPeriod(
        transactions: List<Transaction>,
        period: TimePeriod
    ): List<Transaction> {
        val calendar: Calendar = Calendar.getInstance()
        val now: Long = calendar.timeInMillis

        return when (period) {
            TimePeriod.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                transactions.filter { it.timestamp >= startOfDay && it.timestamp <= now }
            }
            TimePeriod.WEEK -> {
                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfWeek = calendar.timeInMillis
                transactions.filter { it.timestamp >= startOfWeek && it.timestamp <= now }
            }
            TimePeriod.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis
                transactions.filter { it.timestamp >= startOfMonth && it.timestamp <= now }
            }
            TimePeriod.ALL -> transactions
        }
    }

    fun calculateExpensesToday(transactions: List<Transaction>): Double {
        val todayTxs = filterTransactionsByPeriod(transactions, TimePeriod.TODAY)
        val expense = todayTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val refund = todayTxs.filter { it.type == TransactionType.REFUND }.sumOf { it.amount }
        return (expense - refund).coerceAtLeast(0.0)
    }

    fun calculateExpensesThisWeek(transactions: List<Transaction>): Double {
        val weekTxs = filterTransactionsByPeriod(transactions, TimePeriod.WEEK)
        val expense = weekTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val refund = weekTxs.filter { it.type == TransactionType.REFUND }.sumOf { it.amount }
        return (expense - refund).coerceAtLeast(0.0)
    }

    fun calculateExpensesThisMonth(transactions: List<Transaction>): Double {
        val monthTxs = filterTransactionsByPeriod(transactions, TimePeriod.MONTH)
        val expense = monthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val refund = monthTxs.filter { it.type == TransactionType.REFUND }.sumOf { it.amount }
        return (expense - refund).coerceAtLeast(0.0)
    }

    fun calculateIncomeThisMonth(transactions: List<Transaction>): Double {
        val monthTxs = filterTransactionsByPeriod(transactions, TimePeriod.MONTH)
        return monthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }

    fun calculateCategoryBreakdown(transactions: List<Transaction>): List<CategorySpending> {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val refunds = transactions.filter { it.type == TransactionType.REFUND }

        val refundByCategory = refunds.groupBy { it.category }
            .mapValues { it.value.sumOf { r -> r.amount } }

        val totalExpense = expenses.sumOf { it.amount } - refunds.sumOf { it.amount }
        if (totalExpense <= 0.0) return emptyList()

        val grouped = expenses.groupBy { it.category }
        return grouped.map { (category, list) ->
            val gross = list.sumOf { it.amount }
            val ref = refundByCategory[category] ?: 0.0
            val net = (gross - ref).coerceAtLeast(0.0)
            val pct = (net / totalExpense * 100).toFloat()
            CategorySpending(
                category = category,
                amount = net,
                percentage = pct,
                count = list.size
            )
        }.filter { it.amount > 0.0 }.sortedByDescending { it.amount }
    }

    fun calculateDailySpendingTrend(transactions: List<Transaction>, daysCount: Int = 7): List<DailySpending> {
        val result = mutableListOf<DailySpending>()
        val cal: Calendar = Calendar.getInstance()
        val currentDayOfYear: Int = cal.get(Calendar.DAY_OF_YEAR)
        val dayFormat = SimpleDateFormat("EEE", Locale.US)

        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val refunds = transactions.filter { it.type == TransactionType.REFUND }

        for (i in (daysCount - 1) downTo 0) {
            val targetCal: Calendar = Calendar.getInstance()
            targetCal.add(Calendar.DAY_OF_YEAR, -i)

            val targetYear: Int = targetCal.get(Calendar.YEAR)
            val targetDayOfYear: Int = targetCal.get(Calendar.DAY_OF_YEAR)
            val dayNum: Int = targetCal.get(Calendar.DAY_OF_MONTH)
            val dayName: String = dayFormat.format(targetCal.time)

            val dailyExpense = expenses.filter { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                txCal.get(Calendar.YEAR) == targetYear && txCal.get(Calendar.DAY_OF_YEAR) == targetDayOfYear
            }.sumOf { it.amount }

            val dailyRefund = refunds.filter { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                txCal.get(Calendar.YEAR) == targetYear && txCal.get(Calendar.DAY_OF_YEAR) == targetDayOfYear
            }.sumOf { it.amount }

            val netDaily = (dailyExpense - dailyRefund).coerceAtLeast(0.0)
            val isToday = (targetDayOfYear == currentDayOfYear)
            result.add(DailySpending(dateLabel = dayName, dayNumber = dayNum, amount = netDaily, isToday = isToday))
        }

        return result
    }

    /**
     * Predictive Burn Rate & Month-End Forecasting
     */
    fun calculateMonthProjection(
        monthlyBudget: Double,
        monthExpenses: Double
    ): MonthProjection {
        val cal: Calendar = Calendar.getInstance()
        val currentDay: Int = cal.get(Calendar.DAY_OF_MONTH)
        val maxDaysInMonth: Int = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysRemaining: Int = (maxDaysInMonth - currentDay).coerceAtLeast(1)

        val avgDailySpendSoFar: Double = if (currentDay > 0) monthExpenses / currentDay else 0.0
        val projectedTotalSpend: Double = monthExpenses + (avgDailySpendSoFar * daysRemaining)
        val remainingBudget: Double = (monthlyBudget - monthExpenses).coerceAtLeast(0.0)
        val safeDailySpendRemaining: Double = remainingBudget / daysRemaining
        val isProjectedToExceed: Boolean = projectedTotalSpend > monthlyBudget

        return MonthProjection(
            currentDay = currentDay,
            totalDaysInMonth = maxDaysInMonth,
            daysRemaining = daysRemaining,
            avgDailySpend = avgDailySpendSoFar,
            projectedMonthEndSpend = projectedTotalSpend,
            remainingBudget = remainingBudget,
            safeDailySpendLimit = safeDailySpendRemaining,
            isProjectedToExceed = isProjectedToExceed
        )
    }

    /**
     * Calculate Spending Streak (consecutive days within daily budget or logging days)
     */
    fun calculateStreak(transactions: List<Transaction>): Int {
        if (transactions.isEmpty()) return 0
        val sortedTxs = transactions.sortedByDescending { it.timestamp }
        val daysWithLogs = sortedTxs.map { tx ->
            val c: Calendar = Calendar.getInstance()
            c.timeInMillis = tx.timestamp
            "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
        }.distinct()

        var streak = 0
        val checkCal: Calendar = Calendar.getInstance()

        for (i in 0..60) {
            val key = "${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.DAY_OF_YEAR)}"
            if (daysWithLogs.contains(key)) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                if (i == 0) {
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                    val yesterdayKey = "${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.DAY_OF_YEAR)}"
                    if (daysWithLogs.contains(yesterdayKey)) {
                        streak++
                        checkCal.add(Calendar.DAY_OF_YEAR, -1)
                        continue
                    }
                }
                break
            }
        }
        return streak
    }

    /**
     * Compute Badges based on genuine user achievements
     */
    fun calculateBadges(
        transactions: List<Transaction>,
        goals: List<Goal>,
        streak: Int,
        budget: Double
    ): List<FinancialBadge> {
        val expenseTotal = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val incomeTotal = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val completedGoalsCount = goals.count { it.isCompleted }

        return listOf(
            FinancialBadge(
                id = "first_step",
                title = "First Step",
                description = "Record your very first financial transaction",
                iconName = "flag",
                isUnlocked = transactions.isNotEmpty(),
                progress = if (transactions.isNotEmpty()) 1.0f else 0.0f
            ),
            FinancialBadge(
                id = "streak_3",
                title = "Consistency Habit",
                description = "Maintain a 3-day active financial streak",
                iconName = "local_fire_department",
                isUnlocked = streak >= 3,
                progress = (streak / 3.0f).coerceIn(0f, 1f)
            ),
            FinancialBadge(
                id = "streak_7",
                title = "Weekly Master",
                description = "Maintain a 7-day continuous streak",
                iconName = "military_tech",
                isUnlocked = streak >= 7,
                progress = (streak / 7.0f).coerceIn(0f, 1f)
            ),
            FinancialBadge(
                id = "goal_getter",
                title = "Goal Crusher",
                description = "Reach 100% of any savings target",
                iconName = "emoji_events",
                isUnlocked = completedGoalsCount >= 1,
                progress = if (completedGoalsCount >= 1) 1.0f else (goals.maxOfOrNull { it.progressPercentage } ?: 0f)
            ),
            FinancialBadge(
                id = "saver_shield",
                title = "Positive Cashflow",
                description = "Earn more income than total expenses in records",
                iconName = "verified_user",
                isUnlocked = incomeTotal > expenseTotal && transactions.size >= 3,
                progress = if (incomeTotal > 0) ((incomeTotal - expenseTotal) / incomeTotal).toFloat().coerceIn(0f, 1f) else 0f
            ),
            FinancialBadge(
                id = "century_club",
                title = "Financial Archivist",
                description = "Log 25 or more transactions in your ledger",
                iconName = "auto_stories",
                isUnlocked = transactions.size >= 25,
                progress = (transactions.size / 25.0f).coerceIn(0f, 1f)
            )
        )
    }

    /**
     * Generate dynamic personalized financial insights
     */
    fun generateInsights(
        transactions: List<Transaction>,
        monthlyBudget: Double,
        goals: List<Goal>
    ): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()
        val monthExpenses = calculateExpensesThisMonth(transactions)
        val monthIncome = calculateIncomeThisMonth(transactions)

        // 1. Budget pace insight
        val projection = calculateMonthProjection(monthlyBudget, monthExpenses)
        if (monthlyBudget > 0) {
            val budgetUsagePct = (monthExpenses / monthlyBudget * 100).toInt()
            if (projection.isProjectedToExceed) {
                insights.add(
                    FinancialInsight(
                        title = "Budget Overrun Warning",
                        message = "At your current pace of $${String.format(Locale.US, "%.1f", projection.avgDailySpend)}/day, you're projected to spend $${String.format(Locale.US, "%.0f", projection.projectedMonthEndSpend)} this month (limit: $${String.format(Locale.US, "%.0f", monthlyBudget)}).",
                        level = InsightLevel.WARNING,
                        actionSuggestion = "Try capping daily discretionary spending to $${String.format(Locale.US, "%.1f", projection.safeDailySpendLimit)}."
                    )
                )
            } else {
                insights.add(
                    FinancialInsight(
                        title = "Budget Under Control",
                        message = "You have used $budgetUsagePct% of your monthly budget. Your safe daily allowance is $${String.format(Locale.US, "%.1f", projection.safeDailySpendLimit)} for the remaining ${projection.daysRemaining} days.",
                        level = InsightLevel.POSITIVE
                    )
                )
            }
        }

        // 2. Savings rate
        if (monthIncome > 0) {
            val savingsRate = ((monthIncome - monthExpenses) / monthIncome * 100).toInt()
            if (savingsRate >= 20) {
                insights.add(
                    FinancialInsight(
                        title = "Strong Savings Rate ($savingsRate%)",
                        message = "You're saving $savingsRate% of your earnings this month, surpassing the recommended 20% benchmark!",
                        level = InsightLevel.POSITIVE
                    )
                )
            } else if (savingsRate in 1..19) {
                insights.add(
                    FinancialInsight(
                        title = "Moderate Savings Rate ($savingsRate%)",
                        message = "You're retaining $savingsRate% of your income. Trimming non-essential dining or subscriptions could help reach 20%.",
                        level = InsightLevel.NEUTRAL
                    )
                )
            } else if (monthExpenses > monthIncome) {
                insights.add(
                    FinancialInsight(
                        title = "Negative Cash Flow",
                        message = "Expenses exceed income by $${String.format(Locale.US, "%.2f", monthExpenses - monthIncome)} this month. Review your top expense categories.",
                        level = InsightLevel.WARNING
                    )
                )
            }
        }

        // 3. Category concentration
        val categoryBreakdown = calculateCategoryBreakdown(filterTransactionsByPeriod(transactions, TimePeriod.MONTH))
        if (categoryBreakdown.isNotEmpty()) {
            val topCategory = categoryBreakdown.first()
            if (topCategory.percentage > 35) {
                insights.add(
                    FinancialInsight(
                        title = "Top Outflow: ${topCategory.category}",
                        message = "${topCategory.category} accounts for ${topCategory.percentage.toInt()}% ($${String.format(Locale.US, "%.2f", topCategory.amount)}) of all spending this month.",
                        level = InsightLevel.NEUTRAL
                    )
                )
            }
        }

        // 4. Goals highlight
        val inProgressGoals = goals.filter { !it.isCompleted }
        if (inProgressGoals.isNotEmpty()) {
            val closestGoal = inProgressGoals.maxByOrNull { it.progressPercentage }
            if (closestGoal != null && closestGoal.progressPercentage >= 0.5f) {
                insights.add(
                    FinancialInsight(
                        title = "Goal Target in Sight: ${closestGoal.title}",
                        message = "You are at ${(closestGoal.progressPercentage * 100).toInt()}% of your goal! Only $${String.format(Locale.US, "%.2f", closestGoal.remainingAmount)} left to reach it.",
                        level = InsightLevel.POSITIVE
                    )
                )
            }
        }

        return insights
    }
}

data class MonthProjection(
    val currentDay: Int,
    val totalDaysInMonth: Int,
    val daysRemaining: Int,
    val avgDailySpend: Double,
    val projectedMonthEndSpend: Double,
    val remainingBudget: Double,
    val safeDailySpendLimit: Double,
    val isProjectedToExceed: Boolean
)
