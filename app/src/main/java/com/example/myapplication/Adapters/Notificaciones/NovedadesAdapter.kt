package com.example.myapplication.Adapters.Notificaciones

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.io.response.Novedad
import com.example.myapplication.io.response.Seguidor


class NovedadesAdapter(
    private val lista: MutableList<Novedad>,
    private val onAceptarClick: (Novedad) -> Unit,
    private val onCerrarClick: (Novedad) -> Unit
) : RecyclerView.Adapter<NovedadesAdapter.ViewHolder>() {

    fun agregarNovedad(novedad: Novedad) {
        lista.add(novedad)
        notifyItemInserted(lista.size - 1)
    }

    fun eliminarNovedad(novedad: Novedad) {
        val index = lista.indexOf(novedad)
        if (index != -1) {
            lista.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTitulo)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcion)
        val btnAceptar: Button = view.findViewById(R.id.btnAceptar)
        val btnCerrar: ImageButton = view.findViewById(R.id.btnCerrar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacion_novedadmusical, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notificacion = lista[position]

        if (notificacion.tipo == "cancion") {
            holder.tvTitulo.text = "Nueva canción de " + notificacion.nombreArtisticoArtista
        } else {
            holder.tvTitulo.text = "Nuevo álbum de " + notificacion.nombreArtisticoArtista
        }

        if (notificacion.featuring != null && notificacion.featuring.isNotEmpty()) {
            val artistasFeaturing = notificacion.featuring.joinToString(", ")
            holder.tvDescripcion.text = "${notificacion.nombre} ft. $artistasFeaturing"
        } else {
            holder.tvDescripcion.text = notificacion.nombre
        }

        holder.btnAceptar.setOnClickListener {
            onAceptarClick(notificacion)
        }

        holder.btnCerrar.setOnClickListener {
            onCerrarClick(notificacion)
        }

    }

    override fun getItemCount(): Int = lista.size
}
