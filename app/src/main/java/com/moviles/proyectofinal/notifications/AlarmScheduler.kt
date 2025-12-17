package com.moviles.proyectofinal.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    companion object {
        private const val TAG = "AlarmScheduler"
    }

    /**
     * Programa una alarma para mostrar notificación antes del evento o a la hora exacta
     * @param eventoId ID del evento en la base de datos
     * @param fecha Fecha del evento en formato YYYY-MM-DD
     * @param hora Hora del evento en formato HH:mm
     * @param categoria Categoría del evento
     * @param recordatorio Opción de recordatorio: "Sin recordatorio", "A la hora del evento", "5 minutos antes", etc.
     */
    fun scheduleAlarm(
        eventoId: Int,
        fecha: String,
        hora: String,
        categoria: String,
        descripcion: String,
        recordatorio: String
    ) {
        if (recordatorio == "Sin recordatorio") {
            Log.d(TAG, "No se programa alarma para evento $eventoId (sin recordatorio)")
            return // No programar alarma
        }

        // Calcular el tiempo de la alarma
        val triggerTimeMillis = calculateTriggerTime(fecha, hora, recordatorio)
        
        if (triggerTimeMillis == null) {
            Log.e(TAG, "Error calculando tiempo de alarma para evento $eventoId")
            return
        }

        val now = System.currentTimeMillis()
        val timeUntilAlarm = triggerTimeMillis - now
        
        // Solo programar si la alarma es en el futuro (con margen de 1 minuto)
        if (timeUntilAlarm < 60000) {
            Log.w(TAG, "Alarma para evento $eventoId ya pasó o es muy pronto (${timeUntilAlarm}ms)")
            return
        }
        
        Log.d(TAG, "Programando alarma para evento $eventoId:")
        Log.d(TAG, "  - Fecha/Hora: $fecha $hora")
        Log.d(TAG, "  - Recordatorio: $recordatorio")
        Log.d(TAG, "  - Tiempo hasta alarma: ${timeUntilAlarm / 1000 / 60} minutos")
        Log.d(TAG, "  - Timestamp: $triggerTimeMillis")

        // Crear Intent para el BroadcastReceiver
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EVENTO_ID", eventoId)
            putExtra("CATEGORIA", categoria)
            putExtra("DESCRIPCION", descripcion)
            putExtra("FECHA", fecha)
            putExtra("HORA", hora)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventoId, // Usar el ID del evento como requestCode único
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Programar la alarma
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Para Android 6.0 y superior
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
                Log.d(TAG, "Alarma programada exitosamente (setExactAndAllowWhileIdle)")
            } else {
                // Para versiones anteriores
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
                Log.d(TAG, "Alarma programada exitosamente (setExact)")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Error de permisos al programar alarma: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al programar alarma: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Cancela una alarma programada
     * @param eventoId ID del evento
     */
    fun cancelAlarm(eventoId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventoId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Calcula el tiempo de disparo de la alarma en milisegundos
     */
    private fun calculateTriggerTime(fecha: String, hora: String, recordatorio: String): Long? {
        return try {
            // Parsear fecha y hora
            val dateTimeString = "$fecha $hora"
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            format.isLenient = false
            val eventDateTime = format.parse(dateTimeString)
            
            if (eventDateTime == null) {
                Log.e(TAG, "Error parseando fecha/hora: $dateTimeString")
                return null
            }
            
            val eventTimeMillis = eventDateTime.time

            // Calcular los minutos antes según la opción de recordatorio
            val minutesBefore = when (recordatorio) {
                "A la hora del evento" -> 0  // Alarma a la hora exacta
                "5 minutos antes" -> 5
                "15 minutos antes" -> 15
                "30 minutos antes" -> 30
                "1 hora antes" -> 60
                "1 día antes" -> 1440
                else -> {
                    Log.w(TAG, "Opción de recordatorio desconocida: $recordatorio")
                    0
                }
            }

            // Restar los minutos del tiempo del evento
            val triggerTime = eventTimeMillis - (minutesBefore * 60 * 1000)
            
            Log.d(TAG, "Tiempo del evento: ${Date(eventTimeMillis)}")
            Log.d(TAG, "Tiempo de alarma: ${Date(triggerTime)}")
            
            triggerTime
        } catch (e: Exception) {
            Log.e(TAG, "Excepción calculando tiempo: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
