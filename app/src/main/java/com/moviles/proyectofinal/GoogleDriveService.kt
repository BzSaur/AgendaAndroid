package com.moviles.proyectofinal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Servicio simplificado para respaldo/restauración con Google Drive
 * Usa el enfoque de "compartir archivo" para que el usuario lo suba manualmente
 */
class GoogleDriveService(private val context: Context) {

    // URL de la carpeta de Google Drive donde se guardarán los respaldos
    private val DRIVE_FOLDER_URL = "https://drive.google.com/drive/folders/1rLA_oUpAX1Dt7fmOLYkmTGolHxxkKMsz"

    /**
     * Crea un Intent para compartir el archivo de respaldo
     * El usuario puede elegir subirlo a Drive, enviarlo por email, etc.
     */
    fun shareBackupFile(file: File): Intent {
        // Usar FileProvider para obtener un URI seguro
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Respaldo de Agenda - ${file.name}")
            putExtra(Intent.EXTRA_TEXT, """
                Respaldo de eventos de la Agenda
                Archivo: ${file.name}
                
                Para subir a Google Drive:
                1. Selecciona "Guardar en Drive"
                2. O descarga y sube manualmente a: $DRIVE_FOLDER_URL
            """.trimIndent())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Abre el navegador en la carpeta de Google Drive
     */
    fun openDriveFolder(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(DRIVE_FOLDER_URL)
        }
    }

    /**
     * Abre el selector de archivos para que el usuario elija un archivo de respaldo
     */
    fun createFilePickerIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/plain"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
    }

    /**
     * Copia el contenido de un archivo URI (seleccionado por el usuario) al archivo de datos
     */
    suspend fun restoreFromUri(uri: Uri, destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                destinationFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("GoogleDriveService", "Error al restaurar desde URI", e)
            false
        }
    }

    fun getDriveFolderUrl(): String = DRIVE_FOLDER_URL
}
