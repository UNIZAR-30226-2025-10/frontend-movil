package com.example.myapplication.Adapters.OtroArtista

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
import com.example.myapplication.io.response.Cancion
import com.example.myapplication.io.response.CancionesArtista

class CancionesArtistaAdapter(private val clickListener: (CancionesArtista) -> Unit)
    : RecyclerView.Adapter<CancionesArtistaAdapter.CancionViewHolder>() {

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
                .placeholder(R.drawable.no_cancion)  // Imagen por defecto
                .error(R.drawable.no_cancion)
                .into(holder.fotoPortada)
        }

        // Corrección: Se pasa una lambda en lugar de ejecutar la función
        holder.itemView.setOnClickListener { clickListener(cancion) }
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
