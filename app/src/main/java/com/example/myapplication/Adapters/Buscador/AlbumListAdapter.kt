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

class AlbumAdapter(
    private var listaAlbumes: List<Album>,
) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    // Cambia el método para actualizar la lista
    fun updateDataAlbum(searchResponse: List<Album>) {
        listaAlbumes = searchResponse  // Actualiza directamente listaCanciones
        notifyDataSetChanged()  // Notifica al adaptador que se actualizó la lista
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_album, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = listaAlbumes[position]
        holder.nombreAlbum.text = album.nombre
        holder.artistaAlbum.text = album.nombreArtisticoArtista
        Glide.with(holder.itemView.context)
            .load(album.fotoPortada)
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
            .into(holder.imagenAlbum)

    }

    override fun getItemCount(): Int {
        return listaAlbumes.size
    }

    class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreAlbum: TextView = itemView.findViewById(R.id.textView)
        val artistaAlbum: TextView = itemView.findViewById(R.id.textViewArtist)
        val imagenAlbum: ImageView = itemView.findViewById(R.id.imageView)
    }
}