package com.example.myapplication.Adapters.Home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.Recomendaciones

class RecomendacionesAdapter (private var listaRecomendaciones: MutableList<Recomendaciones>) : RecyclerView.Adapter<RecomendacionesAdapter.RecomendacionViewHolder>() {
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
        Glide.with(holder.itemView.context)
            .load(recomendacion.fotoPortada)
            .into(holder.imagenCancion)
    }

    override fun getItemCount(): Int {
        return listaRecomendaciones.size
    }

    class RecomendacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreCancion: TextView = itemView.findViewById(R.id.textView)
        val imagenCancion: ImageView = itemView.findViewById(R.id.imageView)
    }
}