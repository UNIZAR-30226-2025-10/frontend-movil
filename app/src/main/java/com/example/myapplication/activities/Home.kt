package com.example.myapplication.activities


import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.utils.Preferencias
import com.example.myapplication.io.ApiService
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
import com.example.myapplication.Adapters.Home.RecientesYArtistasAdapter
import com.example.myapplication.Adapters.Home.EscuchasAdapter
import com.example.myapplication.Adapters.Home.HeaderAdapter
import com.example.myapplication.Adapters.Home.PlaylistsAdapter
import com.example.myapplication.Adapters.Home.RecomendacionesAdapter
import com.example.myapplication.io.response.HArtistas
import com.example.myapplication.io.response.HRecientes
import com.example.myapplication.io.response.HistorialArtistasResponse
import com.example.myapplication.services.MusicPlayerService


class Home : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var recyclerViewRecientes: RecyclerView
    private lateinit var recyclerViewEscuchas: RecyclerView
    private lateinit var recyclerViewPlaylists: RecyclerView
    private lateinit var recyclerViewRecomendaciones: RecyclerView

    private lateinit var RecientesAdapter: RecientesYArtistasAdapter
    private lateinit var escuchasAdapter: EscuchasAdapter
    private lateinit var playlistsAdapter: PlaylistsAdapter
    private lateinit var recomendacionesAdapter: RecomendacionesAdapter
    /*
    private lateinit var headerRecientesTextView: RecyclerView
    private lateinit var headerEscuchasTextView: RecyclerView
    private lateinit var headerPlaylistsTextView: RecyclerView
    private lateinit var headerRecomendacionesTextView: RecyclerView
    */

    private lateinit var headerRecientesTextView: TextView
    private lateinit var headerEscuchasTextView: TextView
    private lateinit var headerPlaylistsTextView: TextView
    private lateinit var headerRecomendacionesTextView: TextView

    private val listaRecientes = mutableListOf<HRecientes>()
    private val listaArtistas = mutableListOf<HArtistas>()

    private lateinit var progressBar: ProgressBar
    private var musicService: MusicPlayerService? = null
    private var serviceBound = false
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateProgressBar()
            handler.postDelayed(this, 1000) // cada segundo
        }
    }
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.MusicBinder
            musicService = binder.getService()
            serviceBound = true
            handler.post(updateRunnable) // Empieza a actualizar
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
            handler.removeCallbacks(updateRunnable)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_bueno)

        // Inicializar API Service
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
        
        /*
        // Configurar RecyclerView para los encabezados
        val headersRecientes = listOf("Escuchado recientemente")
        val headerRecientesAdapter = HeaderAdapter(headersRecientes)
        headerRecientesTextView = findViewById(R.id.recyclerViewHeadersRecientes)
        headerRecientesTextView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        headerRecientesTextView.adapter = headerRecientesAdapter
        headerRecientesTextView.visibility = View.INVISIBLE

        val headersEscuchas = listOf("Úiltimas escuchas")
        val headerEscuchasAdapter = HeaderAdapter(headersEscuchas)
        headerEscuchasTextView = findViewById(R.id.recyclerViewHeadersEscuchas)
        headerEscuchasTextView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        headerEscuchasTextView.adapter = headerEscuchasAdapter
        headerEscuchasTextView.visibility = View.INVISIBLE


        val headersPlaylists = listOf("Mis playlists")
        val headerPlaylistsAdapter = HeaderAdapter(headersPlaylists)
        headerPlaylistsTextView = findViewById(R.id.recyclerViewHeadersPlaylists)
        headerPlaylistsTextView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        headerPlaylistsTextView.adapter = headerPlaylistsAdapter
        headerPlaylistsTextView.visibility = View.INVISIBLE

        val headersRecomendaciones = listOf("Recomendaciones")
        val headerRecomendacionesAdapter = HeaderAdapter(headersRecomendaciones)
        headerRecomendacionesTextView = findViewById(R.id.recyclerViewHeadersRecomendaciones)
        headerRecomendacionesTextView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        headerRecomendacionesTextView.adapter = headerRecomendacionesAdapter
        headerRecomendacionesTextView.visibility = View.INVISIBLE
        */

        // Configurar TextView para los encabezados
        headerRecientesTextView = findViewById(R.id.textViewHeadersRecientes)
        headerRecientesTextView.visibility = View.INVISIBLE
        
        headerEscuchasTextView = findViewById(R.id.textViewHeadersEscuchas)
        headerEscuchasTextView.visibility = View.INVISIBLE
        
        headerPlaylistsTextView = findViewById(R.id.textViewHeadersPlaylists)
        headerPlaylistsTextView.visibility = View.INVISIBLE
        
        headerRecomendacionesTextView = findViewById(R.id.textViewHeadersRecomendaciones)
        headerRecomendacionesTextView.visibility = View.INVISIBLE

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
        RecientesAdapter = RecientesYArtistasAdapter(mutableListOf())
        recyclerViewRecientes.adapter = RecientesAdapter

        escuchasAdapter = EscuchasAdapter(mutableListOf()) { escucha ->
            val intent = Intent(this, CancionDetail::class.java)
            intent.putExtra("nombre", escucha.nombre)
            intent.putExtra("imagen", escucha.fotoPortada)
            intent.putExtra("id", escucha.id)
            Log.d("Escuchaas", "Home ->Escucha")
            startActivity(intent)
        }
        recyclerViewEscuchas.adapter = escuchasAdapter

        playlistsAdapter = PlaylistsAdapter(mutableListOf()){ playlist ->
            val intent = Intent(this, PlaylistDetail::class.java)
            intent.putExtra("nombre", playlist.nombre)
            intent.putExtra("imagen", playlist.fotoPortada)
            intent.putExtra("id", playlist.id)
            Log.d("Playlist", "Home ->Playlist")
            startActivity(intent)
        }
        recyclerViewPlaylists.adapter = playlistsAdapter

        recomendacionesAdapter = RecomendacionesAdapter(mutableListOf())
        recyclerViewRecomendaciones.adapter = recomendacionesAdapter

        // Cargar datos al iniciar
        loadHomeData()

        // Actualizar la información del mini reproductor
        updateMiniPlayer()

        // Configurar botones de navegación
        setupNavigation()
    }

    private fun updateMiniPlayer() {
        val songImage = findViewById<ImageView>(R.id.songImage)
        val songTitle = findViewById<TextView>(R.id.songTitle)
        val songArtist = findViewById<TextView>(R.id.songArtist)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val stopButton = findViewById<ImageButton>(R.id.stopButton)

        // Obtener la información del mini reproductor desde SharedPreferences
        val songImageUrl = Preferencias.obtenerValorString("fotoPortadaActual", "")
        val songTitleText = Preferencias.obtenerValorString("nombreCancionActual", "Nombre de la canción")
        val songArtistText = Preferencias.obtenerValorString("nombreArtisticoActual", "Artista")
        val songProgress = Preferencias.obtenerValorEntero("progresoCancionActual", 0)

        // Cargar la imagen de la canción
        if (songImageUrl.isNullOrEmpty()) {
            songImage.setImageResource(R.drawable.ic_default_song)
        } else {
            Glide.with(this)
                .load(songImageUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_default_song)
                .error(R.drawable.ic_default_song)
                .into(songImage)
        }

        // Actualizar título y artista
        songTitle.text = songTitleText
        songArtist.text = songArtistText

        // Actualizar barra de progreso
        progressBar.progress = songProgress

        // Configurar botón de stop (detener)
        stopButton.setOnClickListener {
            // Lógica para detener la canción
            Log.d("MiniPlayer", "Canción detenida")
            Preferencias.guardarValorEntero("songProgress", 0)
            progressBar.progress = 0
        }
    }

    private fun updateProgressBar() {
        musicService?.let { service ->
            if (service.isPlaying()) {
                val current = service.getProgress()
                val duration = service.getDuration()

                if (duration > 0) {
                    val progress = (current * 100) / duration
                    progressBar.progress = progress
                }
            }
        }
    }


    override fun onStart() {
        super.onStart()
        val intent = Intent(this, MusicPlayerService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    private fun loadHomeData() {
        getRecientes()
        Log.d("MiApp", "ha hecho recientes")
        getHistorialEscuchas()
        getMisPlaylists()
        getRecomendaciones()
    }

    private fun getRecientes() {
        getHistorialRecientes()
        getHistorialArtistas()
    }

    private fun getHistorialRecientes() {
        val token = Preferencias.obtenerValorString("token", "")
        apiService.getHistorialRecientes("Bearer $token").enqueue(object : Callback<HistorialRecientesResponse> {
            override fun onResponse(call: Call<HistorialRecientesResponse>, response: Response<HistorialRecientesResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            Log.d("MiApp", "entra en respuesta http recientes = $it")
                            listaRecientes.clear()
                            listaRecientes.addAll(it.historial_colecciones)
                            Log.d("MiApp", "ha asignado la lista bien recientes")
                            verificarDatosCompletos()
                        } else {
                            handleErrorCode(it.respuestaHTTP)
                        }
                    } ?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    showToast("Error en la búsqueda: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<HistorialRecientesResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun getHistorialArtistas() {
        val token = Preferencias.obtenerValorString("token", "")
        apiService.getHistorialArtistas("Bearer $token").enqueue(object : Callback<HistorialArtistasResponse> {
            override fun onResponse(call: Call<HistorialArtistasResponse>, response: Response<HistorialArtistasResponse>) {
                Log.d("MiApp", "entra en on response Artistas")
                if (response.isSuccessful) {
                    Log.d("MiApp", "entra en on response succesful Artistas")
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            Log.d("MiApp", "entra en respuesta http artistas = $it")
                            listaArtistas.clear()
                            listaArtistas.addAll(it.historial_artistas)
                            Log.d("MiApp", "ha asignado la lista bien")
                            verificarDatosCompletos()
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

    private fun verificarDatosCompletos() {
        Log.d("MiApp", "entra en verificar datos")
        if (listaRecientes.isNotEmpty() && listaArtistas.isNotEmpty()) {
            Log.d("MiApp", "listas no vacias")
            mezclarArtistasyRecientes()
        }
    }

    private fun mezclarArtistasyRecientes() {
        val listaMezclada = mutableListOf<Any>()
        var i = 0
        var j = 0

        while (i < listaRecientes.size || j < listaArtistas.size) {
            repeat(3) { if (i < listaRecientes.size) listaMezclada.add(listaRecientes[i++]) }
            repeat(3) { if (j < listaArtistas.size) listaMezclada.add(listaArtistas[j++]) }
            if (i < listaRecientes.size) listaMezclada.add(listaRecientes[i++])
            if (j < listaArtistas.size) listaMezclada.add(listaArtistas[j++])
        }
        Log.d("MiApp", "mezcla listas y las manda a actualizar")
        RecientesAdapter.updateData(listaMezclada)
        recyclerViewRecientes.visibility = View.VISIBLE
        headerRecientesTextView.visibility = View.VISIBLE
        Log.d("MiApp", "ha actualizado correctamente")
    }


    private fun getHistorialEscuchas() {
        val token = Preferencias.obtenerValorString("token", "")
        apiService.getHistorialEscuchas("Bearer $token").enqueue(object : Callback<HistorialEscuchasResponse> {
            override fun onResponse(call: Call<HistorialEscuchasResponse>, response: Response<HistorialEscuchasResponse>) {
                Log.d("MiApp", "entra en on response Escuchas")
                if (response.isSuccessful) {
                    Log.d("MiApp", "entra en on response succesful Escuchas")
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            Log.d("MiApp", "entra en respuesta http escuchas = $it")
                            val escuchas = it.historial_canciones
                            Log.d("MiApp", "escuchas = $escuchas")

                            // Actualizar y mostrar las canciones si las hay
                            if (escuchas.isNotEmpty()) {
                                escuchasAdapter.updateDataEscucha(escuchas)
                                recyclerViewEscuchas.visibility = View.VISIBLE
                                headerEscuchasTextView.visibility = View.VISIBLE
                            } else {
                                recyclerViewEscuchas.visibility = View.GONE
                                headerEscuchasTextView.visibility = View.GONE
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
                Log.d("MiApp", "entra en on response Playlists")
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            val misPlaylists = it.playlists

                            // Actualizar y mostrar las canciones si las hay
                            if (misPlaylists.isNotEmpty()) {
                                playlistsAdapter.updateDataMisPlaylists(misPlaylists)
                                recyclerViewPlaylists.visibility = View.VISIBLE
                                headerPlaylistsTextView.visibility = View.VISIBLE
                            } else {
                                recyclerViewPlaylists.visibility = View.GONE
                                headerPlaylistsTextView.visibility = View.GONE
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
                Log.d("MiApp", "entra en on response Recomendaciones")
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            val recomendaciones = it.canciones_recomendadas

                            // Actualizar y mostrar las canciones si las hay
                            if (recomendaciones.isNotEmpty()) {
                                recomendacionesAdapter.updateDataRecomendacion(recomendaciones)
                                recyclerViewRecomendaciones.visibility = View.VISIBLE
                                headerRecomendacionesTextView.visibility = View.VISIBLE
                            } else {
                                recyclerViewRecomendaciones.visibility = View.GONE
                                headerRecomendacionesTextView.visibility = View.GONE
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
            val esOyente = Preferencias.obtenerValorString("esOyente", "")
            if (esOyente == "oyente") {
                Log.d("Login", "El usuario es un oyente")
                startActivity(Intent(this, Perfil::class.java))
            } else {
                Log.d("Login", "El usuario NO es un oyente")
                startActivity(Intent(this, PerfilArtista::class.java))
            }

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