package com.example.myapplication.Adapters.Playlist

import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.io.response.Cancion
import com.bumptech.glide.Glide // Asegúrate de tener Glide importado
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

class SongPlaylistSearchAdapter(
    private var songs: List<Cancion>,
    private val onAddSongClick: (Cancion) -> Unit // Callback para añadir canción
) : RecyclerView.Adapter<SongPlaylistSearchAdapter.SongViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song_search_playlist, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.bind(song)
    }

    override fun getItemCount() = songs.size

    fun updateData(newSongs: List<Cancion>) {
        songs = newSongs
        Log.d("MiAppPlaylist", "3")
        notifyDataSetChanged()
        Log.d("MiAppPlaylist", "3")
    }

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val songName: TextView = itemView.findViewById(R.id.songName)
        private val artistName: TextView = itemView.findViewById(R.id.artistName)
        private val btnAddSong: Button = itemView.findViewById(R.id.btnAddSong)
        private val imageSong: ImageView = itemView.findViewById(R.id.imageView)

        fun bind(song: Cancion) {
            songName.text = song.nombre
            artistName.text = song.nombreArtisticoArtista

            // Usar Glide para cargar la imagen de la portada
            Glide.with(itemView.context)
                .load(song.fotoPortada) // Aquí la URL de la foto de portada
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
                .into(imageSong) // Cargar la imagen en el ImageView

            itemView.setOnClickListener {
                onAddSongClick(song)
            }

            // Configurar el botón "Añadir"
            btnAddSong.setOnClickListener {
                onAddSongClick(song) // Llamar al callback para añadir la canción
            }
        }
    }


}
