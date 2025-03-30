package com.example.myapplication.Adapters.Playlist

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.*

class CancionPAdapter(
    private var listaCanciones: List<CancionP>,
    private val clickListener: (CancionP) -> Unit
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
        holder.nombreCancion.text = cancion.nombre
        holder.artistaCancion.text = cancion.nombreArtisticoArtista
        Log.d("MiApp", "Cancion detalle id: ${cancion.id}")
        Glide.with(holder.itemView.context)
            .load(cancion.fotoPortada)
            .into(holder.imagenCancion)

        // Corrección: Se pasa una lambda en lugar de ejecutar la función
        holder.itemView.setOnClickListener { clickListener(cancion) }
    }

    override fun getItemCount(): Int = listaCanciones.size

    class CancionPViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreCancion: TextView = itemView.findViewById(R.id.textView)
        val artistaCancion: TextView = itemView.findViewById(R.id.textViewArtist)
        val imagenCancion: ImageView = itemView.findViewById(R.id.imageView)
    }
}
