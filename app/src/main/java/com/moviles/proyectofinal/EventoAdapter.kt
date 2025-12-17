package com.moviles.proyectofinal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventoAdapter(private val lista: List<Evento>) : RecyclerView.Adapter<EventoAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtFechaHora: TextView = itemView.findViewById(R.id.txtFechaHora)
        val txtCategoria: TextView = itemView.findViewById(R.id.txtCategoria)
        val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)
        val txtUbicacion: TextView = itemView.findViewById(R.id.txtUbicacion)
        val txtContacto: TextView = itemView.findViewById(R.id.txtContacto)
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
    }
}
