package com.example.myapplication.Adapters.EstadisticasAlbum

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.io.response.CancionEst

class CancionEstAdapter(private val canciones: List<CancionEst>, private val nombreArtista: String,  private val listener: OnEstadisticasClickListener) :
    RecyclerView.Adapter<CancionEstAdapter.CancionViewHolder>() {

    interface OnEstadisticasClickListener {
        fun onVerEstadisticasClick(cancion: CancionEst)
    }

    class CancionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numero: TextView = itemView.findViewById(R.id.numero)
        val nombre: TextView = itemView.findViewById(R.id.nombre)
        val artista: TextView = itemView.findViewById(R.id.artista)
        val duracion: TextView = itemView.findViewById(R.id.duracion)
        val menuOpciones: ImageView = itemView.findViewById(R.id.menu_opciones)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CancionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cancionest, parent, false)
        return CancionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CancionViewHolder, position: Int) {
        val cancion = canciones[position]
        holder.numero.text = (position + 1).toString()
        holder.nombre.text = cancion.nombre
        holder.artista.text = nombreArtista
        holder.duracion.text = formatearDuracion(cancion.duracion)

        holder.menuOpciones.setOnClickListener {
            val popup = PopupMenu(holder.itemView.context, holder.menuOpciones, Gravity.END, 0, R.style.PopupMenuStyle)
            popup.menuInflater.inflate(R.menu.menu_estadisticasalbum, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.ver_estadisticas -> {
                        listener.onVerEstadisticasClick(cancion)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    override fun getItemCount(): Int = canciones.size

    fun formatearDuracion(segundos: Int): String {
        val minutos = segundos / 60
        val segundosRestantes = segundos % 60
        return "${minutos}:${segundosRestantes}"
    }
}
