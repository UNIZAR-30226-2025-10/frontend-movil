package com.example.myapplication.Adapters.Playlist

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

class PlaylistSelectionAdapter(
    private var playlists: List<MisPlaylist>,
    private val onPlaylistSelected: (MisPlaylist) -> Unit
) : RecyclerView.Adapter<PlaylistSelectionAdapter.PlaylistViewHolder>(), Filterable {

    private var filteredList: List<MisPlaylist> = playlists

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist_selection, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(filteredList[position])
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
        private val nameTextView: TextView = itemView.findViewById(R.id.tvPlaylistName)
        private val imageView: ImageView = itemView.findViewById(R.id.ivPlaylistImage)

        fun bind(playlist: MisPlaylist) {
            nameTextView.text = playlist.nombre
            Glide.with(itemView.context)
                .load(playlist.fotoPortada)
                .placeholder(R.drawable.no_cancion)
                .into(imageView)

            itemView.setOnClickListener {
                onPlaylistSelected(playlist)
            }
        }
    }
}