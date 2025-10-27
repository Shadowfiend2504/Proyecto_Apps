package com.example.healthconnectai.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.healthconnectai.data.model.Tarea

@Dao
interface TareaDao {
    @Query("SELECT * FROM tareas ORDER BY id DESC")
    fun getAllTareas(): LiveData<List<Tarea>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tarea: Tarea)

    @Delete
    suspend fun delete(tarea: Tarea)

    @Query("DELETE FROM tareas")
    suspend fun deleteAll()
}
