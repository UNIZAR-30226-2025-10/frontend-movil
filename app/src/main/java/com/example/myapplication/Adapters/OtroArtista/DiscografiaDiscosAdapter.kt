package com.example.myapplication.Adapters.OtroArtista

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.AlbumArtista

class DiscografiaDiscosAdapter : RecyclerView.Adapter<DiscografiaDiscosAdapter.DiscoViewHolder>() {

    private var albumes: List<AlbumArtista> = listOf()
    private var nombreArtista: String = ""  // Variable para almacenar el nombre del artista

    // Método para actualizar el nombre del artista
    fun actualizarNombreArtista(nombre: String) {
        nombreArtista = nombre
        notifyDataSetChanged()  // Notificar que el nombre del artista ha cambiado
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_album, parent, false)
        return DiscoViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiscoViewHolder, position: Int) {
        val album = albumes[position]
        holder.nombre.text = album.nombre
        holder.artista.text = nombreArtista

        // Cargar la imagen del álbum con Glide
        if (!album.fotoPortada.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(album.fotoPortada)
                .placeholder(R.drawable.no_cancion)  // Imagen por defecto
                .error(R.drawable.no_cancion)
                .into(holder.fotoPortada)
        }
    }

    override fun getItemCount(): Int = albumes.size

    fun submitList(newAlbumes: List<AlbumArtista>) {
        albumes = newAlbumes
        notifyDataSetChanged()
    }

    // ViewHolder para los álbumes
    class DiscoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.textView)
        val artista: TextView = view.findViewById(R.id.textViewArtist)  // Texto donde se muestra el nombre del artista
        val fotoPortada: ImageView = view.findViewById(R.id.imageView)
    }
}
