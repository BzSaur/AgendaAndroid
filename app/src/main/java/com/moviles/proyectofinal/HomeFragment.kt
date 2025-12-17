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

    private lateinit var repository: EventoRepository
    private lateinit var adapterHoy: EventoEntityAdapter
    private lateinit var adapterSemana: EventoEntityAdapter
    
    private lateinit var recyclerHoy: RecyclerView
    private lateinit var recyclerSemana: RecyclerView
    private lateinit var txtNoEventosHoy: TextView
    private lateinit var txtNoEventosSemana: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = EventoRepository(requireContext())
        
        // Inicializar vistas
        recyclerHoy = view.findViewById(R.id.recyclerEventosHoy)
        recyclerSemana = view.findViewById(R.id.recyclerEventosSemana)
        txtNoEventosHoy = view.findViewById(R.id.txtNoEventosHoy)
        txtNoEventosSemana = view.findViewById(R.id.txtNoEventosSemana)
        
        // Configurar RecyclerViews
        recyclerHoy.layoutManager = LinearLayoutManager(requireContext())
        recyclerSemana.layoutManager = LinearLayoutManager(requireContext())

        // Crear adapters con referencia al FragmentManager
        adapterHoy = EventoEntityAdapter(emptyList(), childFragmentManager) {
            loadEventos()
        }
        adapterSemana = EventoEntityAdapter(emptyList(), childFragmentManager) {
            loadEventos()
        }
        
        recyclerHoy.adapter = adapterHoy
        recyclerSemana.adapter = adapterSemana

        // Cargar eventos
        loadEventos()
    }

    private fun loadEventos() {
        val eventoDao = AppDatabase.getDatabase(requireContext()).eventoDao()
        
        viewLifecycleOwner.lifecycleScope.launch {
            eventoDao.getAllEventos().collectLatest { eventosEntity ->
                // Separar eventos por fecha
                val (eventosHoy, eventosSemana) = separarEventosPorFecha(eventosEntity)
                
                // Actualizar adapter de Hoy
                adapterHoy.updateData(eventosHoy)
                txtNoEventosHoy.visibility = if (eventosHoy.isEmpty()) View.VISIBLE else View.GONE
                
                // Actualizar adapter de Esta Semana
                adapterSemana.updateData(eventosSemana)
                txtNoEventosSemana.visibility = if (eventosSemana.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
    
    private fun separarEventosPorFecha(eventos: List<EventoEntity>): Pair<List<EventoEntity>, List<EventoEntity>> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        
        // Fecha de hoy
        val hoy = dateFormat.format(calendar.time)
        
        // Inicio de la semana (lunes)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val inicioSemana = calendar.time
        
        // Fin de la semana (domingo)
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val finSemana = calendar.time
        
        val eventosHoy = mutableListOf<EventoEntity>()
        val eventosSemana = mutableListOf<EventoEntity>()
        
        for (evento in eventos) {
            try {
                val fechaEvento = dateFormat.parse(evento.fecha)
                
                if (fechaEvento != null) {
                    if (evento.fecha == hoy) {
                        // Evento de hoy
                        eventosHoy.add(evento)
                    } else if (fechaEvento.after(inicioSemana) && fechaEvento.before(finSemana)) {
                        // Evento de esta semana (excluyendo hoy)
                        eventosSemana.add(evento)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return Pair(eventosHoy, eventosSemana)
    }
}