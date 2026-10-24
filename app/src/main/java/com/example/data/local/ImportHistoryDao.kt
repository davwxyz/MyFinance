package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ImportHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportHistoryDao {

    @Query("SELECT * FROM import_history ORDER BY importedAt DESC")
    fun getAllImportHistory(): Flow<List<ImportHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportHistory(history: ImportHistory): Long

    @Query("DELETE FROM import_history WHERE id = :id")
    suspend fun deleteImportHistoryById(id: Long)

    @Delete
    suspend fun deleteImportHistory(history: ImportHistory)

    @Query("DELETE FROM import_history")
    suspend fun clearAll()
}
