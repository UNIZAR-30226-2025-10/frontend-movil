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
import com.example.myapplication.io.response.MisCanciones
import com.example.myapplication.io.response.Usuario
import com.example.myapplication.utils.Preferencias

class CancionesAdapter(
    private var listaMisCanciones: MutableList<MisCanciones>,
    private val nombreUsuario: String,
    private val clickListener: (MisCanciones) -> Unit
) : RecyclerView.Adapter<CancionesAdapter.MisCancionesViewHolder>() {

    // Método para actualizar la lista de canciones
    fun updateDataMisCanciones(nuevasCanciones: List<MisCanciones>) {
        listaMisCanciones.clear()
        listaMisCanciones.addAll(nuevasCanciones)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MisCancionesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cancion, parent, false)
        return MisCancionesViewHolder(view)
    }

    override fun onBindViewHolder(holder: MisCancionesViewHolder, position: Int) {
        val cancion = listaMisCanciones[position]
        holder.nombreCancion.text = cancion.nombre
        holder.nombreArtista.text = nombreUsuario
        // Cargar imagen con Glide
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
            .into(holder.imagenCancion)

        Log.d("CancionesAdapter", "Cargando imagen: ${cancion.fotoPortada}")
        Log.d("CancionesAdapter", "user: ${nombreUsuario}")

        holder.itemView.setOnClickListener {
            clickListener(cancion)
        }
    }

    override fun getItemCount(): Int {
        return listaMisCanciones.size
    }

    class MisCancionesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreCancion: TextView = itemView.findViewById(R.id.textView) // ajusta ID si es diferente
        val imagenCancion: ImageView = itemView.findViewById(R.id.imageView) // ajusta ID si es diferente
        val nombreArtista: TextView = itemView.findViewById((R.id.textViewArtist))
    }
}
