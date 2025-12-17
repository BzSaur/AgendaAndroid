package com.moviles.proyectofinal.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoDao {
    
    // Insertar un evento
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvento(evento: EventoEntity): Long
    
    // Actualizar un evento existente
    @Update
    suspend fun updateEvento(evento: EventoEntity)
    
    // Eliminar un evento
    @Delete
    suspend fun deleteEvento(evento: EventoEntity)
    
    // Obtener todos los eventos ordenados por fecha descendente
    @Query("SELECT * FROM eventos ORDER BY fecha DESC, hora DESC")
    fun getAllEventos(): Flow<List<EventoEntity>>
    
    // Obtener eventos por rango de fechas
    @Query("SELECT * FROM eventos WHERE fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY fecha DESC, hora DESC")
    fun getEventosByFechaRango(fechaInicio: String, fechaFin: String): Flow<List<EventoEntity>>
    
    // Obtener eventos por categoría
    @Query("SELECT * FROM eventos WHERE categoria = :categoria ORDER BY fecha DESC, hora DESC")
    fun getEventosByCategoria(categoria: String): Flow<List<EventoEntity>>
    
    // Obtener eventos por mes y año
    @Query("SELECT * FROM eventos WHERE fecha LIKE :mesAnio ORDER BY fecha DESC, hora DESC")
    fun getEventosByMes(mesAnio: String): Flow<List<EventoEntity>>
    
    // Obtener eventos por año
    @Query("SELECT * FROM eventos WHERE fecha LIKE :anio ORDER BY fecha DESC, hora DESC")
    fun getEventosByAnio(anio: String): Flow<List<EventoEntity>>
    
    // Buscar evento por ID
    @Query("SELECT * FROM eventos WHERE id = :eventoId")
    suspend fun getEventoById(eventoId: Int): EventoEntity?
    
    // Obtener todos los eventos (para backup) - sin Flow, devuelve lista directa
    @Query("SELECT * FROM eventos ORDER BY fecha DESC, hora DESC")
    suspend fun getAllEventosList(): List<EventoEntity>
    
    // Insertar múltiples eventos (para restore)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMultipleEventos(eventos: List<EventoEntity>)
    
    // Eliminar todos los eventos
    @Query("DELETE FROM eventos")
    suspend fun deleteAllEventos()
}
