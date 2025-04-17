package com.example.myapplication.Adapters.OtroArtista

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.CancionesArtista

class CancionesArtistaAdapter : RecyclerView.Adapter<CancionesArtistaAdapter.CancionViewHolder>() {

    private var canciones: List<CancionesArtista> = listOf()
    private var nombreArtista: String = ""  // Variable para almacenar el nombre del artista

    // Método para actualizar el nombre del artista
    fun actualizarNombreArtista(nombre: String) {
        nombreArtista = nombre
        notifyDataSetChanged()  // Notificar que el nombre del artista ha cambiado
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CancionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cancion, parent, false)
        return CancionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CancionViewHolder, position: Int) {
        val cancion = canciones[position]
        holder.nombre.text = cancion.nombre
        holder.artista.text = nombreArtista

        // Cargar la imagen del álbum con Glide
        if (!cancion.fotoPortada.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(cancion.fotoPortada)
                .placeholder(R.drawable.no_cancion)  // Imagen por defecto
                .error(R.drawable.no_cancion)
                .into(holder.fotoPortada)
        }
    }

    override fun getItemCount(): Int = canciones.size

    fun submitList(newCanciones: List<CancionesArtista>) {
        canciones = newCanciones
        notifyDataSetChanged()
    }

    // ViewHolder para los álbumes
    class CancionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.textView)
        val artista: TextView = view.findViewById(R.id.textViewArtist)
        val fotoPortada: ImageView = view.findViewById(R.id.imageView)
    }
}
