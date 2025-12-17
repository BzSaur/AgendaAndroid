// HomeFragment.kt
package com.moviles.proyectofinal

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.moviles.proyectofinal.database.AppDatabase
import com.moviles.proyectofinal.database.EventoEntity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var adapterHoy: EventoEntityAdapter
    private lateinit var adapterSemana: EventoEntityAdapter
    private lateinit var adapterFuturos: EventoEntityAdapter

    private lateinit var recyclerHoy: RecyclerView
    private lateinit var recyclerSemana: RecyclerView
    private lateinit var recyclerFuturos: RecyclerView

    private lateinit var txtNoEventosHoy: TextView
    private lateinit var txtNoEventosSemana: TextView
    private lateinit var txtNoEventosFuturos: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas
        recyclerHoy = view.findViewById(R.id.recyclerEventosHoy)
        recyclerSemana = view.findViewById(R.id.recyclerEventosSemana)
        recyclerFuturos = view.findViewById(R.id.recyclerEventosFuturos)

        txtNoEventosHoy = view.findViewById(R.id.txtNoEventosHoy)
        txtNoEventosSemana = view.findViewById(R.id.txtNoEventosSemana)
        txtNoEventosFuturos = view.findViewById(R.id.txtNoEventosFuturos)

        // Configurar RecyclerViews
        recyclerHoy.layoutManager = LinearLayoutManager(requireContext())
        recyclerSemana.layoutManager = LinearLayoutManager(requireContext())
        recyclerFuturos.layoutManager = LinearLayoutManager(requireContext())

        // Función auxiliar para recargar
        val onUpdate = { loadEventos() }

        // Crear adapters
        adapterHoy = EventoEntityAdapter(emptyList(), childFragmentManager, onUpdate)
        adapterSemana = EventoEntityAdapter(emptyList(), childFragmentManager, onUpdate)
        adapterFuturos = EventoEntityAdapter(emptyList(), childFragmentManager, onUpdate)

        recyclerHoy.adapter = adapterHoy
        recyclerSemana.adapter = adapterSemana
        recyclerFuturos.adapter = adapterFuturos

        // Cargar eventos
        loadEventos()
    }

    private fun loadEventos() {
        val eventoDao = AppDatabase.getDatabase(requireContext()).eventoDao()

        viewLifecycleOwner.lifecycleScope.launch {
            eventoDao.getAllEventos().collectLatest { eventosEntity ->
                // Separar eventos en 3 listas
                val (hoy, semana, futuros) = separarEventosPorFecha(eventosEntity)

                // Actualizar UI Hoy
                adapterHoy.updateData(hoy)
                txtNoEventosHoy.visibility = if (hoy.isEmpty()) View.VISIBLE else View.GONE

                // Actualizar UI Semana
                adapterSemana.updateData(semana)
                txtNoEventosSemana.visibility = if (semana.isEmpty()) View.VISIBLE else View.GONE

                // Actualizar UI Futuros
                adapterFuturos.updateData(futuros)
                txtNoEventosFuturos.visibility = if (futuros.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    // Devuelve Triple: (Hoy, Semana, Futuros)
    private fun separarEventosPorFecha(eventos: List<EventoEntity>): Triple<List<EventoEntity>, List<EventoEntity>, List<EventoEntity>> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Normalizar "hoy" para comparar solo fechas (sin horas)
        val hoyStr = dateFormat.format(calendar.time)
        val hoyDate = dateFormat.parse(hoyStr) ?: Date()

        // Calcular fin de semana (Domingo)
        calendar.time = hoyDate
        // Si hoy es domingo, el fin de esta semana es hoy. Si es lunes, faltan 6 días, etc.
        // Una lógica simple: "Esta semana" = próximos 7 días o hasta el domingo.
        // Usaremos: Semana = > Hoy Y <= Domingo de esta semana
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        // Asegurar que si hoy es Domingo, el calendar no nos de el domingo pasado
        if (calendar.time.before(hoyDate)) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }
        val finSemanaDate = calendar.time

        val listHoy = mutableListOf<EventoEntity>()
        val listSemana = mutableListOf<EventoEntity>()
        val listFuturos = mutableListOf<EventoEntity>()

        for (evento in eventos) {
            try {
                val fechaEvento = dateFormat.parse(evento.fecha) ?: continue

                if (evento.fecha == hoyStr) {
                    listHoy.add(evento)
                } else if (fechaEvento.after(hoyDate) && (fechaEvento.before(finSemanaDate) || fechaEvento == finSemanaDate)) {
                    listSemana.add(evento)
                } else if (fechaEvento.after(finSemanaDate)) {
                    listFuturos.add(evento)
                }
                // Los eventos pasados se ignoran en Home, o puedes ponerlos en otro lado.
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Triple(listHoy, listSemana, listFuturos)
    }
}