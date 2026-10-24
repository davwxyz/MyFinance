package com.example.domain.parser

import com.example.data.model.TransactionType
import java.util.Locale

object TransactionCategorizer {

    data class CategorizationResult(
        val category: String,
        val type: TransactionType,
        val cleanDescription: String,
        val tags: String
    )

    fun categorize(
        rawDescription: String,
        rawType: String = "",
        rawAmount: Double = 0.0
    ): CategorizationResult {
        val descLower = rawDescription.lowercase(Locale.US)
        val typeLower = rawType.lowercase(Locale.US)

        // 1. Check for Refund
        if (descLower.contains("refund") ||
            descLower.contains("cashback") ||
            descLower.contains("reversal") ||
            descLower.contains("reversed") ||
            descLower.contains("chargeback") ||
            descLower.contains("failed refund")
        ) {
            val cat = when {
                descLower.contains("swiggy") || descLower.contains("zomato") -> "Food & Dining"
                descLower.contains("amazon") || descLower.contains("flipkart") || descLower.contains("myntra") -> "Shopping"
                descLower.contains("uber") || descLower.contains("ola") || descLower.contains("irctc") -> "Transport"
                else -> "Refunds"
            }
            return CategorizationResult(
                category = cat,
                type = TransactionType.REFUND,
                cleanDescription = cleanNarration(rawDescription),
                tags = "#refund"
            )
        }

        // 2. Check for Transfer (self transfer, wallet top-up, own account)
        if (descLower.contains("transfer to self") ||
            descLower.contains("to own account") ||
            descLower.contains("wallet topup") ||
            descLower.contains("wallet reload") ||
            descLower.contains("self transfer") ||
            descLower.contains("internal transfer")
        ) {
            return CategorizationResult(
                category = "Transfers",
                type = TransactionType.TRANSFER,
                cleanDescription = cleanNarration(rawDescription),
                tags = "#transfer"
            )
        }

        // 3. Check for Income (Salary, Interest, Dividend, Credits)
        val isCredit = typeLower.contains("cr") || typeLower.contains("credit") || typeLower.contains("deposit") || typeLower.contains("income")
        if (isCredit || descLower.contains("salary") || descLower.contains("payroll") || descLower.contains("interest credited") || descLower.contains("dividend")) {
            val cat = when {
                descLower.contains("salary") || descLower.contains("payroll") -> "Salary"
                descLower.contains("interest") || descLower.contains("dividend") -> "Investments"
                descLower.contains("freelance") || descLower.contains("consult") -> "Freelance"
                else -> "Other Income"
            }
            return CategorizationResult(
                category = cat,
                type = TransactionType.INCOME,
                cleanDescription = cleanNarration(rawDescription),
                tags = "#income"
            )
        }

        // 4. Categorize Expenses by Merchant / Keyword
        val category = when {
            // Food & Dining
            matchesAny(descLower, "swiggy", "zomato", "mcdonald", "starbucks", "domino", "pizza", "kfc", "burger", "cafe", "coffee", "restaurant", "eats", "biryani", "diner", "bakery", "subway", "chai", "tea stall") -> "Food & Dining"

            // Groceries
            matchesAny(descLower, "blinkit", "zepto", "instamart", "bigbasket", "dmart", "spencer", "nature's basket", "grocery", "supermarket", "provisions", "vegetable", "fruits", "milk", "kirana", "reliance fresh") -> "Groceries"

            // Transport & Fuel
            matchesAny(descLower, "uber", "ola", "rapido", "metro", "irctc", "railway", "petrol", "fuel", "hpcl", "bpcl", "ioc", "shell", "fastag", "toll", "parking", "flight", "indigo", "air india") -> "Transport"

            // Shopping & E-Commerce
            matchesAny(descLower, "amazon", "flipkart", "myntra", "meesho", "ajio", "nykaa", "tata cliq", "zara", "h&m", "uniqlo", "retail", "clothing", "apparel", "decathlon") -> "Shopping"

            // Bills & Utilities
            matchesAny(descLower, "bescom", "tneb", "electricity", "broadband", "airtel", "jio", "vi ", "vodafone", "tataplay", "dth", "water bill", "piped gas", "cylinder", "indane", "hp gas", "billdesk", "recharge") -> "Bills & Utilities"

            // Entertainment & Subscriptions
            matchesAny(descLower, "netflix", "spotify", "hotstar", "prime video", "disney", "youtube", "bookmyshow", "pvr", "inox", "cinema", "apple.com/bill", "google *", "playstore", "steam") -> "Entertainment"

            // Health & Medical
            matchesAny(descLower, "apollo", "pharmacy", "medplus", "1mg", "netmeds", "hospital", "clinic", "dr.", "diagnostic", "pathology", "cult.fit", "gym", "fitness", "dentist") -> "Health"

            // Housing & Rent
            matchesAny(descLower, "rent", "nobroker", "maintenance", "society", "housing", "landlord", "flat rent", "brokerage") -> "Housing"

            // Education
            matchesAny(descLower, "udemy", "coursera", "school", "college", "tuition", "classes", "books", "stationery") -> "Education"

            // Personal Care
            matchesAny(descLower, "salon", "spa", "barber", "grooming", "haircut", "parlour") -> "Personal Care"

            // Generic Transfer to another person via UPI
            descLower.contains("upi/") || descLower.contains("to vpa") || descLower.contains("sent to") || descLower.contains("imps-") || descLower.contains("neft-") -> {
                // If paid to a person, default to General or Transfers
                if (descLower.contains("transfer")) "Transfers" else "General"
            }

            else -> "General"
        }

        val transactionType = if (category == "Transfers") TransactionType.TRANSFER else TransactionType.EXPENSE

        return CategorizationResult(
            category = category,
            type = transactionType,
            cleanDescription = cleanNarration(rawDescription),
            tags = "#${category.lowercase().replace(" ", "").replace("&", "")}"
        )
    }

    private fun matchesAny(text: String, vararg keywords: String): Boolean {
        for (kw in keywords) {
            if (text.contains(kw)) return true
        }
        return false
    }

    private fun cleanNarration(raw: String): String {
        var clean = raw.trim()
        // Strip common bank prefixes like "UPI/CR/", "UPI/DR/", "POS 4321", "NEFT-", "IMPS-"
        val prefixes = listOf("UPI/CR/", "UPI/DR/", "UPI/", "POS/", "POS ", "NEFT-", "IMPS-", "RTGS-", "ACH-", "NACH-")
        for (prefix in prefixes) {
            if (clean.startsWith(prefix, ignoreCase = true)) {
                clean = clean.substring(prefix.length).trim()
            }
        }
        return clean.ifBlank { "Transaction" }
    }
}
