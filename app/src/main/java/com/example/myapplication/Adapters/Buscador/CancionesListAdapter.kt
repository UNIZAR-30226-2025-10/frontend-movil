package com.example.myapplication.Adapters.Buscador

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.*

class CancionAdapter(private val listaCanciones: List<Cancion>) : RecyclerView.Adapter<CancionAdapter.CancionViewHolder>() {

    private var results: List<SearchResultItem> = emptyList()
    fun updateData(searchResponse: List<Cancion>) {
        results = listOf(
            SearchResultItem.HeaderItem("Canciones"),
            SearchResultItem.CancionItem(searchResponse)
        )
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CancionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cancion, parent, false)
        return CancionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CancionViewHolder, position: Int) {
        val cancion = listaCanciones[position]
        holder.nombreCancion.text = cancion.nombre
        holder.artistaCancion.text = cancion.nombreArtisticoArtista
        Glide.with(holder.itemView.context)
            .load(cancion.fotoPortada)
            .into(holder.imagenCancion)
    }

    override fun getItemCount(): Int {
        return listaCanciones.size
    }

    class CancionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreCancion: TextView = itemView.findViewById(R.id.textView)
        val artistaCancion: TextView = itemView.findViewById(R.id.textViewArtist)
        val imagenCancion: ImageView = itemView.findViewById(R.id.imageView)
    }
}

