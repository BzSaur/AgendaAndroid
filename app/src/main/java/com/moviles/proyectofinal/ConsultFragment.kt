// ConsultFragment.kt
package com.moviles.proyectofinal

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import java.util.*
// Importar Corrutinas
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ConsultFragment : Fragment() {

    // Variables de Corrutinas y Repositorio
    private val coroutineScope = MainScope()
    private lateinit var repository: EventoRepository

    // Referencias a UI
    private lateinit var tabConsulta: TabLayout
    private lateinit var tabCategoria: TabLayout
    private lateinit var tableResultados: TableLayout
    private lateinit var spinnerMes: Spinner
    private lateinit var spinnerAnio: Spinner
    private lateinit var spinnerAnioMes: Spinner

    // Layouts de filtro
    private lateinit var layoutRango: LinearLayout
    private lateinit var layoutDia: LinearLayout
    private lateinit var layoutAnio: LinearLayout
    private lateinit var layoutMes: LinearLayout

    // EditTexts de fecha
    private lateinit var editFechaInicio: EditText
    private lateinit var editFechaFinal: EditText
    private lateinit var editFechaDia: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_consult, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = EventoRepository(requireContext())

        // Inicializar referencias
        tabConsulta = view.findViewById(R.id.tabConsulta)
        tabCategoria = view.findViewById(R.id.tabCategoria)
        tableResultados = view.findViewById(R.id.tableResultados)
        spinnerMes = view.findViewById(R.id.spinnerMes)
        spinnerAnio = view.findViewById(R.id.spinnerAnio)
        spinnerAnioMes = view.findViewById(R.id.spinnerAnioMes)
        val btnConsultar = view.findViewById<Button>(R.id.btnConsultar)

        layoutRango = view.findViewById(R.id.layoutRango)
        layoutDia = view.findViewById(R.id.layoutDia)
        layoutAnio = view.findViewById(R.id.layoutAnio)
        layoutMes = view.findViewById(R.id.layoutMes)

        editFechaInicio = view.findViewById(R.id.editFechaInicio)
        editFechaFinal = view.findViewById(R.id.editFechaFinal)
        editFechaDia = view.findViewById(R.id.editFechaDia)

        // Inicialización de Spinners, TabLayouts y DatePickers
        initializeUI(view)

        // Conectar el botón de consulta
        btnConsultar.setOnClickListener {
            performQuery()
        }
    }

    private fun initializeUI(view: View) {
        // Configurar tabs de Consulta
        listOf("Rango", "Año", "Día", "Mes").forEach {
            tabConsulta.addTab(tabConsulta.newTab().setText(it))
        }

        // Configurar tabs de Categoría (Añadir "Todas")
        listOf("Todas", "Cita", "Junta", "Entrega de Proyecto", "Examen", "Otro").forEach {
            tabCategoria.addTab(tabCategoria.newTab().setText(it))
        }

        // Configurar Spinner de Mes
        val meses = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
        spinnerMes.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            meses
        )
        
        // Configurar Spinner de Año (2020-2030)
        val anios = (2020..2030).map { it.toString() }
        val anioAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            anios
        )
        spinnerAnio.adapter = anioAdapter
        spinnerAnioMes.adapter = anioAdapter
        
        // Seleccionar año actual por defecto
        val anioActual = Calendar.getInstance().get(Calendar.YEAR)
        val posicionAnioActual = anios.indexOf(anioActual.toString())
        if (posicionAnioActual >= 0) {
            spinnerAnio.setSelection(posicionAnioActual)
            spinnerAnioMes.setSelection(posicionAnioActual)
        }

        // Lógica de visibilidad de layouts (ya existente en tu código)
        tabConsulta.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                // ... (Tu lógica de visibilidad aquí) ...
                layoutRango.visibility = View.GONE
                layoutDia.visibility = View.GONE
                layoutAnio.visibility = View.GONE
                layoutMes.visibility = View.GONE

                when (tab.position) {
                    0 -> layoutRango.visibility = View.VISIBLE
                    1 -> layoutAnio.visibility = View.VISIBLE
                    2 -> layoutDia.visibility = View.VISIBLE
                    3 -> layoutMes.visibility = View.VISIBLE
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Mostrar rango por defecto y ocultar otros
        tabConsulta.getTabAt(0)?.select()
        layoutRango.visibility = View.VISIBLE

        // Configurar DatePickers
        setupDatePicker(editFechaInicio)
        setupDatePicker(editFechaFinal)
        setupDatePicker(editFechaDia)
    }

    // Función de utilidad para DatePicker
    private fun setupDatePicker(editText: EditText) {
        editText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    // Usamos formato YYYY-MM-DD
                    val fechaStr = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    editText.setText(fechaStr)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }
    }

    // =========================================================================
    // NUEVA FUNCIÓN: Ejecutar Consulta
    // =========================================================================
    private fun performQuery() {
        // 1. Recolectar parámetros
        val tipoConsulta = tabConsulta.selectedTabPosition
        val categoria = tabCategoria.getTabAt(tabCategoria.selectedTabPosition)?.text?.toString()

        val fechaInicio = if (layoutRango.visibility == View.VISIBLE) editFechaInicio.text.toString() else editFechaDia.text.toString()
        val fechaFinal = if (layoutRango.visibility == View.VISIBLE) editFechaFinal.text.toString() else null
        val anio = when {
            layoutAnio.visibility == View.VISIBLE -> spinnerAnio.selectedItem.toString()
            layoutMes.visibility == View.VISIBLE -> spinnerAnioMes.selectedItem.toString()
            else -> null
        }

        // Mes se convierte a formato "MM" (01-12)
        val mesIndex = spinnerMes.selectedItemPosition
        val mes = if (layoutMes.visibility == View.VISIBLE) String.format("%02d", mesIndex + 1) else null

        // 2. Validar
        if (tipoConsulta == 0 && (fechaInicio.isEmpty() || fechaFinal?.isEmpty() == true)) {
            Toast.makeText(requireContext(), "Por favor, selecciona un rango de fechas.", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Ejecutar la consulta en hilo secundario
        coroutineScope.launch {
            val results = repository.getFilteredEventos(
                tipoConsulta, fechaInicio.ifEmpty { null }, fechaFinal, anio, mes, categoria
            )

            // 4. Mostrar los resultados en el hilo principal
            displayResults(results)
        }
    }

    // =========================================================================
    // NUEVA FUNCIÓN: Mostrar Resultados en TableLayout
    // =========================================================================
    private fun displayResults(eventos: List<Evento>) {
        // Eliminar todas las filas excepto la primera (encabezados)
        val childCount = tableResultados.childCount
        if (childCount > 1) {
            tableResultados.removeViews(1, childCount - 1)
        }

        if (eventos.isEmpty()) {
            Toast.makeText(requireContext(), "No se encontraron eventos.", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear una nueva fila para cada evento
        eventos.forEach { evento ->
            val row = TableRow(context)
            row.layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )

            // Columna 1: Fecha
            val tvFecha = TextView(context).apply {
                text = evento.fecha
                setPadding(8, 8, 8, 8)
            }
            // Columna 2: Hora
            val tvHora = TextView(context).apply {
                text = evento.hora
                setPadding(8, 8, 8, 8)
            }
            // Columna 3: Categoría
            val tvCategoria = TextView(context).apply {
                text = evento.categoria
                setPadding(8, 8, 8, 8)
            }
            // Columna 4: Status
            val tvStatus = TextView(context).apply {
                text = evento.status
                setPadding(8, 8, 8, 8)
            }
            // Columna 5: Ubicación/Contacto (para simplicidad, combinamos)
            val tvDesc = TextView(context).apply {
                text = "${evento.ubicacion} / ${evento.contacto}"
                setPadding(8, 8, 8, 8)
                maxLines = 1 // Evitar que la tabla se desborde demasiado
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            row.addView(tvFecha)
            row.addView(tvHora)
            row.addView(tvCategoria)
            row.addView(tvStatus)
            row.addView(tvDesc)

            tableResultados.addView(row)
        }
        Toast.makeText(requireContext(), "Mostrando ${eventos.size} resultados.", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
    }
}