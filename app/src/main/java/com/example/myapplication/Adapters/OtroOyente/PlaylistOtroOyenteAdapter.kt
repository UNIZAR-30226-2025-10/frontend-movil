package com.example.myapplication.Adapters.OtroOyente

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.Playlist
import com.example.myapplication.io.response.PlaylistOyente

class PlaylistOtroOyenteAdapter(
    private var playlist: List<PlaylistOyente>,
    private val clickListener: (PlaylistOyente) -> Unit
) : RecyclerView.Adapter<PlaylistOtroOyenteAdapter.CancionViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CancionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist, parent, false)
        return CancionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CancionViewHolder, position: Int) {
        val playlist = playlist[position]
        holder.nombre.text = playlist.nombre


        // Cargar la imagen del álbum con Glide
        if (!playlist.fotoPortada.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(playlist.fotoPortada)
                .placeholder(R.drawable.no_cancion)  // Imagen por defecto
                .error(R.drawable.no_cancion)
                .into(holder.fotoPortada)
        }
        holder.itemView.setOnClickListener { clickListener(playlist) }
    }

    override fun getItemCount(): Int = playlist.size

    fun submitList(newCanciones: List<PlaylistOyente>) {
        Log.d("otroOyente1", "\"HAY submit con ${newCanciones.size} items")
        playlist = newCanciones
        notifyDataSetChanged()
    }

    class CancionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.textView)
        val artista: TextView = view.findViewById(R.id.usuarioPlaylist)
        val fotoPortada: ImageView = view.findViewById(R.id.imageView)
    }
}
