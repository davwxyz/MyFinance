package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getTransactionsBetween(start: Long, end: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun getTransactionById(id: Long): Flow<Transaction?>

    @Query("SELECT * FROM transactions WHERE referenceId = :refId AND referenceId != '' LIMIT 1")
    suspend fun getTransactionByReferenceId(refId: String): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE importBatchId = :batchId")
    suspend fun deleteTransactionsByBatchId(batchId: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}
