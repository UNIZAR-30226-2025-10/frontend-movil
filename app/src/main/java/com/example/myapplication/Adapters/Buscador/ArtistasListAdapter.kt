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

class ArtistaAdapter(private var listaArtistas: List<Artista>) : RecyclerView.Adapter<ArtistaAdapter.ArtistaViewHolder>() {

    // Cambia el método para actualizar la lista
    fun updateDataArtista(searchResponse: List<Artista>) {
        listaArtistas = searchResponse  // Actualiza directamente listaCanciones
        notifyDataSetChanged()  // Notifica al adaptador que se actualizó la lista
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_artista, parent, false)
        return ArtistaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtistaAdapter.ArtistaViewHolder, position: Int) {
        val artista = listaArtistas[position]
        holder.nombreArtista.text = artista.nombreArtistico
        Glide.with(holder.itemView.context)
            .load(artista.fotoPerfil)
            .circleCrop()
            .into(holder.imagenArtista)
    }

    override fun getItemCount(): Int {
        return listaArtistas.size
    }

    class ArtistaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreArtista: TextView = itemView.findViewById(R.id.textView)
        val imagenArtista: ImageView = itemView.findViewById(R.id.imageView)
    }
}
