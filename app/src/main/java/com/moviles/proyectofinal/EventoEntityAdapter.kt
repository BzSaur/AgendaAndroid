package com.moviles.proyectofinal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.moviles.proyectofinal.database.EventoEntity

class EventoEntityAdapter(
    private var lista: List<EventoEntity>,
    private val fragmentManager: FragmentManager,
    private val onDataChanged: () -> Unit
) : RecyclerView.Adapter<EventoEntityAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtFechaHora: TextView = itemView.findViewById(R.id.txtFechaHora)
        val txtCategoria: TextView = itemView.findViewById(R.id.txtCategoria)
        val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)
        val txtUbicacion: TextView = itemView.findViewById(R.id.txtUbicacion)
        val txtContacto: TextView = itemView.findViewById(R.id.txtContacto)
        val btnEditar: Button = itemView.findViewById(R.id.btnEditar)
        val btnEliminar: Button = itemView.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_evento, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val evento = lista[position]
        holder.txtFechaHora.text = "${evento.fecha} • ${evento.hora}"
        holder.txtCategoria.text = "Categoría: ${evento.categoria}"
        holder.txtStatus.text = "Status: ${evento.status}"
        holder.txtUbicacion.text = "Ubicación: ${evento.ubicacion}"
        holder.txtContacto.text = "Contacto: ${evento.contacto}"

        // Botón Editar
        holder.btnEditar.setOnClickListener {
            val dialog = EditEventDialogFragment(evento) {
                onDataChanged()
            }
            dialog.show(fragmentManager, "EditEventDialog")
        }

        // Botón Eliminar
        holder.btnEliminar.setOnClickListener {
            val dialog = DeleteEventDialogFragment(evento) {
                onDataChanged()
            }
            dialog.show(fragmentManager, "DeleteEventDialog")
        }
    }

    fun updateData(newList: List<EventoEntity>) {
        lista = newList
        notifyDataSetChanged()
    }
}
