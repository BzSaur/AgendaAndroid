package com.moviles.proyectofinal.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "eventos")
data class EventoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fecha: String,
    val hora: String,
    val categoria: String,
    val descripcion: String,
    val status: String,
    val ubicacion: String,
    val contacto: String,
    val recordatorio: String = "" // Opción de recordatorio: "Sin recordatorio", "5 minutos antes", etc.
)
