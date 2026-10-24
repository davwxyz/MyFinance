package com.example.data.model

enum class TransactionType(val displayName: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer"),
    REFUND("Refund")
}
