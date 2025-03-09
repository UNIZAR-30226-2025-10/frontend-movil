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

class PlaylistAdapter(private var listaPLaylists: List<Playlist>) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    // Cambia el método para actualizar la lista
    fun updateDataPlaylists(searchResponse: List<Playlist>) {
        listaPLaylists = searchResponse  // Actualiza directamente listaCanciones
        notifyDataSetChanged()  // Notifica al adaptador que se actualizó la lista
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = listaPLaylists[position]
        holder.nombrePlaylist.text = playlist.nombre
        Glide.with(holder.itemView.context)
            .load(playlist.fotoPortada)
            .into(holder.imagenPlaylist)
    }

    override fun getItemCount(): Int {
        return listaPLaylists.size
    }

    class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombrePlaylist: TextView = itemView.findViewById(R.id.textView)
        val imagenPlaylist: ImageView = itemView.findViewById(R.id.imageView)
    }
}
