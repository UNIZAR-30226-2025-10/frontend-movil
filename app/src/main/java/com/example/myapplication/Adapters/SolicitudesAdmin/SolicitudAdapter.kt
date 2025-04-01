package com.example.myapplication.Adapters.SolicitudesAdmin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.io.response.Album
import com.example.myapplication.io.response.PendienteItem

class SolicitudAdapter(private var solicitudes: List<PendienteItem>) : RecyclerView.Adapter<SolicitudAdapter.ViewHolder>() {

    fun updateDataSolicitudes(searchResponse: List<PendienteItem>) {
        solicitudes = searchResponse  // Actualiza directamente listaCanciones
        notifyDataSetChanged()  // Notifica al adaptador que se actualizó la lista
    }
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val btnRechazar: Button = itemView.findViewById(R.id.btnRechazar)
        val btnAceptar: Button = itemView.findViewById(R.id.btnAceptar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_solicitud, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val solicitud = solicitudes[position]
        holder.tvNombre.text = solicitud.nombreArtistico
        holder.btnAceptar.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Aceptado: ${solicitud.nombreArtistico}", Toast.LENGTH_SHORT).show()
        }
        holder.btnRechazar.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Rechazado: ${solicitud.nombreArtistico}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = solicitudes.size
}
