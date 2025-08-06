
package com.example.habittracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RegistroDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(registro: Registro)

    @Query("SELECT * FROM registro ORDER BY id ASC")
    suspend fun getAll(): List<Registro>

    @Query("DELETE FROM registro")
    suspend fun clearAll()
}
