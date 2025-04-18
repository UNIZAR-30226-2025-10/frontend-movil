package com.example.myapplication.Adapters.Album

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.CancionesAlbum
import com.example.myapplication.io.response.MisPlaylist

class CancionesAlbumAdapter(
    private var canciones: List<CancionesAlbum>,
    private var nombreArtista: String
) : RecyclerView.Adapter<CancionesAlbumAdapter.CancionViewHolder>() {

    // Listener para añadir canciones a playlist
    var onAddToPlaylist: ((CancionesAlbum, String) -> Unit)? = null

    // Listener para obtener las playlists del usuario
    var onGetPlaylists: (((List<MisPlaylist>) -> Unit) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CancionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_canciones_album, parent, false)
        return CancionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CancionViewHolder, position: Int) {
        val cancion = canciones[position]
        holder.bind(cancion, position, nombreArtista)
    }

    override fun getItemCount(): Int = canciones.size

    fun updateData(nuevasCanciones: List<CancionesAlbum>, nombreArtista: String) {
        canciones = nuevasCanciones
        this.nombreArtista = nombreArtista
        notifyDataSetChanged()
    }

    fun submitList(nuevasCanciones: List<CancionesAlbum>) {
        canciones = nuevasCanciones
        notifyDataSetChanged() // Notifica al adaptador para que actualice la UI
    }

    inner class CancionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val numero: TextView = itemView.findViewById(R.id.numero)
        private val nombre: TextView = itemView.findViewById(R.id.nombre)
        private val artista: TextView = itemView.findViewById(R.id.artista)
        private val duracion: TextView = itemView.findViewById(R.id.duracion)
        private val fotoPortada: ImageView = itemView.findViewById(R.id.fotoPortada)
        private val opciones: ImageButton = itemView.findViewById(R.id.options)

        fun bind(cancion: CancionesAlbum, position: Int, nombreArtista: String) {
            numero.text = (position + 1).toString()
            nombre.text = cancion.nombre

            // Procesar duración
            val segundos = cancion.duracion
            val minutos = segundos / 60
            val restoSegundos = segundos % 60
            duracion.text = String.format("%d:%02d", minutos, restoSegundos)

            artista.text = nombreArtista

            Glide.with(itemView.context)
                .load(cancion.fotoPortada)
                .placeholder(R.drawable.no_cancion)
                .error(R.drawable.no_cancion)
                .into(fotoPortada)

            opciones.setOnClickListener {
                showOptionsMenu(it, cancion)
            }
        }

        private fun showOptionsMenu(anchorView: View, cancion: CancionesAlbum) {
            val popup = PopupMenu(anchorView.context, anchorView)
            popup.menuInflater.inflate(R.menu.item_menu_options, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_add_to_playlist -> {
                        val playlists = onGetPlaylists?.invoke { playlists ->
                            Log.d("Playlist", "Playlists obtenidas: $playlists")
                            showPlaylistSelectionDialog(cancion, playlists)
                        }
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        private fun showPlaylistSelectionDialog(cancion: CancionesAlbum, playlists: Any) {
            val context = itemView.context
            val dialog = Dialog(context)
            dialog.setContentView(R.layout.dialog_select_playlist_album)
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val searchView = dialog.findViewById<SearchView>(R.id.searchViewPlaylists)
            val recyclerView = dialog.findViewById<RecyclerView>(R.id.recyclerViewPlaylists)
            val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

            // Casteamos playlists a List<MisPlaylist> de manera segura
            val playlistList = playlists as? List<MisPlaylist> ?: emptyList()

            // Configurar el adaptador
            val adapter = PlaylistSelectorAdapter(playlistList) { selectedPlaylist ->
                onAddToPlaylist?.invoke(cancion, selectedPlaylist.id)
                dialog.dismiss()
                Toast.makeText(context, "Añadido a ${selectedPlaylist.nombre}", Toast.LENGTH_SHORT).show()
            }

            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter

            // Configurar el buscador
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = false

                override fun onQueryTextChange(newText: String?): Boolean {
                    adapter.filter.filter(newText)
                    return true
                }
            })

            btnCancel.setOnClickListener { dialog.dismiss() }
            dialog.show()
        }
    }
}