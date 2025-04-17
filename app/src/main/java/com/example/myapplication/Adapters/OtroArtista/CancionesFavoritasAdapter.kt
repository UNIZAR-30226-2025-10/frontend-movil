package com.example.myapplication.Adapters.OtroArtista

import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.io.response.cancionFavoritaArtista
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.myapplication.R

class CancionesFavoritasAdapter(
    private var canciones: List<cancionFavoritaArtista>
) : RecyclerView.Adapter<CancionesFavoritasAdapter.CancionViewHolder>() {

    inner class CancionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numero: TextView = itemView.findViewById(R.id.numero)
        val nombre: TextView = itemView.findViewById(R.id.nombre)
        val album: TextView = itemView.findViewById(R.id.album)
        val foto: ImageView = itemView.findViewById(R.id.fotoPortada)
        val duracion: TextView = itemView.findViewById(R.id.duracion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CancionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_canciones_fav_artista, parent, false)
        return CancionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CancionViewHolder, position: Int) {
        val cancion = canciones[position]

        // Actualiza el número de la canción
        holder.numero.text = (position + 1).toString()

        // Actualiza los otros campos
        holder.nombre.text = cancion.nombre
        holder.album.text = cancion.album

        // Calcula la duración de la canción en minutos y segundos
        val segundos = cancion.duracion.toIntOrNull() ?: 0
        val minutos = segundos / 60
        val restoSegundos = segundos % 60
        holder.duracion.text = String.format("%d:%02d", minutos, restoSegundos)

        // Carga la imagen de la portada
        Glide.with(holder.itemView.context)
            .load(cancion.fotoPortada)
            .placeholder(R.drawable.no_cancion)
            .into(holder.foto)
    }

    override fun getItemCount(): Int = canciones.size

    fun submitList(newCanciones: List<cancionFavoritaArtista>) {
        canciones = newCanciones
        notifyDataSetChanged()
    }
}
