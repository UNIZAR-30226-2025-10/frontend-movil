package com.example.myapplication.Adapters.Perfil

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.HArtistas

class TopArtistasAdapter(
    private var artistas: MutableList<HArtistas>,
    private val clickListener: (HArtistas) -> Unit
) : RecyclerView.Adapter<TopArtistasAdapter.ArtistaViewHolder>() {

    fun updateData(newItems: List<HArtistas>) {
        artistas.clear()
        artistas.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_artista, parent, false)
        return ArtistaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtistaViewHolder, position: Int) {
        val artista = artistas[position]
        holder.bind(artista)
        holder.itemView.setOnClickListener { clickListener(artista) }
    }

    override fun getItemCount(): Int = artistas.size

    class ArtistaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nombre: TextView = itemView.findViewById(R.id.textView)
        private val fotoPerfil: ImageView = itemView.findViewById(R.id.imageView)

        fun bind(artista: HArtistas) {
            nombre.text = artista.nombreArtistico

            val foto = if (artista.fotoPerfil == "DEFAULT") {
                R.drawable.ic_profile
            } else {
                artista.fotoPerfil
            }

            Glide.with(itemView.context)
                .load(foto)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(fotoPerfil)
        }
    }
}