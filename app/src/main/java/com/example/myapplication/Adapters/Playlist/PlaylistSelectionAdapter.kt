package com.example.myapplication.Adapters.Playlist

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.MisPlaylist
import android.widget.Filter
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

class PlaylistSelectionAdapter(
    private var playlists: List<MisPlaylist>,
    private val onPlaylistSelected: (MisPlaylist) -> Unit
) : RecyclerView.Adapter<PlaylistSelectionAdapter.PlaylistViewHolder>(), Filterable {

    private var filteredList: List<MisPlaylist> = playlists

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist_selection_album, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(filteredList[position], position)
    }

    override fun getItemCount(): Int = filteredList.size

    fun updateData(newPlaylists: List<MisPlaylist>) {
        playlists = newPlaylists
        filteredList = newPlaylists
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filteredResults = if (constraint.isNullOrEmpty()) {
                    playlists
                } else {
                    playlists.filter {
                        it.nombre.contains(constraint, true)
                    }
                }
                return FilterResults().apply { values = filteredResults }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as? List<MisPlaylist> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }

    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playlistNumber: TextView = itemView.findViewById(R.id.numero) // Añadimos un TextView para el número
        private val nameTextView: TextView = itemView.findViewById(R.id.nombre)
        private val imageView: ImageView = itemView.findViewById(R.id.fotoPortada)

        fun bind(playlist: MisPlaylist, position: Int) {
            // Asignamos el número a partir de la posición en la lista (empezando desde 1)
            playlistNumber.text = (position + 1).toString()
            nameTextView.text = playlist.nombre
            var foto: Any
            if (playlist.fotoPortada == "DEFAULT") {
                foto = R.drawable.no_cancion
            } else {
                foto = playlist.fotoPortada
            }
            Glide.with(itemView.context)
                .load(foto)
                .placeholder(R.drawable.no_cancion)
                .transform(
                    MultiTransformation(
                    CenterCrop(),
                    RoundedCorners(
                        TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            8f,
                            itemView.context.resources.displayMetrics
                        ).toInt()
                    )
                )
                )
                .into(imageView)

            itemView.setOnClickListener {
                onPlaylistSelected(playlist)
            }
        }
    }
}