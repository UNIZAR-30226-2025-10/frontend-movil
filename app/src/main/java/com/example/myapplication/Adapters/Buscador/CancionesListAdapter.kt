package com.example.myapplication.Adapters.Buscador

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

class CancionAdapter(
    private var listaCanciones: List<Cancion>,
    private val clickListener: (Cancion) -> Unit
) : RecyclerView.Adapter<CancionAdapter.CancionViewHolder>() {

    fun updateData(searchResponse: List<Cancion>) {
        listaCanciones = searchResponse
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CancionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cancion, parent, false)
        return CancionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CancionViewHolder, position: Int) {
        val cancion = listaCanciones[position]
        holder.nombreCancion.text = cancion.nombre
        holder.artistaCancion.text = cancion.nombreArtisticoArtista
        Log.d("MiApp", "Cancion detalle id: ${cancion.id}")
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

        // Corrección: Se pasa una lambda en lugar de ejecutar la función
        holder.itemView.setOnClickListener { clickListener(cancion) }
    }

    override fun getItemCount(): Int = listaCanciones.size

    class CancionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreCancion: TextView = itemView.findViewById(R.id.textView)
        val artistaCancion: TextView = itemView.findViewById(R.id.textViewArtist)
        val imagenCancion: ImageView = itemView.findViewById(R.id.imageView)
    }
}
