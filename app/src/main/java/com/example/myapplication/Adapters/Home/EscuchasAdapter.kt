package com.example.myapplication.Adapters.Home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.HCancion

    class EscuchasAdapter(private var listaEscuchas: MutableList<HCancion>) : RecyclerView.Adapter<EscuchasAdapter.EscuchaViewHolder>() {
    // Cambia el método para actualizar la lista
    fun updateDataEscucha(searchResponse: List<HCancion>) {
        listaEscuchas.clear()
        listaEscuchas.addAll(searchResponse)
        notifyDataSetChanged()  // Notifica al adaptador que se actualizó la lista
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EscuchaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cancion, parent, false)
        return EscuchaViewHolder(view)
    }

    override fun onBindViewHolder(holder: EscuchaViewHolder, position: Int) {
        val escucha = listaEscuchas[position]
        holder.nombreCancion.text = escucha.nombre
        holder.artistaCancion.text = escucha.nombreArtisticoArtista
        Glide.with(holder.itemView.context)
            .load(escucha.fotoPortada)
            .into(holder.imagenCancion)
    }

    override fun getItemCount(): Int {
        return listaEscuchas.size
    }

    class EscuchaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreCancion: TextView = itemView.findViewById(R.id.textView)
        val artistaCancion: TextView = itemView.findViewById(R.id.textViewArtist)
        val imagenCancion: ImageView = itemView.findViewById(R.id.imageView)
    }
}