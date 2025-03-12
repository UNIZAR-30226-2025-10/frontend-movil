package com.example.myapplication.Adapters.Home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.*

class ArtistasAdapter(private var listaArtistas: List<HArtistas>) : RecyclerView.Adapter<ArtistasAdapter.ArtistaHViewHolder>() {
    // Cambia el método para actualizar la lista
    fun updateDataArtista(searchResponse: List<HArtistas>) {
        listaArtistas = searchResponse
        notifyDataSetChanged()  // Notifica al adaptador que se actualizó la lista
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistaHViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_artista, parent, false)
        return ArtistaHViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtistaHViewHolder, position: Int) {
        val artista = listaArtistas[position]
        holder.nombreArtista.text = artista.nombreArtistico
        Glide.with(holder.itemView.context)
            .load(artista.fotoPerfil)
            .into(holder.fotoPerfil)
    }

    override fun getItemCount(): Int {
        return listaArtistas.size
    }

    class ArtistaHViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreArtista: TextView = itemView.findViewById(R.id.textViewArtist)
        val fotoPerfil: ImageView = itemView.findViewById(R.id.imageView)
    }
}