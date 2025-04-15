package com.example.myapplication.Adapters.Buscador

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.*

class PerfilAdapter(private var listaPerfiles: List<Perfil>,
                    private val clickListener: (Perfil) -> Unit
) : RecyclerView.Adapter<PerfilAdapter.PerfilViewHolder>() {

    // Cambia el método para actualizar la lista
    fun updateDataPerfiles(searchResponse: List<Perfil>) {
        listaPerfiles = searchResponse  // Actualiza directamente listaCanciones
        notifyDataSetChanged()  // Notifica al adaptador que se actualizó la lista
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PerfilViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_perfil, parent, false)
        return PerfilViewHolder(view)
    }

    override fun onBindViewHolder(holder: PerfilViewHolder, position: Int) {
        val perfil = listaPerfiles[position]
        holder.nombrePerfil.text = perfil.nombreUsuario
        Glide.with(holder.itemView.context)
            .load(perfil.fotoPerfil)
            .into(holder.imagenPerfil)

        holder.itemView.setOnClickListener { clickListener(perfil) }
    }

    override fun getItemCount(): Int {
        return listaPerfiles.size
    }

    class PerfilViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombrePerfil: TextView = itemView.findViewById(R.id.textView)
        val imagenPerfil: ImageView = itemView.findViewById(R.id.imageView)
    }
}
