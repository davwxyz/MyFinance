package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY category ASC")
    fun getAllBudgets(): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<Budget>): List<Long>

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)

    @Query("DELETE FROM budgets")
    suspend fun clearAll()
}
