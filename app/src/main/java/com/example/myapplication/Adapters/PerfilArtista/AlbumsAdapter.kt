package com.example.myapplication.Adapters.PerfilArtista

import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.myapplication.R
import com.example.myapplication.io.response.*

class AlbumsAdapter(
    private var listaMisAlbumss: MutableList<MisAlbums>,
    private val clickListener: (MisAlbums) -> Unit
) : RecyclerView.Adapter<AlbumsAdapter.MisAlbumsViewHolder>() {

    // Cambia el método para actualizar la lista
    fun updateDataMisAlbums(searchResponse: List<MisAlbums>) {
        listaMisAlbumss.clear()
        listaMisAlbumss.addAll(searchResponse)  // Actualiza directamente listaCanciones
        notifyDataSetChanged()  // Notifica al adaptador que se actualizó la lista
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MisAlbumsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_album_perfil_artista, parent, false)
        return MisAlbumsViewHolder(view)
    }

    override fun onBindViewHolder(holder: MisAlbumsViewHolder, position: Int) {
        val album = listaMisAlbumss[position]
        holder.nombreAlbum.text = album.nombre
        Glide.with(holder.itemView.context)
            .load(album.fotoPortada)
            .transform(
                MultiTransformation(
                    CenterCrop(),
                    RoundedCorners(
                        TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            12f,
                            holder.itemView.context.resources.displayMetrics
                        ).toInt()
                    )
                )
            )
            .into(holder.imagenAlbum)

        val imagen = holder.imagenAlbum
        Log.d("AdapterP", "Imagen album: $imagen")


        holder.itemView.setOnClickListener { clickListener(album) }
    }

    override fun getItemCount(): Int {
        return listaMisAlbumss.size
    }

    class MisAlbumsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreAlbum: TextView = itemView.findViewById(R.id.textView)
        val imagenAlbum: ImageView = itemView.findViewById(R.id.imageView)
    }
}