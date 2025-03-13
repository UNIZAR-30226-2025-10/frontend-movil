package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.io.request.DeleteAccountRequest
import com.example.myapplication.io.response.DeleteAccountResponse
import com.example.myapplication.io.response.HistorialRecientesResponse
import com.example.myapplication.io.response.LogOutResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.myapplication.io.response.HistorialEscuchasResponse
import com.example.myapplication.io.response.PlaylistsResponse
import com.example.myapplication.io.response.RecomendacionesResponse
import com.example.myapplication.Adapters.Home.RecientesAdapter
import com.example.myapplication.Adapters.Home.EscuchasAdapter
import com.example.myapplication.Adapters.Home.HeaderAdapter
import com.example.myapplication.Adapters.Home.PlaylistsAdapter
import com.example.myapplication.Adapters.Home.RecomendacionesAdapter

class Home : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var recyclerViewRecientes: RecyclerView
    private lateinit var recyclerViewEscuchas: RecyclerView
    private lateinit var recyclerViewPlaylists: RecyclerView
    private lateinit var recyclerViewRecomendaciones: RecyclerView

    private lateinit var RecientesAdapter: RecientesAdapter
    private lateinit var escuchasAdapter: EscuchasAdapter
    private lateinit var playlistsAdapter: PlaylistsAdapter
    private lateinit var recomendacionesAdapter: RecomendacionesAdapter

    private lateinit var headerRecientesRecyclerView: RecyclerView
    private lateinit var headerEscuchasRecyclerView: RecyclerView
    private lateinit var headerPlaylistsRecyclerView: RecyclerView
    private lateinit var headerRecomendacionesRecyclerView: RecyclerView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_bueno)

        // Inicializar API Service
        apiService = ApiService.create()

        // Configurar RecyclerView para los encabezados
        val headersRecientes = listOf("Escuchado recientemente")
        val headerRecientesAdapter = HeaderAdapter(headersRecientes)
        headerRecientesRecyclerView = findViewById(R.id.recyclerViewHeadersRecientes)
        headerRecientesRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        headerRecientesRecyclerView.adapter = headerRecientesAdapter
        headerRecientesRecyclerView.visibility = View.INVISIBLE

        val headersEscuchas = listOf("Úiltimas escuchas")
        val headerEscuchasAdapter = HeaderAdapter(headersEscuchas)
        headerEscuchasRecyclerView = findViewById(R.id.recyclerViewHeadersEscuchas)
        headerEscuchasRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        headerEscuchasRecyclerView.adapter = headerEscuchasAdapter
        headerEscuchasRecyclerView.visibility = View.INVISIBLE


        val headersPlaylists = listOf("Mis playlists")
        val headerPlaylistsAdapter = HeaderAdapter(headersPlaylists)
        headerPlaylistsRecyclerView = findViewById(R.id.recyclerViewHeadersPlaylists)
        headerPlaylistsRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        headerPlaylistsRecyclerView.adapter = headerPlaylistsAdapter
        headerPlaylistsRecyclerView.visibility = View.INVISIBLE

        val headersRecomendaciones = listOf("Recomendaciones")
        val headerRecomendacionesAdapter = HeaderAdapter(headersRecomendaciones)
        headerRecomendacionesRecyclerView = findViewById(R.id.recyclerViewHeadersRecomendaciones)
        headerRecomendacionesRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        headerRecomendacionesRecyclerView.adapter = headerRecomendacionesAdapter
        headerRecomendacionesRecyclerView.visibility = View.INVISIBLE

        //Inicializar los recyclerView
        recyclerViewRecientes = findViewById(R.id.recyclerViewRecientes)
        recyclerViewRecientes.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerViewEscuchas = findViewById(R.id.recyclerViewEscuchas)
        recyclerViewEscuchas.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerViewPlaylists = findViewById(R.id.recyclerViewMisPlaylists)
        recyclerViewPlaylists.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerViewRecomendaciones = findViewById(R.id.recyclerViewRecomendaciones)
        recyclerViewRecomendaciones.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Inicia los adaptadores
        RecientesAdapter = RecientesAdapter(mutableListOf())
        recyclerViewRecientes.adapter = RecientesAdapter

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
        getHistorialRecientes()
        getHistorialEscuchas()
        getMisPlaylists()
        getRecomendaciones()
    }

    private fun getHistorialRecientes() {
        val token = Preferencias.obtenerValorString("token", "")
        apiService.getHistorialRecientes("Bearer $token").enqueue(object : Callback<HistorialRecientesResponse> {
            override fun onResponse(call: Call<HistorialRecientesResponse>, response: Response<HistorialRecientesResponse>) {
                Log.d("Mi app", "entra en on response Recientes")
                if (response.isSuccessful) {
                    Log.d("Mi app", "entra en on response succesful Recientes")
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            Log.d("Mi app", "entra en respuesta http Recientes")
                            val Recientes = it.historial_colecciones
                            Log.d("Mi app", "recientes = $Recientes")

                            // Actualizar y mostrar las canciones si las hay
                            if (Recientes.isNotEmpty()) {
                                Log.d("Mi app", "no esta vacía")
                                RecientesAdapter.updateDataReciente(Recientes)
                                recyclerViewRecientes.visibility = View.VISIBLE
                                headerRecientesRecyclerView.visibility = View.VISIBLE
                            } else {
                                Log.d("Mi app", "no hay recientes")
                                recyclerViewRecientes.visibility = View.GONE
                                headerRecientesRecyclerView.visibility = View.GONE
                                showToast("No hay Recientes")
                            }

                        } else {
                            handleErrorCode(it.respuestaHTTP)
                        }
                    } ?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    showToast("Error en la búsqueda: Código ${response.code()}")
                }
                Log.d("Mi app", "sale de recientes")
            }

            override fun onFailure(call: Call<HistorialRecientesResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun getHistorialEscuchas() {
        val token = Preferencias.obtenerValorString("token", "")
        apiService.getHistorialEscuchas("Bearer $token").enqueue(object : Callback<HistorialEscuchasResponse> {
            override fun onResponse(call: Call<HistorialEscuchasResponse>, response: Response<HistorialEscuchasResponse>) {
                Log.d("Mi app", "entra en on response Escuchas")
                if (response.isSuccessful) {
                    Log.d("Mi app", "entra en on response succesful Escuchas")
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            Log.d("Mi app", "entra en respuesta http escuchas = $it")
                            val escuchas = it.historial_canciones
                            Log.d("Mi app", "recientes = $escuchas")

                            // Actualizar y mostrar las canciones si las hay
                            if (escuchas.isNotEmpty()) {
                                escuchasAdapter.updateDataEscucha(escuchas)
                                recyclerViewEscuchas.visibility = View.VISIBLE
                                headerEscuchasRecyclerView.visibility = View.VISIBLE
                            } else {
                                recyclerViewEscuchas.visibility = View.GONE
                                headerEscuchasRecyclerView.visibility = View.GONE
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
                Log.d("Mi app", "entra en on response Playlists")
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            val misPlaylists = it.playlists

                            // Actualizar y mostrar las canciones si las hay
                            if (misPlaylists.isNotEmpty()) {
                                playlistsAdapter.updateDataMisPlaylists(misPlaylists)
                                recyclerViewPlaylists.visibility = View.VISIBLE
                                headerPlaylistsRecyclerView.visibility = View.VISIBLE
                            } else {
                                recyclerViewPlaylists.visibility = View.GONE
                                headerPlaylistsRecyclerView.visibility = View.GONE
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
                Log.d("Mi app", "entra en on response Recomendaciones")
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            val recomendaciones = it.canciones_recomendadas

                            // Actualizar y mostrar las canciones si las hay
                            if (recomendaciones.isNotEmpty()) {
                                recomendacionesAdapter.updateDataRecomendacion(recomendaciones)
                                recyclerViewRecomendaciones.visibility = View.VISIBLE
                                headerRecomendacionesRecyclerView.visibility = View.VISIBLE
                            } else {
                                recyclerViewRecomendaciones.visibility = View.GONE
                                headerRecomendacionesRecyclerView.visibility = View.GONE
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


