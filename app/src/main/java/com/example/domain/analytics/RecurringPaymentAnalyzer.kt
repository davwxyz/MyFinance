package com.example.domain.analytics

import com.example.data.model.Budget
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import java.util.Calendar
import java.util.Locale

data class DetectedRecurringPayment(
    val title: String,
    val category: String,
    val estimatedAmount: Double,
    val occurrencesCount: Int,
    val estimatedDayOfMonth: Int,
    val confidence: String // "High", "Medium"
)

data class BudgetUtilisation(
    val category: String,
    val budgetLimit: Double,
    val actualSpent: Double,
    val percentageUsed: Float,
    val isOverBudget: Boolean,
    val remaining: Double
)

data class SavingsRecommendation(
    val title: String,
    val description: String,
    val potentialMonthlySavings: Double,
    val category: String,
    val impact: String // "High Impact", "Medium Impact", "Quick Win"
)

object RecurringPaymentAnalyzer {

    /**
     * Analyze actual transactions to detect recurring monthly commitments
     * (e.g. Netflix, Rent, Broadband, Gym, Insurance, Electricity)
     */
    fun detectRecurringPayments(transactions: List<Transaction>): List<DetectedRecurringPayment> {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        if (expenses.isEmpty()) return emptyList()

        val grouped = expenses.groupBy { normalizeMerchant(it.description.ifBlank { it.category }) }
        val detected = mutableListOf<DetectedRecurringPayment>()

        for ((merchant, txs) in grouped) {
            if (txs.size >= 2) {
                val amounts = txs.map { it.amount }
                val minAmt = amounts.minOrNull() ?: 0.0
                val maxAmt = amounts.maxOrNull() ?: 0.0
                // Similar amounts within 10%
                val isConsistentAmount = (maxAmt - minAmt) <= (minAmt * 0.15) || (maxAmt - minAmt) < 5.0

                val daysOfMonth = txs.map {
                    val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    c.get(Calendar.DAY_OF_MONTH)
                }
                val avgDay = daysOfMonth.average().toInt().coerceIn(1, 31)

                if (isConsistentAmount || txs.size >= 3) {
                    val avgAmt = amounts.average()
                    detected.add(
                        DetectedRecurringPayment(
                            title = merchant.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() },
                            category = txs.first().category,
                            estimatedAmount = avgAmt,
                            occurrencesCount = txs.size,
                            estimatedDayOfMonth = avgDay,
                            confidence = if (txs.size >= 3 && isConsistentAmount) "High" else "Medium"
                        )
                    )
                }
            }
        }

        return detected.sortedByDescending { it.estimatedAmount }
    }

    /**
     * Compute budget utilisation per category based on active budgets and recorded expenses
     */
    fun computeBudgetUtilisation(
        budgets: List<Budget>,
        monthExpenses: List<Transaction>
    ): List<BudgetUtilisation> {
        val expenseTransactions = monthExpenses.filter { it.type == TransactionType.EXPENSE }
        val spendByCategory = expenseTransactions.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        return budgets.map { budget ->
            val spent = spendByCategory[budget.category] ?: 0.0
            val pct = if (budget.monthlyLimit > 0) (spent / budget.monthlyLimit).toFloat() else 0f
            val isOver = spent > budget.monthlyLimit
            val remaining = (budget.monthlyLimit - spent).coerceAtLeast(0.0)

            BudgetUtilisation(
                category = budget.category,
                budgetLimit = budget.monthlyLimit,
                actualSpent = spent,
                percentageUsed = pct,
                isOverBudget = isOver,
                remaining = remaining
            )
        }.sortedByDescending { it.percentageUsed }
    }

    /**
     * Generate actionable savings recommendations based on user transaction history
     */
    fun generateSavingsRecommendations(
        transactions: List<Transaction>,
        monthlyBudget: Double
    ): List<SavingsRecommendation> {
        val recommendations = mutableListOf<SavingsRecommendation>()
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalSpent = expenses.sumOf { it.amount }

        if (totalSpent <= 0.0) return emptyList()

        val categoryBreakdown = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }

        // 1. Food & Dining / Delivery recommendation
        val diningSpend = categoryBreakdown["Food & Dining"] ?: 0.0
        if (diningSpend > 0) {
            val diningPct = (diningSpend / totalSpent * 100).toInt()
            if (diningPct >= 20 || diningSpend > 200) {
                val potential = diningSpend * 0.25 // Trim 25% by meal prepping
                recommendations.add(
                    SavingsRecommendation(
                        title = "Optimise Food Delivery & Dining",
                        description = "Food & Dining represents $diningPct% of your expenses. Cooking at home 2 extra nights per week could retain $${String.format(Locale.US, "%.0f", potential)} monthly.",
                        potentialMonthlySavings = potential,
                        category = "Food & Dining",
                        impact = "High Impact"
                    )
                )
            }
        }

        // 2. Recurring Subscriptions Audit
        val recurring = detectRecurringPayments(transactions)
        val subscriptionTotal = recurring.filter { it.category == "Entertainment" || it.category == "Bills & Utilities" }
            .sumOf { it.estimatedAmount }
        if (subscriptionTotal > 50) {
            recommendations.add(
                SavingsRecommendation(
                    title = "Subscription & App Audit",
                    description = "You have ${recurring.size} recurring commitments totaling $${String.format(Locale.US, "%.0f", subscriptionTotal)}/mo. Auditing unused streaming services can yield immediate savings.",
                    potentialMonthlySavings = subscriptionTotal * 0.3,
                    category = "Entertainment",
                    impact = "Quick Win"
                )
            )
        }

        // 3. Shopping & Discretionary Impulses
        val shoppingSpend = categoryBreakdown["Shopping"] ?: 0.0
        if (shoppingSpend > 150) {
            val potential = shoppingSpend * 0.20
            recommendations.add(
                SavingsRecommendation(
                    title = "Implement 48-Hour Shopping Rule",
                    description = "Pausing for 48 hours before non-essential purchases could curb impulsive checkouts and save ~$${String.format(Locale.US, "%.0f", potential)} per month.",
                    potentialMonthlySavings = potential,
                    category = "Shopping",
                    impact = "Medium Impact"
                )
            )
        }

        // 4. Transport & Rideshare
        val transportSpend = categoryBreakdown["Transport"] ?: 0.0
        if (transportSpend > 100) {
            recommendations.add(
                SavingsRecommendation(
                    title = "Consolidate Transit & Rideshares",
                    description = "Combining errands and using scheduled public transit or monthly passes can trim rideshare premiums by ~15%.",
                    potentialMonthlySavings = transportSpend * 0.15,
                    category = "Transport",
                    impact = "Medium Impact"
                )
            )
        }

        return recommendations
    }

    private fun normalizeMerchant(desc: String): String {
        val lower = desc.lowercase(Locale.US)
        return when {
            lower.contains("swiggy") -> "Swiggy"
            lower.contains("zomato") -> "Zomato"
            lower.contains("netflix") -> "Netflix"
            lower.contains("spotify") -> "Spotify"
            lower.contains("uber") -> "Uber"
            lower.contains("ola") -> "Ola"
            lower.contains("amazon") -> "Amazon"
            lower.contains("blinkit") -> "Blinkit"
            lower.contains("zepto") -> "Zepto"
            lower.contains("airtel") -> "Airtel"
            lower.contains("jio") -> "Jio"
            lower.contains("rent") -> "Apartment Rent"
            lower.contains("broadband") || lower.contains("fiber") -> "Broadband Internet"
            lower.contains("gym") || lower.contains("cult") -> "Fitness Membership"
            else -> desc.take(24)
        }
    }
}
