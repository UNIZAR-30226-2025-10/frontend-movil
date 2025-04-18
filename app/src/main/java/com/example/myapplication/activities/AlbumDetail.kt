package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.Adapters.Album.CancionesAlbumAdapter
import com.example.myapplication.Adapters.Album.CancionesAlbumAdapter.Companion.REQUEST_CREATE_PLAYLIST
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.AddToPlaylistRequest
import com.example.myapplication.io.response.CancionesAlbum
import com.example.myapplication.io.response.DatosAlbumResponse
import com.example.myapplication.io.response.DatosArtista
import com.example.myapplication.io.response.DatosArtistaResponse
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.InvitacionPlaylist
import com.example.myapplication.io.response.MisPlaylist
import com.example.myapplication.io.response.Novedad
import com.example.myapplication.io.response.PlaylistsResponse
import com.example.myapplication.io.response.Seguidor
import com.example.myapplication.services.WebSocketEventHandler
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AlbumDetail : AppCompatActivity() {
    private lateinit var nombreAlbum: TextView
    private lateinit var duracion: TextView
    private lateinit var artista: TextView
    private lateinit var fotoPortada: ImageView
    private lateinit var apiService: ApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var cancionesAdapter: CancionesAlbumAdapter
    private lateinit var dot: View
    private var album:  DatosAlbumResponse? = null
    var selectedCancionParaAñadir: CancionesAlbum? = null
    var albumId: String? = null

    //EVENTOS PARA LAS NOTIFICACIONES
    private val listenerNovedad: (Novedad) -> Unit = {
        runOnUiThread {
            Log.d("LOGS_NOTIS", "evento en home")
            dot.visibility = View.VISIBLE
        }
    }
    private val listenerSeguidor: (Seguidor) -> Unit = {
        runOnUiThread {
            Log.d("LOGS_NOTIS", "evento en home")
            dot.visibility = View.VISIBLE
        }
    }
    private val listenerInvitacion: (InvitacionPlaylist) -> Unit = {
        runOnUiThread {
            Log.d("LOGS_NOTIS", "evento en home")
            dot.visibility = View.VISIBLE
        }
    }
    private val listenerInteraccion: (Interaccion) -> Unit = {
        runOnUiThread {
            Log.d("LOGS_NOTIS", "evento en home")
            dot.visibility = View.VISIBLE
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.album)

        apiService = ApiService.create()

        nombreAlbum = findViewById(R.id.nombreAlbum)
        duracion = findViewById(R.id.duracion)
        artista = findViewById(R.id.artista)
        fotoPortada = findViewById(R.id.imageView)
        recyclerView = findViewById(R.id.cancionesAlbum)
        dot = findViewById<View>(R.id.notificationDot)

        // Obtener el id del album del intent
        val albumId = intent.getStringExtra("id") ?: ""

        cancionesAdapter = CancionesAlbumAdapter(emptyList(), "").apply {
            // Configurar listeners del adaptador
            onAddToPlaylist = { cancion, playlistId ->
                addSongToPlaylist(cancion, playlistId)
            }

            onGetPlaylists = { callback ->
                getUserPlaylists(callback)
            }

        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = cancionesAdapter

        val profileImageButton = findViewById<ImageButton>(R.id.profileImageButton)
        val profileImageUrl = Preferencias.obtenerValorString("fotoPerfil", "")
        if (profileImageUrl.isNullOrEmpty() || profileImageUrl == "DEFAULT") {
            // Cargar la imagen predeterminada
            profileImageButton.setImageResource(R.drawable.ic_profile)
        } else {
            // Cargar la imagen desde la URL con Glide
            Glide.with(this)
                .load(profileImageUrl)
                .placeholder(R.drawable.ic_profile) // Imagen por defecto mientras carga
                .error(R.drawable.ic_profile) // Imagen si hay error
                .circleCrop()
                .into(profileImageButton)
        }

        //PARA EL CIRCULITO ROJO DE NOTIFICACIONES
        if (Preferencias.obtenerValorBooleano("hay_notificaciones",false) == true) {
            dot.visibility = View.VISIBLE
        } else {
            dot.visibility = View.GONE
        }

        //Para actualizar el punto rojo en tiempo real, suscripcion a los eventos
        WebSocketEventHandler.registrarListenerNovedad(listenerNovedad)
        WebSocketEventHandler.registrarListenerSeguidor(listenerSeguidor)
        WebSocketEventHandler.registrarListenerInvitacion(listenerInvitacion)
        WebSocketEventHandler.registrarListenerInteraccion(listenerInteraccion)


        getDatosAlbum(albumId)
        setupNavigation()
    }

    private fun getDatosAlbum(albumId: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.getDatosAlbum(authHeader,albumId).enqueue(object : Callback<DatosAlbumResponse> {
            override fun onResponse(call: Call<DatosAlbumResponse>, response: Response<DatosAlbumResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    album = response.body()

                    // Mostrar la información en los TextViews
                    nombreAlbum.text = album?.nombre
                    artista.text = album?.nombreArtisticoArtista
                    val segundos = album?.duracion.toString().toIntOrNull() ?: 0
                    val minutos = segundos / 60
                    val restoSegundos = segundos % 60
                    duracion.text = "${String.format("%d:%02d", minutos, restoSegundos)} minutos"

                    val canciones = album?.canciones
                    canciones?.let {
                        album?.nombreArtisticoArtista?.let { it1 ->
                            cancionesAdapter.updateData(it,
                                it1
                            )
                        }
                    }

                    // Cargar la imagen usando Glide
                    val foto = album?.fotoPortada
                    if (!foto.isNullOrEmpty()) {
                        Glide.with(this@AlbumDetail)
                            .load(album?.fotoPortada)
                            .placeholder(R.drawable.no_cancion)
                            .error(R.drawable.no_cancion)
                            .into(fotoPortada)
                    }


                } else {
                    Log.d("Album","Error al obtener los datos del artista")
                }
            }

            override fun onFailure(call: Call<DatosAlbumResponse>, t: Throwable) {
                Log.d("Album","Error conexion")
            }
        })
    }

    // Define un callback para devolver las playlists
    private fun getUserPlaylists(callback: (List<MisPlaylist>) -> Unit) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.getMisPlaylists(authHeader).enqueue(object : Callback<PlaylistsResponse> {
            override fun onResponse(call: Call<PlaylistsResponse>, response: Response<PlaylistsResponse>) {
                if (response.isSuccessful) {
                    callback(response.body()?.playlists ?: emptyList())
                } else {
                    showToast("Error al obtener tus playlists")
                    callback(emptyList())
                }
            }

            override fun onFailure(call: Call<PlaylistsResponse>, t: Throwable) {
                showToast("Error de conexión: ${t.message}")
                callback(emptyList())
            }
        })
    }



    private fun addSongToPlaylist(cancion: CancionesAlbum, playlistId: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        val request = AddToPlaylistRequest(cancion.id, playlistId)

        apiService.addSongToPlaylist(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                when {
                    response.isSuccessful -> {
                        showToast("Canción añadida a la playlist")
                    }
                    response.code() == 403 -> {
                        showToast("No tienes permiso para añadir canciones a esta playlist")
                    }
                    response.code() == 404 -> {
                        showToast("La playlist no existe")
                    }
                    response.code() == 409 -> {
                        showToast("Esta canción ya está en la playlist")
                    }
                    else -> {
                        showToast("Error al añadir canción")
                    }
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                showToast("Error de conexión: ${t.message}")
            }
        })
    }

    private fun setupNavigation() {
        val buttonPerfil: ImageButton = findViewById(R.id.profileImageButton)
        val buttonNotis: ImageButton = findViewById(R.id.notificationImageButton)
        val buttonHome: ImageButton = findViewById(R.id.nav_home)
        val buttonSearch: ImageButton = findViewById(R.id.nav_search)
        val buttonCrear: ImageButton = findViewById(R.id.nav_create)

        buttonPerfil.setOnClickListener {
            val esOyente = Preferencias.obtenerValorString("esOyente", "")
            if (esOyente == "oyente") {
                Log.d("Login", "El usuario es un oyente")
                startActivity(Intent(this, Perfil::class.java))
            } else {
                Log.d("Login", "El usuario NO es un oyente")
                startActivity(Intent(this, PerfilArtista::class.java))
            }
        }

        buttonNotis.setOnClickListener {
            startActivity(Intent(this, Notificaciones::class.java))
        }

        buttonHome.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }

        buttonSearch.setOnClickListener {
            startActivity(Intent(this, Buscador::class.java))
        }

        buttonCrear.setOnClickListener {
            startActivity(Intent(this, CrearPlaylist::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CancionesAlbumAdapter.REQUEST_CREATE_PLAYLIST && resultCode == RESULT_OK) {
            val nuevaPlaylistId = data?.getStringExtra("playlist_id")  // Este dato lo tiene que devolver la actividad CrearPlaylist
            val cancion = selectedCancionParaAñadir

            if (nuevaPlaylistId != null && cancion != null) {
                // Llama al método de añadir
                cancionesAdapter.onAddToPlaylist?.invoke(cancion, nuevaPlaylistId)
                Toast.makeText(this, "Canción añadida a la nueva playlist", Toast.LENGTH_SHORT).show()
                selectedCancionParaAñadir = null
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    override fun onDestroy() {
        super.onDestroy()
        WebSocketEventHandler.eliminarListenerNovedad(listenerNovedad)
        WebSocketEventHandler.eliminarListenerSeguidor(listenerSeguidor)
        WebSocketEventHandler.eliminarListenerInvitacion(listenerInvitacion)
        WebSocketEventHandler.eliminarListenerInteraccion(listenerInteraccion)
    }
}