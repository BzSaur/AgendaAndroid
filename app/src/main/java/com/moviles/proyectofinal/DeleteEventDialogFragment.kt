package com.moviles.proyectofinal

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.moviles.proyectofinal.database.EventoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DeleteEventDialogFragment(
    private val eventoEntity: EventoEntity,
    private val onEventDeleted: () -> Unit
) : DialogFragment() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var repository: EventoRepository

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        repository = EventoRepository(requireContext())
        // Capturamos un contexto seguro (applicationContext) y una referencia a la Activity
        val appCtx = requireContext().applicationContext
        val parentActivity = activity

        return AlertDialog.Builder(requireContext())
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar este evento?\n\nFecha: ${eventoEntity.fecha}\nCategoría: ${eventoEntity.categoria}")
            .setPositiveButton("Eliminar") { _, _ ->
                // Usamos GlobalScope para asegurar que la operación termine
                // aunque el diálogo se cierre inmediatamente.
                @Suppress("OPT_IN_USAGE")
                GlobalScope.launch(Dispatchers.Main) {
                    repository.deleteEvento(eventoEntity)
                    // Usar applicationContext para el Toast evita llamar requireContext() si el fragmento
                    // ya no está adjunto. Invocamos el callback sólo si la Activity aún existe.
                    Toast.makeText(appCtx, "Evento eliminado", Toast.LENGTH_SHORT).show()
                    parentActivity?.let { onEventDeleted() }
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
    }
}
