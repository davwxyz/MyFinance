package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Bill
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY isPaidThisMonth ASC, dueDateDay ASC")
    fun getAllBills(): Flow<List<Bill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBills(bills: List<Bill>): List<Long>

    @Update
    suspend fun updateBill(bill: Bill)

    @Delete
    suspend fun deleteBill(bill: Bill)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteBillById(id: Long)

    @Query("DELETE FROM bills")
    suspend fun clearAll()
}
