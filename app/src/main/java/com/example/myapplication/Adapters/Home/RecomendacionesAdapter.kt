package com.example.myapplication.Adapters.Home

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
import com.example.myapplication.io.response.HCancion
import com.example.myapplication.io.response.Recomendaciones
import com.example.myapplication.utils.Preferencias

class RecomendacionesAdapter (
    private var listaRecomendaciones: MutableList<Recomendaciones>,
    private val clickListener: (Recomendaciones) -> Unit
) : RecyclerView.Adapter<RecomendacionesAdapter.RecomendacionViewHolder>() {
    // Cambia el método para actualizar la lista
    fun updateDataRecomendacion(searchResponse: List<Recomendaciones>) {
        listaRecomendaciones.clear()
        listaRecomendaciones.addAll(searchResponse)
        notifyDataSetChanged()  // Notifica al adaptador que se actualizó la lista
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecomendacionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cancion, parent, false)
        return RecomendacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecomendacionViewHolder, position: Int) {
        val recomendacion = listaRecomendaciones[position]
        holder.nombreCancion.text = recomendacion.nombre
        holder.nombreArtista.text = recomendacion.nombreArtisticoArtista
        var foto: Any
        if (recomendacion.fotoPortada == "DEFAULT") {
            foto = R.drawable.no_cancion
        } else {
            foto = recomendacion.fotoPortada
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
            .into(holder.imagenCancion)

        holder.itemView.setOnClickListener { clickListener(recomendacion) }
    }

    override fun getItemCount(): Int {
        return listaRecomendaciones.size
    }

    class RecomendacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreCancion: TextView = itemView.findViewById(R.id.textView)
        val nombreArtista: TextView = itemView.findViewById(R.id.textViewArtist)
        val imagenCancion: ImageView = itemView.findViewById(R.id.imageView)
    }
}