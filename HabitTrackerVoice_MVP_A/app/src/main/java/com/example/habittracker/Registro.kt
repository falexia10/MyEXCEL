
package com.example.habittracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registro")
data class Registro(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: String,
    val entrante: String?,
    val principal: String?,
    val postre: String?,
    val esfuerzo: Int?,
    val horas: Double?,
    val lecturaMin: Int?
)
