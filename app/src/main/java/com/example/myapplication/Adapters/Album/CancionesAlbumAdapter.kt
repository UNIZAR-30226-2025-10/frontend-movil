package com.example.myapplication.Adapters.Album

import android.app.Dialog
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TypefaceSpan
import android.util.Log
import android.view.Gravity
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
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.activities.AlbumDetail
import com.example.myapplication.activities.CrearPlaylist
import com.example.myapplication.io.response.CancionP
import com.example.myapplication.io.response.CancionesAlbum
import com.example.myapplication.io.response.MisPlaylist

class CancionesAlbumAdapter(
    private var canciones: List<CancionesAlbum>,
    private var nombreArtista: String,
    private val clickListener: (CancionesAlbum) -> Unit
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

            val featuringText = if (cancion.featuring.isNotEmpty()) {
                " ft. ${cancion.featuring.joinToString(", ")}"
            } else {
                ""
            }
            artista.text = nombreArtista + featuringText

            Glide.with(itemView.context)
                .load(cancion.fotoPortada)
                .placeholder(R.drawable.no_cancion)
                .error(R.drawable.no_cancion)
                .into(fotoPortada)

            opciones.setOnClickListener {
                showOptionsMenu(it, cancion)
            }

            itemView.setOnClickListener { clickListener(cancion) }

        }

        private fun showOptionsMenu(anchorView: View, cancion: CancionesAlbum) {
            val popup = PopupMenu(anchorView.context, anchorView, Gravity.START, 0, R.style.PopupMenuStyle)
            popup.menuInflater.inflate(R.menu.item_menu_options, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_add_to_playlist -> {
                        // Obtener las playlists
                        onGetPlaylists?.invoke { playlists ->
                            Log.d("Playlist", "Playlists obtenidas: $playlists")
                            showPlaylistSelectionDialog(cancion, playlists)
                        }
                        true
                    }
                    else -> false
                }
            }

            for (i in 0 until popup.menu.size()) {
                val item = popup.menu.getItem(i)
                val spanString = SpannableString(item.title)
                spanString.setSpan(
                    TypefaceSpan(ResourcesCompat.getFont(anchorView.context, R.font.poppins_regular)!!),
                    0, spanString.length,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                item.title = spanString
            }
            popup.show()
        }

        private fun showPlaylistSelectionDialog(cancion: CancionesAlbum, playlists: List<MisPlaylist>) {
            val context = itemView.context
            val dialog = Dialog(context)
            dialog.setContentView(R.layout.dialog_select_playlist_album)
            dialog.window?.setLayout((Resources.getSystem().displayMetrics.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val searchView = dialog.findViewById<SearchView>(R.id.searchViewPlaylists)
            val recyclerView = dialog.findViewById<RecyclerView>(R.id.recyclerViewPlaylists)
            val btnCreatePlaylist = dialog.findViewById<Button>(R.id.btnCreatePlaylist)

            // Configurar el adaptador con las playlists obtenidas
            val adapter = PlaylistSelectorAdapter(playlists) { selectedPlaylist ->
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

            // Cuando se crea una nueva playlist
            btnCreatePlaylist.setOnClickListener {
                val intent = Intent(context, CrearPlaylist::class.java)
                (context as? AlbumDetail)?.selectedCancionParaAñadir = cancion  // PASO IMPORTANTE
                (context as? AlbumDetail)?.startActivityForResult(intent, REQUEST_CREATE_PLAYLIST)
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    companion object {
        const val REQUEST_CREATE_PLAYLIST = 1
    }
}
