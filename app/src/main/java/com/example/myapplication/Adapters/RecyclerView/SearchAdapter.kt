import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.*

class SearchAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var results: List<SearchResultItem> = emptyList()

    fun updateData(searchResponse: BuscadorResponse) {
        results = listOf(
            SearchResultItem.HeaderItem("Canciones"),
            SearchResultItem.CancionItem(searchResponse.canciones),

            SearchResultItem.HeaderItem("Álbumes"),
            SearchResultItem.AlbumItem(searchResponse.albumes),

            SearchResultItem.HeaderItem("Artistas"),
            SearchResultItem.ArtistaItem(searchResponse.artistas),

            SearchResultItem.HeaderItem("Playlists"),
            SearchResultItem.PlaylistItem(searchResponse.playlists),

            SearchResultItem.HeaderItem("Perfiles"),
            SearchResultItem.PerfilItem(searchResponse.perfiles)
        )
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (results[position]) {
            is SearchResultItem.HeaderItem -> VIEW_TYPE_HEADER
            is SearchResultItem.CancionItem -> VIEW_TYPE_CANCION
            is SearchResultItem.AlbumItem -> VIEW_TYPE_ALBUM
            is SearchResultItem.ArtistaItem -> VIEW_TYPE_ARTISTA
            is SearchResultItem.PlaylistItem -> VIEW_TYPE_PLAYLIST
            is SearchResultItem.PerfilItem -> VIEW_TYPE_PERFIL
            else -> throw IllegalArgumentException("Invalid item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_header, parent, false))
            VIEW_TYPE_CANCION -> CancionViewHolder(inflater.inflate(R.layout.item_cancion, parent, false))
            VIEW_TYPE_ALBUM -> AlbumViewHolder(inflater.inflate(R.layout.item_album, parent, false))
            VIEW_TYPE_ARTISTA -> ArtistaViewHolder(inflater.inflate(R.layout.item_artista, parent, false))
            VIEW_TYPE_PLAYLIST -> PlaylistViewHolder(inflater.inflate(R.layout.item_playlist, parent, false))
            VIEW_TYPE_PERFIL -> PerfilViewHolder(inflater.inflate(R.layout.item_perfil, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = results[position]) {
            is SearchResultItem.HeaderItem -> (holder as HeaderViewHolder).bind(item.title)
            is SearchResultItem.CancionItem -> {
                val cancion = item.cancion[position] // Obtén la canción individual
                (holder as CancionViewHolder).bind(cancion)
            }
            is SearchResultItem.AlbumItem -> {
                val album = item.album[position]
                (holder as AlbumViewHolder).bind(album)
            }
            is SearchResultItem.ArtistaItem -> {
                val artista = item.artista[position]
                (holder as ArtistaViewHolder).bind(artista)
            }
            is SearchResultItem.PlaylistItem -> {
                val playlist = item.playlist[position]
                (holder as PlaylistViewHolder).bind(playlist)
            }
            is SearchResultItem.PerfilItem -> {
                val perfil = item.perfil[position]
                (holder as PerfilViewHolder).bind(perfil)
            }
        }
    }

    override fun getItemCount(): Int = results.size

    companion object {
        private const val VIEW_TYPE_HEADER = 1
        private const val VIEW_TYPE_CANCION = 2
        private const val VIEW_TYPE_ALBUM = 3
        private const val VIEW_TYPE_ARTISTA = 4
        private const val VIEW_TYPE_PLAYLIST = 5
        private const val VIEW_TYPE_PERFIL = 6
    }
}

class CancionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val imageView: ImageView = view.findViewById(R.id.imageView)
    private val textView: TextView = view.findViewById(R.id.textView)

    fun bind(cancion: Cancion) {
        textView.text = cancion.nombre
        Glide.with(itemView.context).load(cancion.fotoPortada).into(imageView)
    }
}

class AlbumViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val imageView: ImageView = view.findViewById(R.id.imageView)
    private val textView: TextView = view.findViewById(R.id.textView)

    fun bind(album: Album) {
        textView.text = album.nombre
        Glide.with(itemView.context).load(album.fotoPortada).into(imageView)
    }
}

class ArtistaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val imageView: ImageView = view.findViewById(R.id.imageView)
    private val textView: TextView = view.findViewById(R.id.textView)

    fun bind(artista: Artista) {
        textView.text = artista.nombreArtistico
        Glide.with(itemView.context).load(artista.fotoPerfil).into(imageView)
    }
}

class PlaylistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val imageView: ImageView = view.findViewById(R.id.imageView)
    private val textView: TextView = view.findViewById(R.id.textView)

    fun bind(playlist: Playlist) {
        textView.text = playlist.nombre
        Glide.with(itemView.context).load(playlist.fotoPortada).into(imageView)
    }
}

class PerfilViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val imageView: ImageView = view.findViewById(R.id.imageView)
    private val textView: TextView = view.findViewById(R.id.textView)

    fun bind(perfil: Perfil) {
        textView.text = perfil.nombreUsuario
        Glide.with(itemView.context).load(perfil.fotoPerfil).into(imageView)
    }
}

class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val textView: TextView = view.findViewById(R.id.headerTextView)

    fun bind(title: String) {
        textView.text = title
    }
}
