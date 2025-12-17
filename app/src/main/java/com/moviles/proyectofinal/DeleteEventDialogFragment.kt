package com.moviles.proyectofinal

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.moviles.proyectofinal.database.EventoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

        return AlertDialog.Builder(requireContext())
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar este evento?\n\nFecha: ${eventoEntity.fecha}\nCategoría: ${eventoEntity.categoria}")
            .setPositiveButton("Eliminar") { _, _ ->
                coroutineScope.launch {
                    repository.deleteEvento(eventoEntity)
                    Toast.makeText(requireContext(), "Evento eliminado", Toast.LENGTH_SHORT).show()
                    onEventDeleted()
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
