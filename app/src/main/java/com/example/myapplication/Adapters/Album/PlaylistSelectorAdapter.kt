package com.example.myapplication.Adapters.Album

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.MisPlaylist

class PlaylistSelectorAdapter(
    private var playlists: List<MisPlaylist>,
    private val onPlaylistSelected: (MisPlaylist) -> Unit
) : RecyclerView.Adapter<PlaylistSelectorAdapter.PlaylistViewHolder>(), Filterable {

    private var filteredList: List<MisPlaylist> = playlists

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist_selection_album, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(filteredList[position], position)
    }

    override fun getItemCount() = filteredList.size

    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playlistNumber: TextView = itemView.findViewById(R.id.numero) // Añadimos un TextView para el número
        private val playlistName: TextView = itemView.findViewById(R.id.nombre)
        private val foto: ImageView = itemView.findViewById(R.id.fotoPortada)

        fun bind(playlist: MisPlaylist, position: Int) {
            // Asignamos el número a partir de la posición en la lista (empezando desde 1)
            playlistNumber.text = (position + 1).toString()

            playlistName.text = playlist.nombre
            Glide.with(itemView.context)
                .load(playlist.fotoPortada)
                .into(foto)
            itemView.setOnClickListener { onPlaylistSelected(playlist) }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filtered = if (constraint.isNullOrEmpty()) {
                    playlists
                } else {
                    playlists.filter {
                        it.nombre.contains(constraint, ignoreCase = true)
                    }
                }
                return FilterResults().apply { values = filtered }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as? List<MisPlaylist> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}
