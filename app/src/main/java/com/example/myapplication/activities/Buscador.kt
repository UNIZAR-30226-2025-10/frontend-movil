package com.example.myapplication.activities

import HeaderAdapter
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.Adapters.Buscador.AlbumAdapter
import com.example.myapplication.Adapters.Buscador.ArtistaAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.BuscadorResponse
import com.example.myapplication.utils.Preferencias
import com.example.myapplication.Adapters.Buscador.CancionAdapter
import com.example.myapplication.Adapters.Buscador.PerfilAdapter
import com.example.myapplication.Adapters.Buscador.PlaylistAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Buscador : AppCompatActivity() {
    private lateinit var searchEditText: EditText
    private lateinit var recyclerViewCancion: RecyclerView
    private lateinit var recyclerViewArtista: RecyclerView
    private lateinit var recyclerViewAlbum: RecyclerView
    private lateinit var recyclerViewPlaylist: RecyclerView
    private lateinit var recyclerViewPerfil: RecyclerView
    private lateinit var headerCancionesRecyclerView: RecyclerView
    private lateinit var headerArtistasRecyclerView: RecyclerView
    private lateinit var headerAlbumesRecyclerView: RecyclerView
    private lateinit var headerPlaylistsRecyclerView: RecyclerView
    private lateinit var headerPerfilesRecyclerView: RecyclerView
    private lateinit var cancionAdapter: CancionAdapter
    private lateinit var artistaAdapter: ArtistaAdapter
    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var playlistAdapter: PlaylistAdapter
    private lateinit var perfilAdapter: PerfilAdapter
    private lateinit var apiService: ApiService
    private lateinit var checkCanciones: CheckBox
    private lateinit var checkArtistas: CheckBox
    private lateinit var checkAlbumes: CheckBox
    private lateinit var checkPlaylists: CheckBox
    private lateinit var checkPerfiles: CheckBox

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.buscador)

        apiService = ApiService.create()

        val profileImageButton = findViewById<ImageButton>(R.id.profileImageButton)

        // Obtener la URL de la imagen de perfil desde SharedPreferences
        val profileImageUrl = Preferencias.obtenerValorString("fotoPerfil", "")

        Log.d("ProfileImage", "URL de la imagen de perfil: $profileImageUrl")


        // Verificar si la API devolvió "DEFAULT" o si no hay imagen guardada
        if (profileImageUrl.isNullOrEmpty() || profileImageUrl == "DEFAULT") {
            // Cargar la imagen predeterminada
            profileImageButton.setImageResource(R.drawable.ic_profile)
        } else {
            // Cargar la imagen desde la URL con Glide
            Glide.with(this)
                .load(profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile) // Imagen por defecto mientras carga
                .error(R.drawable.ic_profile) // Imagen si hay error
                .into(profileImageButton)
        }

        // Configurar RecyclerView para los encabezados
        val headersCanciones = listOf("Canciones")
        val headerCancionesAdapter = HeaderAdapter(headersCanciones)
        headerCancionesRecyclerView = findViewById(R.id.recyclerViewHeadersCanciones)
        headerCancionesRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        headerCancionesRecyclerView.adapter = headerCancionesAdapter
        headerCancionesRecyclerView.visibility = View.INVISIBLE

        val headersAlbumes = listOf("Álbumes")
        val headeAlbumesAdapter = HeaderAdapter(headersAlbumes)
        headerAlbumesRecyclerView = findViewById(R.id.recyclerViewHeadersÁlbumes)
        headerAlbumesRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        headerAlbumesRecyclerView.adapter = headeAlbumesAdapter
        headerAlbumesRecyclerView.visibility = View.INVISIBLE

        val headersArtistas = listOf("Artistas")
        val headerArtistasAdapter = HeaderAdapter(headersArtistas)
        headerArtistasRecyclerView = findViewById(R.id.recyclerViewHeadersArtistas)
        headerArtistasRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        headerArtistasRecyclerView.adapter = headerArtistasAdapter
        headerArtistasRecyclerView.visibility = View.INVISIBLE

        val headersPlaylists = listOf("Playlists")
        val headerPlaylistsAdapter = HeaderAdapter(headersPlaylists)
        headerPlaylistsRecyclerView = findViewById(R.id.recyclerViewHeadersPlaylists)
        headerPlaylistsRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        headerPlaylistsRecyclerView.adapter = headerPlaylistsAdapter
        headerPlaylistsRecyclerView.visibility = View.INVISIBLE

        val headersPerfiles = listOf("Perfiles")
        val headerPerfilesAdapter = HeaderAdapter(headersPerfiles)
        headerPerfilesRecyclerView = findViewById(R.id.recyclerViewHeadersPerfiles)
        headerPerfilesRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        headerPerfilesRecyclerView.adapter = headerPerfilesAdapter
        headerPerfilesRecyclerView.visibility = View.INVISIBLE

        searchEditText = findViewById(R.id.searchInput)
        recyclerViewCancion = findViewById(R.id.recyclerViewCanciones)
        recyclerViewCancion.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerViewArtista = findViewById(R.id.recyclerViewArtistas)
        recyclerViewArtista.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerViewAlbum = findViewById(R.id.recyclerViewAlbumes)
        recyclerViewAlbum.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerViewPlaylist = findViewById(R.id.recyclerViewPlaylists)
        recyclerViewPlaylist.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerViewPerfil = findViewById(R.id.recyclerViewPerfiles)
        recyclerViewPerfil.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Inicia los adaptadores
        cancionAdapter = CancionAdapter(mutableListOf()) { cancion ->
            val intent = Intent(this, CancionDetail::class.java)
            intent.putExtra("nombre", cancion.nombre)
            intent.putExtra("artista", cancion.nombreArtisticoArtista)
            intent.putExtra("imagen", cancion.fotoPortada)
            intent.putExtra("id", cancion.id)
            startActivity(intent)
        }
        recyclerViewCancion.adapter = cancionAdapter

        artistaAdapter = ArtistaAdapter(mutableListOf())
        recyclerViewArtista.adapter = artistaAdapter

        albumAdapter = AlbumAdapter(mutableListOf())
        recyclerViewAlbum.adapter = albumAdapter

        playlistAdapter = PlaylistAdapter(mutableListOf()) { playlist ->
            val intent = Intent(this, PlaylistDetail::class.java)
            intent.putExtra("nombre", playlist.nombre)
            intent.putExtra("usuario", playlist.nombreUsuarioCreador)
            intent.putExtra("imagen", playlist.fotoPortada)
            intent.putExtra("id", playlist.id)
            Log.d("Playlist", "Buscador -> Playlist")
            startActivity(intent)
        }
        recyclerViewPlaylist.adapter = playlistAdapter

        perfilAdapter = PerfilAdapter(mutableListOf()){perfil ->
            val intent = Intent(this, OtroOyente::class.java)
            intent.putExtra("nombre", perfil.nombreUsuario)
            intent.putExtra("imagen", perfil.fotoPerfil)
            Log.d("Perfil", "Buscador -> Perfil")
            startActivity(intent)
        }
        recyclerViewPerfil.adapter = perfilAdapter

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                val termino = charSequence.toString().trim()
                if (termino.isNotEmpty()) search(termino)
            }
            override fun afterTextChanged(editable: Editable?) {}
        })

        checkCanciones = findViewById(R.id.checkCanciones)
        checkArtistas = findViewById(R.id.checkArtistas)
        checkAlbumes = findViewById(R.id.checkAlbumes)
        checkPlaylists = findViewById(R.id.checkPlaylists)
        checkPerfiles = findViewById(R.id.checkPerfiles)

        setupNavigation()

        // Configurar eventos de cambio en los CheckBox para actualizar la vista
        val checkBoxes = listOf(checkCanciones, checkArtistas, checkAlbumes, checkPlaylists, checkPerfiles)
        for (checkBox in checkBoxes) {
            checkBox.setOnCheckedChangeListener { _, _ ->
                aplicarFiltros()
            }
        }
    }


    private fun search(termino: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.searchBuscador(authHeader, termino).enqueue(object : Callback<BuscadorResponse> {
            override fun onResponse(call: Call<BuscadorResponse>, response: Response<BuscadorResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            cancionAdapter.updateData(it.canciones)
                            artistaAdapter.updateDataArtista(it.artistas)
                            albumAdapter.updateDataAlbum(it.albumes)
                            playlistAdapter.updateDataPlaylists(it.playlists)
                            perfilAdapter.updateDataPerfiles(it.perfiles)
                            aplicarFiltros()
                        } else {
                            handleErrorCode(it.respuestaHTTP)
                        }
                    } ?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    showToast("Error en la búsqueda: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<BuscadorResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun aplicarFiltros() {
        val todosDesmarcados = !(checkCanciones.isChecked || checkArtistas.isChecked ||
                checkAlbumes.isChecked || checkPlaylists.isChecked || checkPerfiles.isChecked)

        // Si todos están desmarcados, mostrar todas las categorías
        recyclerViewCancion.visibility = if (todosDesmarcados || checkCanciones.isChecked) View.VISIBLE else View.GONE
        headerCancionesRecyclerView.visibility = if (todosDesmarcados || checkCanciones.isChecked) View.VISIBLE else View.GONE

        recyclerViewArtista.visibility = if (todosDesmarcados || checkArtistas.isChecked) View.VISIBLE else View.GONE
        headerArtistasRecyclerView.visibility = if (todosDesmarcados || checkArtistas.isChecked) View.VISIBLE else View.GONE

        recyclerViewAlbum.visibility = if (todosDesmarcados || checkAlbumes.isChecked) View.VISIBLE else View.GONE
        headerAlbumesRecyclerView.visibility = if (todosDesmarcados || checkAlbumes.isChecked) View.VISIBLE else View.GONE

        recyclerViewPlaylist.visibility = if (todosDesmarcados || checkPlaylists.isChecked) View.VISIBLE else View.GONE
        headerPlaylistsRecyclerView.visibility = if (todosDesmarcados || checkPlaylists.isChecked) View.VISIBLE else View.GONE

        recyclerViewPerfil.visibility = if (todosDesmarcados || checkPerfiles.isChecked) View.VISIBLE else View.GONE
        headerPerfilesRecyclerView.visibility = if (todosDesmarcados || checkPerfiles.isChecked) View.VISIBLE else View.GONE
    }

    private fun setupNavigation() {
        val buttonPerfil: ImageButton = findViewById(R.id.profileImageButton)
        val buttonHome: ImageButton = findViewById(R.id.nav_home)
        val buttonSearch: ImageButton = findViewById(R.id.nav_search)
        val buttonCrear: ImageButton = findViewById(R.id.nav_create)

        buttonPerfil.setOnClickListener {
            startActivity(Intent(this, Perfil::class.java))
        }

        buttonHome.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }

        buttonSearch.setOnClickListener {
            startActivity(Intent(this, Buscador::class.java))
        }

        buttonCrear.setOnClickListener {
            startActivity(Intent(this, Perfil::class.java))
        }
    }

    private fun handleErrorCode(statusCode: Int) {
        val message = when (statusCode) {
            400 -> "Error: Correo o usuario en uso"
            500 -> "Error interno del servidor"
            else -> "Error desconocido ($statusCode)"
        }
        showToast(message)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}