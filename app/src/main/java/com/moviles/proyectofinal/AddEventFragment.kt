// AddEventFragment.kt
package com.moviles.proyectofinal

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import java.util.*
// Importar Corrutinas
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AddEventFragment : Fragment() {

    private val coroutineScope = MainScope()
    private lateinit var repository: EventoRepository
    private lateinit var editContacto: EditText
    private lateinit var editUbicacion: EditText
    
    // Launcher para seleccionar ubicación desde Google Maps
    private val pickLocationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Extraer ubicación del URI (formato: geo:lat,lon?q=nombre)
                val uriString = uri.toString()
                val location = extractLocationFromUri(uriString)
                editUbicacion.setText(location)
            }
        }
    }
    
    // Launcher para seleccionar contacto
    private val pickContactLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { contactUri ->
                handleContactSelection(contactUri)
            }
        }
    }
    
    // Launcher para solicitar permiso de lectura de contactos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openContactPicker()
        } else {
            Toast.makeText(requireContext(), "Permiso de contactos denegado", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Launcher para solicitar permiso de notificaciones (Android 13+)
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(requireContext(), "Las notificaciones están deshabilitadas", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_event, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = EventoRepository(requireContext()) // Inicializar repositorio
        
        // Solicitar permisos necesarios
        requestNotificationPermission()
        requestExactAlarmPermission()

        val spinnerStatus = view.findViewById<Spinner>(R.id.spinnerStatus)
        val spinnerRecordatorio = view.findViewById<Spinner>(R.id.spinnerRecordatorio)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutTipo)
        val editFecha = view.findViewById<EditText>(R.id.editFecha)
        val editHora = view.findViewById<EditText>(R.id.editHora)
        val editDescripcion = view.findViewById<EditText>(R.id.editDescripcion)
        editUbicacion = view.findViewById<EditText>(R.id.editUbicacion)
        editContacto = view.findViewById<EditText>(R.id.editContacto)
        val btnGuardar = view.findViewById<Button>(R.id.btnGuardar)
        
        // Click en el campo de ubicación abre selector de ubicación
        editUbicacion.setOnClickListener {
            openLocationPicker()
        }
        
        // Click en el campo de contacto abre el selector de contactos
        editContacto.setOnClickListener {
            checkContactsPermissionAndPick()
        }

        // [Tu código existente para inicializar Spinners y TabLayout]
        // ...
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.status_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerStatus.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.recordatorio_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerRecordatorio.adapter = adapter
        }

        listOf("Cita", "Junta", "Entrega de Proyecto", "Examen", "Otro").forEach {
            tabLayout.addTab(tabLayout.newTab().setText(it))
        }
        // ...

        val calendar = Calendar.getInstance()

        editFecha.setOnClickListener {
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    // Usamos formato YYYY-MM-DD para fácil consulta/ordenamiento
                    val fechaStr = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    editFecha.setText(fechaStr)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        editHora.setOnClickListener {
            val timePicker = TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    val horaStr = String.format("%02d:%02d", hourOfDay, minute)
                    editHora.setText(horaStr)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )
            timePicker.show()
        }

        // === Lógica de Guardado ===
        btnGuardar.setOnClickListener {
            // 1. Obtener la categoría seleccionada del TabLayout
            val selectedTab = tabLayout.getTabAt(tabLayout.selectedTabPosition)
            val categoria = selectedTab?.text?.toString() ?: "Otro"

            // 2. Crear el objeto Evento
            val nuevoEvento = Evento(
                fecha = editFecha.text.toString(),
                hora = editHora.text.toString(),
                categoria = categoria,
                descripcion = editDescripcion.text.toString(),
                status = spinnerStatus.selectedItem.toString(),
                ubicacion = editUbicacion.text.toString(),
                contacto = editContacto.text.toString()
            )

            // 3. Obtener la opción de recordatorio seleccionada
            val recordatorio = spinnerRecordatorio.selectedItem.toString()

            // 4. Guardar en el repositorio usando corrutinas
            coroutineScope.launch {
                repository.saveEvento(nuevoEvento, recordatorio)

                // Mostrar mensaje de éxito en el hilo principal (UI)
                with(requireContext()) {
                    Toast.makeText(this, "Evento guardado: ${nuevoEvento.categoria}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
    }
    
    // =========================================================================
    // Función para solicitar permiso de notificaciones
    // =========================================================================
    
    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permiso ya concedido
                }
                else -> {
                    // Solicitar permiso
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
    
    // =========================================================================
    // Función para abrir selector de ubicación con Google Maps
    // =========================================================================
    
    private fun openLocationPicker() {
        // Crear un diálogo con opciones
        val opciones = arrayOf("Buscar en Google Maps", "Usar ubicación actual", "Escribir manualmente")
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar Ubicación")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> openGoogleMapsForSearch()
                    1 -> useCurrentLocation()
                    2 -> enableManualInput()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun openGoogleMapsForSearch() {
        try {
            // Usar Intent con ACTION_PICK para seleccionar ubicación
            // Nota: Google Maps no soporta directamente ACTION_PICK para lugares
            // Por lo tanto, abriremos Maps y el usuario deberá copiar la dirección
            val uri = Uri.parse("geo:0,0?q=")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                Toast.makeText(
                    requireContext(), 
                    "Busca el lugar, mantén presionado y copia la dirección", 
                    Toast.LENGTH_LONG
                ).show()
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Google Maps no está instalado", Toast.LENGTH_SHORT).show()
                enableManualInput()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al abrir Maps", Toast.LENGTH_SHORT).show()
            enableManualInput()
        }
    }
    
    private fun useCurrentLocation() {
        // Esta funcionalidad requeriría permisos de ubicación y GPS
        // Por ahora, mostrar mensaje y permitir entrada manual
        Toast.makeText(
            requireContext(),
            "Función de ubicación actual no disponible. Usa búsqueda o escribe manualmente.",
            Toast.LENGTH_LONG
        ).show()
        enableManualInput()
    }
    
    private fun enableManualInput() {
        editUbicacion.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            isCursorVisible = true
            requestFocus()
            // Mostrar teclado
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(this, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }
    
    private fun extractLocationFromUri(uriString: String): String {
        // Intentar extraer información de ubicación del URI
        return try {
            if (uriString.contains("q=")) {
                val query = uriString.substringAfter("q=").substringBefore("&")
                Uri.decode(query)
            } else {
                "Ubicación desde Maps"
            }
        } catch (e: Exception) {
            "Ubicación"
        }
    }
    
    // =========================================================================
    // Funciones para el selector de contactos
    // =========================================================================
    
    private fun checkContactsPermissionAndPick() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso ya concedido
                openContactPicker()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) -> {
                // Mostrar explicación al usuario
                Toast.makeText(
                    requireContext(),
                    "Se necesita acceso a contactos para seleccionar un contacto",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
            else -> {
                // Solicitar permiso directamente
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }
    
    private fun openContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        pickContactLauncher.launch(intent)
    }
    
    private fun handleContactSelection(contactUri: Uri) {
        val cursor: Cursor? = requireContext().contentResolver.query(
            contactUri,
            null,
            null,
            null,
            null
        )
        
        cursor?.use {
            if (it.moveToFirst()) {
                // Obtener el nombre del contacto
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val name = if (nameIndex >= 0) it.getString(nameIndex) else "Sin nombre"
                
                // Obtener el ID del contacto
                val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                val contactId = if (idIndex >= 0) it.getString(idIndex) else null
                
                // Intentar obtener el número de teléfono
                var phoneNumber = ""
                if (contactId != null) {
                    val hasPhoneIndex = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                    val hasPhone = if (hasPhoneIndex >= 0) it.getInt(hasPhoneIndex) else 0
                    
                    if (hasPhone > 0) {
                        val phoneCursor = requireContext().contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(contactId),
                            null
                        )
                        
                        phoneCursor?.use { pc ->
                            if (pc.moveToFirst()) {
                                val phoneIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                phoneNumber = if (phoneIndex >= 0) pc.getString(phoneIndex) else ""
                            }
                        }
                    }
                }
                
                // Establecer el texto en el campo de contacto
                val contactText = if (phoneNumber.isNotEmpty()) {
                    "$name - $phoneNumber"
                } else {
                    name
                }
                editContacto.setText(contactText)
            }
        }
    }
    
    // =========================================================================
    // Función para solicitar permiso de alarmas exactas (Android 12+)
    // =========================================================================
    
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = requireContext().getSystemService(android.app.AlarmManager::class.java)
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                // Informar al usuario y llevarlo a la configuración
                Toast.makeText(
                    requireContext(),
                    "Para recibir notificaciones, permite las alarmas exactas",
                    Toast.LENGTH_LONG
                ).show()
                
                try {
                    // Abrir configuración de alarmas exactas
                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}