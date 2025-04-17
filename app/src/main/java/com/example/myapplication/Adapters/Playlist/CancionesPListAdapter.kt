package com.example.myapplication.Adapters.Playlist

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.Cancion
import com.example.myapplication.io.response.CancionP

class CancionPAdapter(
    private var listaCanciones: List<CancionP>,
    private val clickListener: (CancionP) -> Unit,
    private val onOptionsClick: (CancionP) -> Unit,
    private val onFavoriteClick: (CancionP, isFavorite: Boolean) -> Unit
) : RecyclerView.Adapter<CancionPAdapter.CancionPViewHolder>() {

    fun updateData(searchResponse: List<CancionP>) {
        listaCanciones = searchResponse
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CancionPViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cancion_playlist, parent, false)
        return CancionPViewHolder(view)
    }

    override fun onBindViewHolder(holder: CancionPViewHolder, position: Int) {
        val cancion = listaCanciones[position]
        holder.bind(cancion)
    }

    override fun getItemCount(): Int = listaCanciones.size

    inner class CancionPViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nombreCancion: TextView = itemView.findViewById(R.id.textView)
        private val artistaCancion: TextView = itemView.findViewById(R.id.textViewArtist)
        private val imagenCancion: ImageView = itemView.findViewById(R.id.imageView)
        private val btnOptions: ImageButton = itemView.findViewById(R.id.btnMoreOptions)
        private val btnFavorite: ImageButton = itemView.findViewById(R.id.btn_favorito)

        fun bind(cancion: CancionP) {
            nombreCancion.text = cancion.nombre
            val featuringText = if (cancion.featuring.isNotEmpty()) {
                " ft. ${cancion.featuring.joinToString(", ")}"
            } else {
                ""
            }
            artistaCancion.text = cancion.nombreArtisticoArtista + featuringText

            Glide.with(itemView.context)
                .load(cancion.fotoPortada)
                .into(imagenCancion)

            // Mostrar el estado actual de favorito
            btnFavorite.setImageResource(
                if (cancion.fav) R.drawable.ic_heart_lleno
                else R.drawable.ic_heart_vacio
            )

            itemView.setOnClickListener { clickListener(cancion) }

            btnOptions.setOnClickListener {
                onOptionsClick(cancion)
            }

            btnFavorite.setOnClickListener {
                // Cambiar el estado inmediatamente para feedback visual
                val newFavState = !cancion.fav
                btnFavorite.setImageResource(
                    if (newFavState) R.drawable.ic_heart_lleno
                    else R.drawable.ic_heart_vacio
                )
                // Notificar al activity/fragment
                onFavoriteClick(cancion, newFavState)
            }
        }
    }
}