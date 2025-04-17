package com.example.myapplication.Adapters.OtroArtista

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.CancionPopulares

class CancionesPopularesAdapter(
    private val onFavoriteClick: (CancionPopulares, Boolean, Int) -> Unit
) : RecyclerView.Adapter<CancionesPopularesAdapter.CancionViewHolder>() {

    private val canciones = mutableListOf<CancionPopulares>()
    private var nombreArtista = ""

    fun actualizarNombreArtista(nombre: String) {
        nombreArtista = nombre
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CancionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_canciones_populares, parent, false)
        return CancionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CancionViewHolder, position: Int) {
        holder.bind(canciones[position], position)
    }

    override fun getItemCount() = canciones.size

    fun submitList(newCanciones: List<CancionPopulares>) {
        canciones.clear()
        canciones.addAll(newCanciones)
        notifyDataSetChanged()
    }

    fun updateFavoriteState(position: Int, isFavorite: Boolean) {
        if (position in canciones.indices) {
            canciones[position].fav = isFavorite
            notifyItemChanged(position)
        }
    }

    inner class CancionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val numero: TextView = view.findViewById(R.id.numero)
        private val nombre: TextView = view.findViewById(R.id.nombre)
        private val artista: TextView = view.findViewById(R.id.artista)
        private val fotoPortada: ImageView = view.findViewById(R.id.fotoPortada)
        private val favorito: ImageButton = view.findViewById(R.id.favorito)
        private val duracion: TextView = view.findViewById(R.id.duracion)

        fun bind(cancion: CancionPopulares, position: Int) {
            numero.text = (position + 1).toString()
            nombre.text = cancion.nombre
            val segundos = cancion.duracion.toIntOrNull() ?: 0
            val minutos = segundos / 60
            val restoSegundos = segundos % 60
            duracion.text = String.format("%d:%02d", minutos, restoSegundos)

            val featuringText = if (cancion.featuring.isNotEmpty()) {
                " ft. ${cancion.featuring.joinToString(", ")}"
            } else {
                ""
            }
            artista.text = nombreArtista + featuringText

            Glide.with(itemView.context)
                .load(cancion.fotoPortada)
                .placeholder(R.drawable.no_cancion)
                .error(R.drawable.no_cancion)
                .into(fotoPortada)

            favorito.setImageResource(
                if (cancion.fav) R.drawable.ic_heart_lleno
                else R.drawable.ic_heart_vacio
            )

            favorito.setOnClickListener {
                it.isEnabled = false // Deshabilitar temporalmente

                val newState = !cancion.fav
                favorito.setImageResource(
                    if (newState) R.drawable.ic_heart_lleno
                    else R.drawable.ic_heart_vacio
                )

                // Habilitar después de 500ms para evitar múltiples clicks
                favorito.postDelayed({ it.isEnabled = true }, 500)

                onFavoriteClick(cancion, newState, position)
            }
        }
    }
}