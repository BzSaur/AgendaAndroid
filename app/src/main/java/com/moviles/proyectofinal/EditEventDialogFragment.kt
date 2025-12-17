package com.moviles.proyectofinal

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.tabs.TabLayout
import com.moviles.proyectofinal.database.EventoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.*

class EditEventDialogFragment(
    private val eventoEntity: EventoEntity,
    private val onEventUpdated: () -> Unit
) : DialogFragment() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var repository: EventoRepository

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        repository = EventoRepository(requireContext())

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_evento, null)

        // Referencias a las vistas
        val editEditFecha = view.findViewById<EditText>(R.id.editEditFecha)
        val editEditHora = view.findViewById<EditText>(R.id.editEditHora)
        val tabLayoutEditCategoria = view.findViewById<TabLayout>(R.id.tabLayoutEditCategoria)
        val editEditDescripcion = view.findViewById<EditText>(R.id.editEditDescripcion)
        val spinnerEditStatus = view.findViewById<Spinner>(R.id.spinnerEditStatus)
        val editEditUbicacion = view.findViewById<EditText>(R.id.editEditUbicacion)
        val editEditContacto = view.findViewById<EditText>(R.id.editEditContacto)
        val btnCancelar = view.findViewById<Button>(R.id.btnCancelar)
        val btnGuardarCambios = view.findViewById<Button>(R.id.btnGuardarCambios)

        // Prellenar datos
        editEditFecha.setText(eventoEntity.fecha)
        editEditHora.setText(eventoEntity.hora)
        editEditDescripcion.setText(eventoEntity.descripcion)
        editEditUbicacion.setText(eventoEntity.ubicacion)
        editEditContacto.setText(eventoEntity.contacto)
        
        // Seleccionar categoría actual en el TabLayout
        val categorias = listOf("Escuela", "Trabajo", "Casa", "Otro")
        val categoriaIndex = categorias.indexOf(eventoEntity.categoria)
        if (categoriaIndex >= 0) {
            tabLayoutEditCategoria.selectTab(tabLayoutEditCategoria.getTabAt(categoriaIndex))
        }

        // Configurar Spinner de Status
        val statusOptions = resources.getStringArray(R.array.status_array)
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusOptions)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEditStatus.adapter = statusAdapter

        // Seleccionar el status actual
        val currentStatusPosition = statusOptions.indexOf(eventoEntity.status)
        if (currentStatusPosition >= 0) {
            spinnerEditStatus.setSelection(currentStatusPosition)
        }
        
        // DatePicker para Fecha
        editEditFecha.setOnClickListener {
            val calendar = Calendar.getInstance()
            try {
                val parts = editEditFecha.text.toString().split("-")
                if (parts.size == 3) {
                    calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    val fecha = String.format("%04d-%02d-%02d", year, month + 1, day)
                    editEditFecha.setText(fecha)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }
        
        // TimePicker para Hora
        editEditHora.setOnClickListener {
            val calendar = Calendar.getInstance()
            try {
                val parts = editEditHora.text.toString().split(":")
                if (parts.size == 2) {
                    calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                    calendar.set(Calendar.MINUTE, parts[1].toInt())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            val timePicker = TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    val hora = String.format("%02d:%02d", hourOfDay, minute)
                    editEditHora.setText(hora)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )
            timePicker.show()
        }
        
        // Selector de ubicación
        editEditUbicacion.setOnClickListener {
            openLocationPickerDialog(editEditUbicacion)
        }

        // Botón Cancelar
        btnCancelar.setOnClickListener {
            dismiss()
        }

        // Botón Guardar
        btnGuardarCambios.setOnClickListener {
            val selectedTab = tabLayoutEditCategoria.getTabAt(tabLayoutEditCategoria.selectedTabPosition)
            val nuevaCategoria = selectedTab?.text?.toString() ?: eventoEntity.categoria
            
            val updatedEvento = eventoEntity.copy(
                fecha = editEditFecha.text.toString(),
                hora = editEditHora.text.toString(),
                categoria = nuevaCategoria,
                descripcion = editEditDescripcion.text.toString(),
                status = spinnerEditStatus.selectedItem.toString(),
                ubicacion = editEditUbicacion.text.toString(),
                contacto = editEditContacto.text.toString()
            )

            coroutineScope.launch {
                repository.updateEvento(updatedEvento)
                Toast.makeText(requireContext(), "Evento actualizado", Toast.LENGTH_SHORT).show()
                onEventUpdated()
                dismiss()
            }
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
    }
    
    private fun openLocationPickerDialog(editText: EditText) {
        val opciones = arrayOf("Buscar en Google Maps", "Escribir manualmente")
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar Ubicación")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> {
                        try {
                            val uri = android.net.Uri.parse("geo:0,0?q=")
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                            intent.setPackage("com.google.android.apps.maps")
                            
                            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                                Toast.makeText(requireContext(), "Busca el lugar, mantén presionado y copia la dirección", Toast.LENGTH_LONG).show()
                                startActivity(intent)
                            } else {
                                Toast.makeText(requireContext(), "Google Maps no está instalado", Toast.LENGTH_SHORT).show()
                                enableManualInputDialog(editText)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Error al abrir Maps", Toast.LENGTH_SHORT).show()
                            enableManualInputDialog(editText)
                        }
                    }
                    1 -> enableManualInputDialog(editText)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun enableManualInputDialog(editText: EditText) {
        editText.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            isCursorVisible = true
            requestFocus()
        }
    }
}
