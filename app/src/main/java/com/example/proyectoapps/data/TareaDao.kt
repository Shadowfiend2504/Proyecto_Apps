package com.example.proyectoapps.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TareaDao {
    @Query("SELECT * FROM tareas ORDER BY fecha DESC")
    fun getAllFlow(): Flow<List<Tarea>>

    @Query("SELECT * FROM tareas ORDER BY fecha DESC")
    suspend fun getAll(): List<Tarea>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tarea: Tarea): Long

    @Delete
    suspend fun delete(tarea: Tarea)

    @Query("DELETE FROM tareas")
    suspend fun clearAll()
}
