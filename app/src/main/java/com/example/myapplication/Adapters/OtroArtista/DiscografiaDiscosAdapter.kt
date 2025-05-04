package com.example.myapplication.Adapters.OtroArtista

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
import com.example.myapplication.io.response.AlbumArtista

class DiscografiaDiscosAdapter(
    private var onItemClick: (AlbumArtista) -> Unit // Lambda para manejar clics
) : RecyclerView.Adapter<DiscografiaDiscosAdapter.DiscoViewHolder>() {

    private var albumes: List<AlbumArtista> = listOf()
    private var nombreArtista: String = ""

    fun actualizarNombreArtista(nombre: String) {
        nombreArtista = nombre
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_album, parent, false)
        return DiscoViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiscoViewHolder, position: Int) {
        val album = albumes[position]
        holder.nombre.text = album.nombre
        holder.artista.text = nombreArtista

        // Cargar imagen con Glide
        if (!album.fotoPortada.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(album.fotoPortada)
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
                .placeholder(R.drawable.no_cancion)
                .error(R.drawable.no_cancion)
                .into(holder.fotoPortada)
        }

        // Configurar clic en el item
        holder.itemView.setOnClickListener {
            onItemClick(album) // Llamar a la lambda con el álbum seleccionado
        }
    }

    override fun getItemCount(): Int = albumes.size

    fun submitList(newAlbumes: List<AlbumArtista>) {
        albumes = newAlbumes
        notifyDataSetChanged()
    }

    class DiscoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.textView)
        val artista: TextView = view.findViewById(R.id.textViewArtist)
        val fotoPortada: ImageView = view.findViewById(R.id.imageView)
    }
}
