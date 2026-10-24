package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType?): String {
        return type?.name ?: TransactionType.EXPENSE.name
    }

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType {
        return try {
            value?.let { TransactionType.valueOf(it) } ?: TransactionType.EXPENSE
        } catch (_: Exception) {
            TransactionType.EXPENSE
        }
    }
}
