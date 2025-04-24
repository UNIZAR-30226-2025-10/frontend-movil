package com.example.myapplication.Adapters.CrearAlbum

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.request.CrearCancionRequest
import com.example.myapplication.io.request.NuevaCancionRequest
import com.example.myapplication.io.response.CancionEst

class CancionesAnadidasAdapter(private val canciones: MutableList<NuevaCancionRequest>, private val nombreArtista: String, private val onDelete: (Int) -> Unit ) :
    RecyclerView.Adapter<CancionesAnadidasAdapter.CancionViewHolder>() {

    class CancionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numero: TextView = itemView.findViewById(R.id.numero)
        val nombre: TextView = itemView.findViewById(R.id.textView)
        val artista: TextView = itemView.findViewById(R.id.textViewArtist)
        val duracion: TextView = itemView.findViewById(R.id.duracion)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnMoreOptions) // Cruz para eliminar
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CancionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cancion_crear_album, parent, false)
        return CancionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CancionViewHolder, position: Int) {
        val cancion = canciones[position]
        holder.numero.text = (position + 1).toString()
        holder.nombre.text = cancion.nombre
        if (cancion.artistasFt != null) {
            holder.artista.text = nombreArtista + " .ft " + cancion.artistasFt
        } else {
            holder.artista.text = nombreArtista
        }
        holder.duracion.text = formatearDuracion(cancion.duracion)

        holder.btnEliminar.setOnClickListener {
            onDelete(position)
        }
    }

    override fun getItemCount(): Int = canciones.size

    fun formatearDuracion(segundos: Int): String {
        val minutos = segundos / 60
        val segundosRestantes = segundos % 60
        return String.format("%d:%02d", minutos, segundosRestantes)
    }

    fun eliminarCancion(position: Int) {
        canciones.removeAt(position)
        notifyItemRemoved(position) // Notificar que un ítem ha sido removido
        notifyItemRangeChanged(position, canciones.size) // Actualizar los elementos restantes
    }
}