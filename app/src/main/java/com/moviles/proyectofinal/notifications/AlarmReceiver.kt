package com.moviles.proyectofinal.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.moviles.proyectofinal.MainActivity
import com.moviles.proyectofinal.R

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val CHANNEL_ID = "eventos_recordatorios"
        private const val CHANNEL_NAME = "Recordatorios de Eventos"
        private const val CHANNEL_DESCRIPTION = "Notificaciones para recordatorios de eventos"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "¡Alarma recibida!")
        
        // Obtener datos del evento
        val eventoId = intent.getIntExtra("EVENTO_ID", -1)
        val categoria = intent.getStringExtra("CATEGORIA") ?: "Evento"
        val descripcion = intent.getStringExtra("DESCRIPCION") ?: ""
        val fecha = intent.getStringExtra("FECHA") ?: ""
        val hora = intent.getStringExtra("HORA") ?: ""
        
        Log.d(TAG, "Evento ID: $eventoId")
        Log.d(TAG, "Categoría: $categoria")
        Log.d(TAG, "Descripción: $descripcion")

        // Crear y mostrar la notificación
        showNotification(context, eventoId, categoria, descripcion, fecha, hora)
    }

    private fun showNotification(
        context: Context,
        eventoId: Int,
        categoria: String,
        descripcion: String,
        fecha: String,
        hora: String
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de notificación para Android 8.0 y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Obtener el sonido de notificación predeterminado
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()
            
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                setSound(soundUri, audioAttributes)
                setBypassDnd(true) // Bypass Do Not Disturb
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Canal de notificación creado")
        }

        // Intent para abrir la aplicación cuando se toca la notificación
        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            eventoId,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construir el contenido de la notificación
        val notificationTitle = "Recordatorio: $categoria"
        val notificationText = if (descripcion.isNotEmpty()) {
            "$descripcion - $fecha a las $hora"
        } else {
            "$fecha a las $hora"
        }

        // Obtener sonido de notificación
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        // Crear la notificación
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notificationText)
            )
            .setPriority(NotificationCompat.PRIORITY_MAX) // Máxima prioridad
            .setCategory(NotificationCompat.CATEGORY_ALARM) // Categoría ALARM para máxima visibilidad
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500)) // Vibración más larga
            .setSound(soundUri) // Agregar sonido
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Usar defaults del sistema
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Mostrar en pantalla de bloqueo
            .setFullScreenIntent(pendingIntent, true) // Mostrar como actividad de pantalla completa
            .build()

        // Mostrar la notificación
        notificationManager.notify(eventoId, notification)
        Log.d(TAG, "Notificación mostrada con ID: $eventoId")
    }
}
