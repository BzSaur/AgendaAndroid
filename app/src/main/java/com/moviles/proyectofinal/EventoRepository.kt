// EventoRepository.kt
package com.moviles.proyectofinal

import android.content.Context
import android.net.Uri
import com.moviles.proyectofinal.database.AppDatabase
import com.moviles.proyectofinal.database.EventoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventoRepository(private val context: Context) {

    // Room Database
    private val database = AppDatabase.getDatabase(context)
    private val eventoDao = database.eventoDao()
    
    // Directorio temporal dentro del almacenamiento interno para guardar respaldos
    private val BACKUP_DIR = "backups"
    
    // Servicio de Google Drive
    private val driveService = GoogleDriveService(context)

    // =========================================================================
    // Funciones de conversión entre Evento y EventoEntity
    // =========================================================================
    
    private fun Evento.toEntity(recordatorio: String = ""): EventoEntity {
        return EventoEntity(
            fecha = this.fecha,
            hora = this.hora,
            categoria = this.categoria,
            descripcion = this.descripcion,
            status = this.status,
            ubicacion = this.ubicacion,
            contacto = this.contacto,
            recordatorio = recordatorio
        )
    }
    
    private fun EventoEntity.toEvento(): Evento {
        return Evento(
            fecha = this.fecha,
            hora = this.hora,
            categoria = this.categoria,
            descripcion = this.descripcion,
            status = this.status,
            ubicacion = this.ubicacion,
            contacto = this.contacto
        )
    }

    // =========================================================================
    // Operaciones CRUD con Room
    // =========================================================================

    /**
     * Carga todos los eventos de la base de datos. Devuelve Flow para observar cambios.
     */
    fun loadEventosFlow(): Flow<List<Evento>> {
        return eventoDao.getAllEventos().map { entities ->
            entities.map { it.toEvento() }
        }
    }
    
    /**
     * Carga todos los eventos de la base de datos. Se ejecuta en el hilo IO.
     */
    suspend fun loadEventos(): List<Evento> = withContext(Dispatchers.IO) {
        return@withContext eventoDao.getAllEventosList().map { it.toEvento() }
    }

    /**
     * Guarda un nuevo evento en la base de datos. Se ejecuta en el hilo IO.
     * @param recordatorio Opción de recordatorio seleccionada
     * @return ID del evento insertado
     */
    suspend fun saveEvento(evento: Evento, recordatorio: String = ""): Long = withContext(Dispatchers.IO) {
        return@withContext try {
            val eventoId = eventoDao.insertEvento(evento.toEntity(recordatorio))
            
            // Programar alarma si hay recordatorio
            if (recordatorio != "Sin recordatorio") {
                withContext(Dispatchers.Main) {
                    val alarmScheduler = com.moviles.proyectofinal.notifications.AlarmScheduler(context)
                    alarmScheduler.scheduleAlarm(
                        eventoId = eventoId.toInt(),
                        fecha = evento.fecha,
                        hora = evento.hora,
                        categoria = evento.categoria,
                        descripcion = evento.descripcion,
                        recordatorio = recordatorio
                    )
                }
            }
            
            eventoId
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }
    
    /**
     * Actualiza un evento existente en la base de datos.
     */
    suspend fun updateEvento(eventoEntity: EventoEntity) = withContext(Dispatchers.IO) {
        try {
            eventoDao.updateEvento(eventoEntity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Elimina un evento de la base de datos.
     */
    suspend fun deleteEvento(eventoEntity: EventoEntity) = withContext(Dispatchers.IO) {
        try {
            // Cancelar alarma si existe
            withContext(Dispatchers.Main) {
                val alarmScheduler = com.moviles.proyectofinal.notifications.AlarmScheduler(context)
                alarmScheduler.cancelAlarm(eventoEntity.id)
            }
            
            eventoDao.deleteEvento(eventoEntity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Obtiene un evento por ID
     */
    suspend fun getEventoById(id: Int): EventoEntity? = withContext(Dispatchers.IO) {
        return@withContext eventoDao.getEventoById(id)
    }

    // =========================================================================
    // Filtrado de eventos
    // =========================================================================
    suspend fun getFilteredEventos(
        tipoConsulta: Int,
        fechaInicio: String?,
        fechaFinal: String?,
        anio: String?,
        mes: String?,
        categoria: String?
    ): List<Evento> = withContext(Dispatchers.IO) {
        var eventos = loadEventos()

        if (categoria != null && categoria != "Todas") {
            eventos = eventos.filter { it.categoria == categoria }
        }

        eventos = when (tipoConsulta) {
            // 0: Rango
            0 -> {
                if (fechaInicio != null && fechaFinal != null) {
                    eventos.filter { it.fecha >= fechaInicio && it.fecha <= fechaFinal }
                } else eventos
            }
            // 1: Año
            1 -> {
                if (anio != null) {
                    eventos.filter { it.fecha.startsWith(anio) }
                } else eventos
            }
            // 2: Día (Fecha única)
            2 -> {
                if (fechaInicio != null) {
                    eventos.filter { it.fecha == fechaInicio }
                } else eventos
            }
            // 3: Mes
            3 -> {
                if (mes != null) {
                    val mesPattern = "-$mes-"
                    eventos.filter { it.fecha.contains(mesPattern) }
                } else eventos
            }
            else -> eventos // Sin filtro de tiempo
        }

        return@withContext eventos.sortedBy { it.fecha }
    }

    // =========================================================================
    // Respaldo y Restauración con Room
    // =========================================================================

    /**
     * Crea un archivo de respaldo exportando todos los eventos de la base de datos a formato texto.
     * @return Ruta completa del archivo de respaldo creado, o null si falla.
     */
    suspend fun backupData(): String? = withContext(Dispatchers.IO) {
        try {
            val eventos = eventoDao.getAllEventosList()
            if (eventos.isEmpty()) {
                return@withContext null // No hay datos que respaldar
            }

            // Crear directorio de respaldo si no existe
            val backupDir = File(context.filesDir, BACKUP_DIR)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            // Nombre de archivo con marca de tiempo para evitar sobrescribir
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFileName = "eventos_backup_$timeStamp.txt"
            val destFile = File(backupDir, backupFileName)

            // Escribir eventos al archivo
            destFile.bufferedWriter().use { writer ->
                eventos.forEach { entity ->
                    writer.write(entity.toEvento().toTxtLine())
                }
            }

            return@withContext destFile.absolutePath

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Elimina todos los eventos de la base de datos (para restauración limpia).
     * @return true si los datos fueron borrados correctamente.
     */
    suspend fun restoreData(): Boolean = withContext(Dispatchers.IO) {
        try {
            eventoDao.deleteAllEventos()
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * Obtiene la ruta del directorio de respaldo para que el usuario pueda acceder a los archivos.
     */
    fun getBackupDirectoryPath(): String {
        return File(context.filesDir, BACKUP_DIR).absolutePath
    }

    // =========================================================================
    // Respaldo a Google Drive (Simplificado)
    // =========================================================================

    /**
     * Crea un respaldo local exportando la base de datos a un archivo de texto
     * y devuelve el archivo para compartir
     */
    suspend fun createBackupFile(): File? = withContext(Dispatchers.IO) {
        try {
            val eventos = eventoDao.getAllEventosList()
            if (eventos.isEmpty()) {
                return@withContext null
            }

            val backupDir = File(context.filesDir, BACKUP_DIR)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFileName = "eventos_backup_$timeStamp.txt"
            val destFile = File(backupDir, backupFileName)

            // Escribir eventos al archivo en formato texto
            destFile.bufferedWriter().use { writer ->
                eventos.forEach { entity ->
                    writer.write(entity.toEvento().toTxtLine())
                }
            }
            
            return@withContext destFile

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Obtiene el servicio de Drive para compartir archivos
     */
    fun getDriveService(): GoogleDriveService = driveService

    /**
     * Restaura datos desde un URI (archivo seleccionado por el usuario)
     * Lee el archivo de texto y lo importa a la base de datos Room
     */
    suspend fun restoreFromBackupFile(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // Leer el archivo de backup
            val eventos = mutableListOf<EventoEntity>()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.lineSequence().forEach { line ->
                        Evento.fromTxtLine(line)?.let { evento ->
                            eventos.add(evento.toEntity())
                        }
                    }
                }
            }
            
            if (eventos.isNotEmpty()) {
                // Borrar eventos existentes e insertar los nuevos
                eventoDao.deleteAllEventos()
                eventoDao.insertMultipleEventos(eventos)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Obtiene el archivo de datos principal (ya no se usa con Room, 
     * pero se mantiene para compatibilidad)
     */
    fun getDataFile(): File {
        return File(context.filesDir, BACKUP_DIR)
    }
}