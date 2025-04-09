package com.example.myapplication.Adapters.EstadisticasAlbum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.io.response.CancionEst

class CancionEstAdapter(private val canciones: List<CancionEst>, private val nombreArtista: String) :
    RecyclerView.Adapter<CancionEstAdapter.CancionViewHolder>() {

    class CancionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numero: TextView = itemView.findViewById(R.id.numero)
        val nombre: TextView = itemView.findViewById(R.id.nombre)
        val artista: TextView = itemView.findViewById(R.id.artista)
        val duracion: TextView = itemView.findViewById(R.id.duracion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CancionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cancionest, parent, false)
        return CancionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CancionViewHolder, position: Int) {
        val cancion = canciones[position]
        holder.numero.text = (position + 1).toString()
        holder.nombre.text = cancion.nombre
        holder.artista.text = nombreArtista
        holder.duracion.text = formatearDuracion(cancion.duracion)
    }

    override fun getItemCount(): Int = canciones.size

    fun formatearDuracion(segundos: Int): String {
        val minutos = segundos / 60
        val segundosRestantes = segundos % 60
        return "${minutos}:${segundosRestantes}"
    }
}
