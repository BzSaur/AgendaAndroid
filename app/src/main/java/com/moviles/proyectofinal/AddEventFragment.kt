package com.moviles.proyectofinal

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*

class AddEventFragment : Fragment() {

    private val coroutineScope = MainScope()
    private lateinit var repository: EventoRepository
    private lateinit var editContacto: EditText
    private lateinit var editUbicacion: EditText
    private lateinit var mapView: MapView
    private var myLocationOverlay: MyLocationNewOverlay? = null

    // Launcher para contactos
    private val pickContactLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { contactUri -> handleContactSelection(contactUri) }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openContactPicker()
        else Toast.makeText(requireContext(), "Permiso denegado", Toast.LENGTH_SHORT).show()
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) Toast.makeText(requireContext(), "Notificaciones deshabilitadas", Toast.LENGTH_SHORT).show()
    }

    // Launcher para permisos de ubicación (multiple)
    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            setupMyLocation()
        } else {
            Toast.makeText(requireContext(), "Sin permiso de ubicación, el mapa no te seguirá.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_add_event, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // IMPORTANTE: Configuración de OSMDroid
        Configuration.getInstance().load(requireContext(), androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()))
        Configuration.getInstance().userAgentValue = requireContext().packageName

        repository = EventoRepository(requireContext())
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
        mapView = view.findViewById(R.id.mapView)

        // --- CONFIGURACIÓN DEL MAPA ---
        setupMap(view.findViewById(R.id.scrollViewAdd))
        checkLocationPermissions()

        // Ubicación: Permitir edición manual
        editUbicacion.isFocusableInTouchMode = true
        editUbicacion.isClickable = true

        editContacto.setOnClickListener { checkContactsPermissionAndPick() }

        // Configuración de Spinners y Tabs
        ArrayAdapter.createFromResource(requireContext(), R.array.status_array, android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerStatus.adapter = it
        }
        ArrayAdapter.createFromResource(requireContext(), R.array.recordatorio_array, android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerRecordatorio.adapter = it
        }
        listOf("Cita", "Junta", "Entrega de Proyecto", "Examen", "Otro").forEach {
            tabLayout.addTab(tabLayout.newTab().setText(it))
        }

        // Fechas y Horas
        val calendar = Calendar.getInstance()
        editFecha.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                editFecha.setText(String.format("%04d-%02d-%02d", y, m + 1, d))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        editHora.setOnClickListener {
            TimePickerDialog(requireContext(), { _, h, m ->
                editHora.setText(String.format("%02d:%02d", h, m))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        // Botón Guardar
        btnGuardar.setOnClickListener {
            val categoria = tabLayout.getTabAt(tabLayout.selectedTabPosition)?.text?.toString() ?: "Otro"

            if (editUbicacion.text.toString().isEmpty()) {
                Toast.makeText(requireContext(), "Por favor selecciona una ubicación en el mapa", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nuevoEvento = Evento(
                fecha = editFecha.text.toString(),
                hora = editHora.text.toString(),
                categoria = categoria,
                descripcion = editDescripcion.text.toString(),
                status = spinnerStatus.selectedItem.toString(),
                ubicacion = editUbicacion.text.toString(),
                contacto = editContacto.text.toString()
            )
            val recordatorio = spinnerRecordatorio.selectedItem.toString()

            coroutineScope.launch {
                val insertId = repository.saveEvento(nuevoEvento, recordatorio)
                if (insertId > 0) {
                    Toast.makeText(requireContext(), "Evento guardado", Toast.LENGTH_SHORT).show()

                    // Limpiar formulario
                    editFecha.setText("")
                    editHora.setText("")
                    editDescripcion.setText("")
                    editUbicacion.setText("")
                    editContacto.setText("")
                    spinnerStatus.setSelection(0)
                    spinnerRecordatorio.setSelection(0)
                    tabLayout.selectTab(tabLayout.getTabAt(0))

                    // Limpiar marcadores en el mapa y centrar
                    mapView.overlays.removeAll { it is Marker }
                    mapView.invalidate()
                    mapView.controller.setCenter(GeoPoint(19.4326, -99.1332))
                } else {
                    Toast.makeText(requireContext(), "Error al guardar evento", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupMap(scrollView: ScrollView) {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)

        // Ubicación por defecto (CDMX o centro de vista)
        val startPoint = GeoPoint(19.4326, -99.1332)
        mapView.controller.setCenter(startPoint)

        // Solución para conflicto de ScrollView y Mapa
        mapView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> scrollView.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP -> scrollView.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

        // Detectar clics en el mapa
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let { point ->
                    updateLocationFromMap(point)
                    // Si tocamos el mapa, dejamos de seguir al GPS
                    myLocationOverlay?.disableFollowLocation()
                }
                return true
            }
            override fun longPressHelper(p: GeoPoint?): Boolean = false
        }
        val overlay = MapEventsOverlay(mapEventsReceiver)
        mapView.overlays.add(overlay)
    }

    private fun updateLocationFromMap(point: GeoPoint) {
        // 1. Poner marcador
        mapView.overlays.removeAll { it is Marker } // Limpiar marcadores anteriores
        val marker = Marker(mapView)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Ubicación seleccionada"
        mapView.overlays.add(marker)
        mapView.invalidate() // Refrescar mapa

        // 2. Intentar obtener dirección (Geocoding) o usar coordenadas
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            // Nota: getFromLocation es bloqueante, idealmente en corrutina, pero para simplicidad escolar:
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                // Construir dirección legible
                val sb = StringBuilder()
                if (address.thoroughfare != null) sb.append(address.thoroughfare).append(" ")
                if (address.subThoroughfare != null) sb.append(address.subThoroughfare)

                val direccion = if (sb.isNotEmpty()) sb.toString() else "Ubicación seleccionada"
                editUbicacion.setText(direccion)
            } else {
                // Fallback a coordenadas
                val coords = String.format("%.4f, %.4f", point.latitude, point.longitude)
                editUbicacion.setText("Coord: $coords")
            }
        } catch (e: Exception) {
            // Si falla el Geocoder (común en emuladores), usar coordenadas
            val coords = String.format("%.4f, %.4f", point.latitude, point.longitude)
            editUbicacion.setText("Ubicación: $coords")
        }
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupMyLocation()
        } else {
            requestLocationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun setupMyLocation() {
        val provider = GpsMyLocationProvider(requireContext())
        provider.addLocationSource(android.location.LocationManager.NETWORK_PROVIDER)

        myLocationOverlay = MyLocationNewOverlay(provider, mapView)
        myLocationOverlay?.enableMyLocation()
        myLocationOverlay?.enableFollowLocation()
        myLocationOverlay?.runOnFirstFix {
            activity?.runOnUiThread {
                mapView.controller.animateTo(myLocationOverlay?.myLocation)
            }
        }
        mapView.overlays.add(myLocationOverlay)
    }

    // --- Funciones auxiliares existentes ---
    private fun checkContactsPermissionAndPick() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            openContactPicker()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }
    private fun openContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        pickContactLauncher.launch(intent)
    }
    private fun handleContactSelection(contactUri: Uri) {
        val cursor: Cursor? = requireContext().contentResolver.query(contactUri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIdx = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                editContacto.setText(if (nameIdx >= 0) it.getString(nameIdx) else "Contacto")
            }
        }
    }
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = requireContext().getSystemService(android.app.AlarmManager::class.java)
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                try { startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) } catch (e: Exception) {}
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        myLocationOverlay?.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        myLocationOverlay?.disableMyLocation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
    }
}