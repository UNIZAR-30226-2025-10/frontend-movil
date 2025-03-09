package com.example.myapplication.activities

import HeaderAdapter
import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.buscador)

        apiService = ApiService.create()

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
        cancionAdapter = CancionAdapter(mutableListOf())
        recyclerViewCancion.adapter = cancionAdapter

        artistaAdapter = ArtistaAdapter(mutableListOf())
        recyclerViewArtista.adapter = artistaAdapter

        albumAdapter = AlbumAdapter(mutableListOf())
        recyclerViewAlbum.adapter = albumAdapter

        playlistAdapter = PlaylistAdapter(mutableListOf())
        recyclerViewPlaylist.adapter = playlistAdapter

        perfilAdapter = PerfilAdapter(mutableListOf())
        recyclerViewPerfil.adapter = perfilAdapter

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                val termino = charSequence.toString().trim()
                if (termino.isNotEmpty()) search(termino)
            }
            override fun afterTextChanged(editable: Editable?) {}
        })
    }

    private fun search(termino: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.searchBuscador(authHeader, termino).enqueue(object : Callback<BuscadorResponse> {
            override fun onResponse(call: Call<BuscadorResponse>, response: Response<BuscadorResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            val canciones = it.canciones
                            val artistas = it.artistas
                            val albumes = it.albumes
                            val playlist = it.playlists
                            val perfil = it.perfiles

                            // Actualizar y mostrar las canciones si las hay
                            if (canciones.isNotEmpty()) {
                                cancionAdapter.updateData(canciones)
                                recyclerViewCancion.visibility = View.VISIBLE
                                headerCancionesRecyclerView.visibility = View.VISIBLE
                            } else {
                                recyclerViewPlaylist.visibility = View.GONE
                                headerPlaylistsRecyclerView.visibility = View.GONE
                                showToast("No se encontraron canciones")
                            }

                            // Actualizar y mostrar los artistas si los hay
                            if (artistas.isNotEmpty()) {
                                artistaAdapter.updateDataArtista(artistas)
                                recyclerViewArtista.visibility = View.VISIBLE
                                headerArtistasRecyclerView.visibility = View.VISIBLE
                            } else {
                                recyclerViewPlaylist.visibility = View.GONE
                                headerPlaylistsRecyclerView.visibility = View.GONE
                                showToast("No se encontraron artistas")
                            }

                            // Actualizar y mostrar los álbumes si los hay
                            if (albumes.isNotEmpty()) {
                                albumAdapter.updateDataAlbum(albumes)
                                recyclerViewAlbum.visibility = View.VISIBLE
                                headerAlbumesRecyclerView.visibility = View.VISIBLE
                            } else {
                                recyclerViewPlaylist.visibility = View.GONE
                                headerPlaylistsRecyclerView.visibility = View.GONE
                                showToast("No se encontraron albumes")
                            }

                            // Actualizar y mostrar las playlists si las hay
                            if (playlist.isNotEmpty()) {
                                playlistAdapter.updateDataPlaylists(playlist)
                                recyclerViewPlaylist.visibility = View.VISIBLE
                                headerPlaylistsRecyclerView.visibility = View.VISIBLE
                            } else {
                                recyclerViewPlaylist.visibility = View.GONE
                                headerPlaylistsRecyclerView.visibility = View.GONE
                                showToast("No se encontraron playlists")
                            }

                            // Mostrar los perfiles solo si hay perfiles
                            if (perfil.isNotEmpty()) {
                                perfilAdapter.updateDataPerfiles(perfil)
                                recyclerViewPerfil.visibility = View.VISIBLE
                                headerPerfilesRecyclerView.visibility = View.VISIBLE

                                showToast("Se encontraron ${perfil.size} perfiles")
                            } else {
                                recyclerViewPerfil.visibility = View.GONE
                                headerPerfilesRecyclerView.visibility = View.GONE

                                showToast("No se encontraron perfiles")
                            }
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
