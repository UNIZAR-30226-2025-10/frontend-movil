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

class PlaylistsAdapter (
    private var listaMisPLaylists: MutableList<MisPlaylist>,
    private val clickListener: (Playlist) -> Unit
) : RecyclerView.Adapter<PlaylistsAdapter.MisPlaylistViewHolder>() {

    // Cambia el método para actualizar la lista
    fun updateDataMisPlaylists(searchResponse: List<MisPlaylist>) {
        listaMisPLaylists.clear()
        listaMisPLaylists.addAll(searchResponse)  // Actualiza directamente listaCanciones
        notifyDataSetChanged()  // Notifica al adaptador que se actualizó la lista
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MisPlaylistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist, parent, false)
        return MisPlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: MisPlaylistViewHolder, position: Int) {
        val playlist = listaMisPLaylists[position]
        holder.nombrePlaylist.text = playlist.nombre
        Glide.with(holder.itemView.context)
            .load(playlist.fotoPortada)
            .into(holder.imagenPlaylist)
    }

    override fun getItemCount(): Int {
        return listaMisPLaylists.size
    }

    class MisPlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombrePlaylist: TextView = itemView.findViewById(R.id.textView)
        val imagenPlaylist: ImageView = itemView.findViewById(R.id.imageView)
    }
}