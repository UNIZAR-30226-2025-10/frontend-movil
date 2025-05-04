package com.example.myapplication.Adapters.Home

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

class RecientesYArtistasAdapter(private var items: MutableList<Any>,
                                private val clickListener: (Any) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_RECIENTE = 1
        private const val TYPE_ARTISTA = 2
    }

    fun updateData(newItems: List<Any>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HRecientes -> TYPE_RECIENTE
            is HArtistas -> TYPE_ARTISTA
            else -> throw IllegalArgumentException("Tipo de vista desconocido")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_RECIENTE -> RecienteViewHolder(inflater.inflate(R.layout.item_reciente, parent, false))
            TYPE_ARTISTA -> ArtistaViewHolder(inflater.inflate(R.layout.item_artista, parent, false))
            else -> throw IllegalArgumentException("Vista desconocida")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is RecienteViewHolder -> holder.bind(items[position] as HRecientes)
            is ArtistaViewHolder -> holder.bind(items[position] as HArtistas)
        }
        holder.itemView.setOnClickListener { clickListener(items[position]) }
    }

    override fun getItemCount(): Int = items.size

    class RecienteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nombre: TextView = itemView.findViewById(R.id.textViewReciente)
        private val autor: TextView = itemView.findViewById(R.id.textViewReciente2)
        private val fotoPortada: ImageView = itemView.findViewById(R.id.imageViewReciente)

        fun bind(reciente: HRecientes) {
            nombre.text = reciente.nombre
            autor.text = reciente.autor

            var foto: Any
            if (reciente.fotoPortada == "DEFAULT") {
                foto = R.drawable.no_cancion
            } else {
                foto = reciente.fotoPortada
            }

            Glide.with(itemView.context)
                .load(foto)
                .transform(
                    MultiTransformation(
                        CenterCrop(),
                        RoundedCorners(
                            TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                12f,
                                itemView.context.resources.displayMetrics
                            ).toInt()
                        )
                    )
                )
                .into(fotoPortada)
        }
    }

    class ArtistaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nombre: TextView = itemView.findViewById(R.id.textView)
        private val fotoPerfil: ImageView = itemView.findViewById(R.id.imageView)

        fun bind(artista: HArtistas) {
            nombre.text = artista.nombreArtistico

            var foto: Any
            if (artista.fotoPerfil == "DEFAULT") {
                foto = R.drawable.ic_profile
            } else {
                foto = artista.fotoPerfil
            }

            Glide.with(itemView.context)
                .load(foto)
                .circleCrop()
                .into(fotoPerfil)
        }
    }
}
