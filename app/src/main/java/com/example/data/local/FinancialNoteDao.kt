package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FinancialNote
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialNoteDao {
    @Query("SELECT * FROM financial_notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<FinancialNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: FinancialNote): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<FinancialNote>): List<Long>

    @Update
    suspend fun updateNote(note: FinancialNote)

    @Delete
    suspend fun deleteNote(note: FinancialNote)

    @Query("DELETE FROM financial_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM financial_notes")
    suspend fun clearAll()
}
