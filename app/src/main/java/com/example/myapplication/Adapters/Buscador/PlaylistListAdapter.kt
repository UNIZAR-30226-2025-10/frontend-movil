package com.example.myapplication.Adapters.Buscador

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.myapplication.R
import com.example.myapplication.io.response.*

class PlaylistAdapter(
    private var listaPLaylists: List<Playlist>,
    private val clickListener: (Playlist) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

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
        holder.nombreCreador.text = playlist.nombreUsuarioCreador
        var foto: Any
        if (playlist.fotoPortada == "DEFAULT") {
            foto = R.drawable.no_cancion
        } else {
            foto = playlist.fotoPortada
        }
        Glide.with(holder.itemView.context)
            .load(foto)
            .centerCrop()
            .transform(
                RoundedCorners(
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    12f,
                    holder.itemView.context.resources.displayMetrics
                ).toInt()
            )
            )
            .into(holder.imagenPlaylist)

        holder.itemView.setOnClickListener { clickListener(playlist) }
    }

    override fun getItemCount(): Int {
        return listaPLaylists.size
    }

    class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombrePlaylist: TextView = itemView.findViewById(R.id.textView)
        val nombreCreador: TextView = itemView.findViewById(R.id.usuarioPlaylist)
        val imagenPlaylist: ImageView = itemView.findViewById(R.id.imageView)
    }
}
