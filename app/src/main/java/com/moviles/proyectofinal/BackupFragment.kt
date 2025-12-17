// BackupFragment.kt
package com.moviles.proyectofinal

import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BackupFragment : Fragment() {

    private val coroutineScope = MainScope()
    private lateinit var repository: EventoRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_backup, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = EventoRepository(requireContext())
        val btnRespaldar = view.findViewById<Button>(R.id.btnRespaldar)
        val txtResultado = view.findViewById<TextView>(R.id.txtResultado)

        btnRespaldar.setOnClickListener {
            txtResultado.text = "Creando respaldo..."

            coroutineScope.launch {
                val backupFile = repository.createBackupFile()

                if (backupFile != null) {
                    txtResultado.text = """
                        ✔️ Respaldo creado exitosamente
                        
                        Archivo: ${backupFile.name}
                        
                        Toca el botón de abajo para:
                        • Subirlo a Google Drive
                        • Enviarlo por correo
                        • Guardarlo en otro lugar
                    """.trimIndent()

                    // Abrir selector para compartir el archivo usando FileProvider
                    try {
                        shareBackup(backupFile)
                    } catch (e: Exception) {
                        txtResultado.text = "Error al compartir: ${e.message}"
                    }
                } else {
                    txtResultado.text = "⚠️ No hay datos para respaldar o ocurrió un error."
                }
            }
        }
    }

    private fun shareBackup(file: File) {
        try {
            val authority = "${requireContext().packageName}.provider"
            val uri = FileProvider.getUriForFile(requireContext(), authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Guardar respaldo en..."))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error preparando envío: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
    }
}