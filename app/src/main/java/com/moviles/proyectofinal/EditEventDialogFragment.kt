package com.moviles.proyectofinal

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.tabs.TabLayout
import com.moviles.proyectofinal.database.EventoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.*
import java.util.regex.Pattern

class EditEventDialogFragment(
    private val eventoEntity: EventoEntity,
    private val onEventUpdated: () -> Unit
) : DialogFragment() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var repository: EventoRepository
    private lateinit var editUbicacion: EditText
    private lateinit var mapView: MapView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // OSM config
        Configuration.getInstance().load(requireContext(), androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()))

        repository = EventoRepository(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_evento, null)

        val editFecha = view.findViewById<EditText>(R.id.editEditFecha)
        val editHora = view.findViewById<EditText>(R.id.editEditHora)
        val tabCategoria = view.findViewById<TabLayout>(R.id.tabLayoutEditCategoria)
        val editDescripcion = view.findViewById<EditText>(R.id.editEditDescripcion)
        val spinnerStatus = view.findViewById<Spinner>(R.id.spinnerEditStatus)
        val spinnerRecordatorio = view.findViewById<Spinner>(R.id.spinnerEditRecordatorio)
        val editContacto = view.findViewById<EditText>(R.id.editEditContacto)
        val btnCancelar = view.findViewById<Button>(R.id.btnCancelar)
        val btnGuardar = view.findViewById<Button>(R.id.btnGuardarCambios)

        editUbicacion = view.findViewById(R.id.editEditUbicacion)
        mapView = view.findViewById(R.id.mapViewEdit)
        val scrollView = view.findViewById<ScrollView>(R.id.scrollViewEdit)

        // Prellenar
        editFecha.setText(eventoEntity.fecha)
        editHora.setText(eventoEntity.hora)
        editDescripcion.setText(eventoEntity.descripcion)
        editUbicacion.setText(eventoEntity.ubicacion)
        editContacto.setText(eventoEntity.contacto)

        // Configurar mapa
        setupMap(scrollView)
        
        // Intentar ubicar el marcador según el texto guardado
        restoreLocationOnMap(eventoEntity.ubicacion)

        val cats = listOf("Escuela", "Trabajo", "Casa", "Otro")
        val catIndex = cats.indexOf(eventoEntity.categoria)
        if (catIndex >= 0) tabCategoria.selectTab(tabCategoria.getTabAt(catIndex))

        val statusArr = resources.getStringArray(R.array.status_array)
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusArr)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = statusAdapter
        val statusPos = statusArr.indexOf(eventoEntity.status)
        if (statusPos >= 0) spinnerStatus.setSelection(statusPos)

        val recArr = resources.getStringArray(R.array.recordatorio_array)
        val recAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, recArr)
        recAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRecordatorio.adapter = recAdapter
        val recPos = recArr.indexOf(eventoEntity.recordatorio)
        if (recPos >= 0) spinnerRecordatorio.setSelection(recPos)

        editFecha.setOnClickListener {
            val c = Calendar.getInstance()
            try { val p = editFecha.text.split("-"); if(p.size==3) c.set(p[0].toInt(), p[1].toInt()-1, p[2].toInt()) } catch(_:Exception){}
            DatePickerDialog(requireContext(), { _, y, m, d -> editFecha.setText(String.format("%04d-%02d-%02d", y, m+1, d)) }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
        editHora.setOnClickListener {
            val c = Calendar.getInstance()
            try { val p = editHora.text.split(":"); if(p.size==2) {c.set(Calendar.HOUR_OF_DAY, p[0].toInt()); c.set(Calendar.MINUTE, p[1].toInt())} } catch(_:Exception){}
            TimePickerDialog(requireContext(), { _, h, m -> editHora.setText(String.format("%02d:%02d", h, m)) }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }

        btnCancelar.setOnClickListener { dismiss() }
        btnGuardar.setOnClickListener {
            val nuevaCat = tabCategoria.getTabAt(tabCategoria.selectedTabPosition)?.text?.toString() ?: eventoEntity.categoria
            val updated = eventoEntity.copy(
                fecha = editFecha.text.toString(),
                hora = editHora.text.toString(),
                categoria = nuevaCat,
                descripcion = editDescripcion.text.toString(),
                status = spinnerStatus.selectedItem.toString(),
                ubicacion = editUbicacion.text.toString(),
                contacto = editContacto.text.toString(),
                recordatorio = spinnerRecordatorio.selectedItem.toString()
            )
            coroutineScope.launch {
                repository.updateEvento(updated)
                onEventUpdated()
                dismiss()
            }
        }

        return AlertDialog.Builder(requireContext()).setView(view).create()
    }

    private fun setupMap(scrollView: ScrollView) {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(16.0)
        
        // Manejo de scroll conflictivo
        mapView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> scrollView.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP -> scrollView.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let { updateLocationFromMap(it) }
                return true
            }
            override fun longPressHelper(p: GeoPoint?): Boolean = false
        }
        mapView.overlays.add(MapEventsOverlay(mapEventsReceiver))
    }

    private fun restoreLocationOnMap(locationStr: String) {
        // 1. Intentar buscar Coordenadas en formato "Lat: 19.43, Lon: -99.13" o similar
        // Regex simple para buscar números decimales
        val p = Pattern.compile("Lat:\\s*(-?\\d+(\\.\\d+)?),\\s*Lon:\\s*(-?\\d+(\\.\\d+)?)")
        val m = p.matcher(locationStr)
        
        var point: GeoPoint? = null
        
        if (m.find()) {
            try {
                val lat = m.group(1)?.toDouble()
                val lon = m.group(3)?.toDouble()
                if (lat != null && lon != null) {
                    point = GeoPoint(lat, lon)
                }
            } catch (e: Exception) {}
        }
        
        // 2. Si no es coordenada, intentar geocoding inverso (nombre -> coordenadas)
        if (point == null && locationStr.isNotEmpty()) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(locationStr, 1)
                if (!addresses.isNullOrEmpty()) {
                    point = GeoPoint(addresses[0].latitude, addresses[0].longitude)
                }
            } catch (e: Exception) {}
        }

        // 3. Poner marcador si encontramos algo
        if (point != null) {
            mapView.controller.setCenter(point)
            val marker = Marker(mapView)
            marker.position = point
            marker.title = locationStr
            mapView.overlays.add(marker)
        } else {
            // Default CDMX
            mapView.controller.setCenter(GeoPoint(19.4326, -99.1332))
        }
    }

    private fun updateLocationFromMap(point: GeoPoint) {
        mapView.overlays.removeAll { it is Marker }
        val marker = Marker(mapView)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(marker)
        mapView.invalidate()
        
        editUbicacion.setText(String.format("Lat: %.5f, Lon: %.5f", point.latitude, point.longitude))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
    }
}