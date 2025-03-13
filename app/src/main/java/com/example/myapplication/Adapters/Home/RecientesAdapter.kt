package com.example.myapplication.Adapters.Home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.*

class RecientesAdapter(private var listaRecientes: MutableList<HRecientes>) :
    RecyclerView.Adapter<RecientesAdapter.RecienteHViewHolder>() {
    // Cambia el método para actualizar la lista
    fun updateDataReciente(searchResponse: List<HRecientes>) {
        listaRecientes.clear()
        listaRecientes.addAll(searchResponse)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecienteHViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reciente, parent, false)
        return RecienteHViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecienteHViewHolder, position: Int) {
        val Reciente = listaRecientes[position]
        holder.nombreReciente.text = Reciente.nombre
        Glide.with(holder.itemView.context)
            .load(Reciente.fotoPortada)
            .into(holder.foto)
    }

    override fun getItemCount(): Int {
        return listaRecientes.size
    }

    class RecienteHViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreReciente: TextView = itemView.findViewById(R.id.textViewArtist)
        val foto: ImageView = itemView.findViewById(R.id.imageView)
    }
}