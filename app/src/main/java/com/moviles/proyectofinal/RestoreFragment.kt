// RestoreFragment.kt
package com.moviles.proyectofinal

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RestoreFragment : Fragment() {

    private val coroutineScope = MainScope()
    private lateinit var repository: EventoRepository
    private lateinit var txtResultado: TextView

    // Launcher para seleccionar archivo
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // El usuario seleccionó un archivo
                txtResultado.text = "Restaurando datos desde el archivo seleccionado..."
                
                coroutineScope.launch {
                    val success = repository.restoreFromBackupFile(uri)
                    
                    if (success) {
                        txtResultado.text = """
                            ✔️ ¡Datos restaurados exitosamente!
                            
                            Los eventos han sido importados desde el respaldo.
                            Ve a Inicio para ver tus eventos restaurados.
                        """.trimIndent()
                        
                        Toast.makeText(
                            requireContext(),
                            "Datos restaurados correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        txtResultado.text = "❌ Error al restaurar los datos. Verifica que el archivo sea válido."
                    }
                }
            }
        } else {
            txtResultado.text = "Selección de archivo cancelada."
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_restore, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = EventoRepository(requireContext())
        val btnRestaurar = view.findViewById<Button>(R.id.btnRestaurar)
        txtResultado = view.findViewById(R.id.txtResultadoRestore)

        btnRestaurar.setOnClickListener {
            txtResultado.text = "Selecciona el archivo de respaldo..."
            
            // Abrir selector de archivos
            try {
                val intent = repository.getDriveService().createFilePickerIntent()
                filePickerLauncher.launch(intent)
                
                Toast.makeText(
                    requireContext(),
                    "Selecciona el archivo de respaldo (descárgalo de Drive si es necesario)",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                txtResultado.text = "Error al abrir selector de archivos: ${e.message}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
    }
}