package com.example.myapplication.activities

import Buscador
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.utils.Preferencias
import com.example.myapplication.io.ApiService
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.io.request.DeleteAccountRequest
import com.example.myapplication.io.response.DeleteAccountResponse
import com.example.myapplication.io.response.HistorialArtistasResponse
import com.example.myapplication.io.response.LogOutResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.myapplication.io.response.HistorialEscuchasResponse
import com.example.myapplication.io.response.PlaylistsResponse
import com.example.myapplication.io.response.RecomendacionesResponse
import com.example.myapplication.Adapters.Home.ArtistasAdapter
import com.example.myapplication.Adapters.Home.EscuchasAdapter
import com.example.myapplication.Adapters.Home.PlaylistsAdapter
import com.example.myapplication.Adapters.Home.RecomendacionesAdapter

class Home : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var recyclerViewArtistas: RecyclerView
    private lateinit var recyclerViewEscuchas: RecyclerView
    private lateinit var recyclerViewPlaylists: RecyclerView
    private lateinit var recyclerViewRecomendaciones: RecyclerView
    private lateinit var artistasAdapter: ArtistasAdapter
    private lateinit var escuchasAdapter: EscuchasAdapter
    private lateinit var playlistsAdapter: PlaylistsAdapter
    private lateinit var recomendacionesAdapter: RecomendacionesAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_bueno)

        // Inicializar API Service
        apiService = ApiService.create()


        //Inicializar los recyclerView
        recyclerViewArtistas = findViewById(R.id.recyclerViewArtistasH)
        recyclerViewArtistas.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerViewEscuchas = findViewById(R.id.recyclerViewEscuchas)
        recyclerViewEscuchas.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerViewPlaylists = findViewById(R.id.recyclerViewMisPlaylists)
        recyclerViewPlaylists.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerViewRecomendaciones = findViewById(R.id.recyclerViewRecomendaciones)
        recyclerViewRecomendaciones.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Inicia los adaptadores
        artistasAdapter = ArtistasAdapter(mutableListOf())
        recyclerViewArtistas.adapter = artistasAdapter

        escuchasAdapter = EscuchasAdapter(mutableListOf())
        recyclerViewEscuchas.adapter = escuchasAdapter

        playlistsAdapter = PlaylistsAdapter(mutableListOf())
        recyclerViewPlaylists.adapter = playlistsAdapter

        recomendacionesAdapter = RecomendacionesAdapter(mutableListOf())
        recyclerViewRecomendaciones.adapter = recomendacionesAdapter

        // Cargar datos al iniciar
        loadHomeData()

        // Configurar botones de navegación
        setupNavigation()
    }

    private fun loadHomeData() {
        getHistorialartistas()
        getHistorialEscuchas()
        getMisPlaylists()
        getRecomendaciones()
    }

    private fun getHistorialartistas() {
        val token = Preferencias.obtenerValorString("token", "")
        apiService.getHistorialArtistas("Bearer $token").enqueue(object : Callback<HistorialArtistasResponse> {
            override fun onResponse(call: Call<HistorialArtistasResponse>, response: Response<HistorialArtistasResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            val artistas = it.historial_artistas

                            // Actualizar y mostrar las canciones si las hay
                            if (artistas.isNotEmpty()) {
                                artistasAdapter.updateDataArtista(artistas)
                                recyclerViewArtistas.visibility = View.VISIBLE
                            } else {
                                recyclerViewArtistas.visibility = View.GONE
                                showToast("No hay artistas")
                            }

                        } else {
                            handleErrorCode(it.respuestaHTTP)
                        }
                    } ?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    showToast("Error en la búsqueda: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<HistorialArtistasResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun getHistorialEscuchas() {
        val token = Preferencias.obtenerValorString("token", "")
        apiService.getHistorialEscuchas("Bearer $token").enqueue(object : Callback<HistorialEscuchasResponse> {
            override fun onResponse(call: Call<HistorialEscuchasResponse>, response: Response<HistorialEscuchasResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            val escuchas = it.historial_escuchas

                            // Actualizar y mostrar las canciones si las hay
                            if (escuchas.isNotEmpty()) {
                                escuchasAdapter.updateDataEscucha(escuchas)
                                recyclerViewEscuchas.visibility = View.VISIBLE
                            } else {
                                recyclerViewEscuchas.visibility = View.GONE
                                showToast("No hay escuchas")
                            }

                        } else {
                            handleErrorCode(it.respuestaHTTP)
                        }
                    } ?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    showToast("Error en la búsqueda: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<HistorialEscuchasResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }



    private fun getMisPlaylists() {
        val token = Preferencias.obtenerValorString("token", "")
        apiService.getMisPlaylists("Bearer $token").enqueue(object : Callback<PlaylistsResponse> {
            override fun onResponse(call: Call<PlaylistsResponse>, response: Response<PlaylistsResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            val misPlaylists = it.mis_playlists

                            // Actualizar y mostrar las canciones si las hay
                            if (misPlaylists.isNotEmpty()) {
                                playlistsAdapter.updateDataMisPlaylists(misPlaylists)
                                recyclerViewPlaylists.visibility = View.VISIBLE
                            } else {
                                recyclerViewPlaylists.visibility = View.GONE
                                showToast("No hay playlists")
                            }

                        } else {
                            handleErrorCode(it.respuestaHTTP)
                        }
                    } ?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    showToast("Error en la búsqueda: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<PlaylistsResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun getRecomendaciones() {
        val token = Preferencias.obtenerValorString("token", "")
        apiService.getRecomendaciones("Bearer $token").enqueue(object : Callback<RecomendacionesResponse> {
            override fun onResponse(call: Call<RecomendacionesResponse>, response: Response<RecomendacionesResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            val recomendaciones = it.canciones_recomendadas

                            // Actualizar y mostrar las canciones si las hay
                            if (recomendaciones.isNotEmpty()) {
                                recomendacionesAdapter.updateDataRecomendacion(recomendaciones)
                                recyclerViewRecomendaciones.visibility = View.VISIBLE
                            } else {
                                recyclerViewRecomendaciones.visibility = View.GONE
                                showToast("No hay recomendaciones")
                            }

                        } else {
                            handleErrorCode(it.respuestaHTTP)
                        }
                    } ?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    showToast("Error en la búsqueda: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<RecomendacionesResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
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


