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
import android.view.MotionEvent
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
import com.example.myapplication.Adapters.Notificaciones.InvitacionesAdapter
import com.example.myapplication.io.request.AudioRequest
import com.example.myapplication.io.response.AddReproduccionResponse
import com.example.myapplication.io.response.AudioResponse
import com.example.myapplication.io.response.CancionInfoResponse
import com.example.myapplication.io.response.GetInvitacionesResponse
import com.example.myapplication.io.response.HArtistas
import com.example.myapplication.io.response.HRecientes
import com.example.myapplication.io.response.HayNotificacionesResponse
import com.example.myapplication.io.response.HistorialArtistasResponse
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.InvitacionPlaylist
import com.example.myapplication.io.response.Novedad
import com.example.myapplication.io.response.Seguidor
import com.example.myapplication.services.MusicPlayerService
import com.example.myapplication.services.WebSocketEventHandler
import org.json.JSONObject


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

    private lateinit var headerRecientesTextView: TextView
    private lateinit var headerEscuchasTextView: TextView
    private lateinit var headerPlaylistsTextView: TextView
    private lateinit var headerRecomendacionesTextView: TextView

    private val listaRecientes = mutableListOf<HRecientes>()
    private val listaArtistas = mutableListOf<HArtistas>()

    private lateinit var dot: View

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
            handler.post(updateRunnable)
            // El servicio ya está listo, ahora actualiza el mini reproductor
            updateMiniReproductor()
            actualizarIconoPlayPause()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
            handler.removeCallbacks(updateRunnable)
        }
    }

    private var isDataLoaded = false

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_bueno)

        // Inicializar API Service
        apiService = ApiService.create()
        dot = findViewById<View>(R.id.notificationDot)

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
            reproducir(escucha.id)
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

        recomendacionesAdapter = RecomendacionesAdapter(mutableListOf()){ recomendacion ->
            reproducir(recomendacion.id)
        }
        recyclerViewRecomendaciones.adapter = recomendacionesAdapter


        progressBar = findViewById(R.id.progressBar)
        // Actualizar la información del mini reproductor
        updateMiniReproductor()

        // Cargar datos al iniciar
        loadHomeData()

        // Configurar botones de navegación
        setupNavigation()
    }

    private fun updateMiniReproductor() {
        val songImage = findViewById<ImageView>(R.id.songImage)
        val songTitle = findViewById<TextView>(R.id.songTitle)
        val songArtist = findViewById<TextView>(R.id.songArtist)
        val stopButton = findViewById<ImageButton>(R.id.stopButton)

        val songImageUrl = Preferencias.obtenerValorString("fotoPortadaActual", "")
        val songTitleText = Preferencias.obtenerValorString("nombreCancionActual", "Nombre de la canción")
        val songArtistText = Preferencias.obtenerValorString("nombreArtisticoActual", "Artista")
        val songProgress = Preferencias.obtenerValorEntero("progresoCancionActual", 0)

        // Imagen
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

        songTitle.text = songTitleText
        songArtist.text = songArtistText
        progressBar.progress = songProgress/1749

        songImage.setOnClickListener {
            startActivity(Intent(this, CancionReproductorDetail::class.java))
        }

        // Configurar botón de play/pause
        stopButton.setOnClickListener {
            Log.d("MiniReproductor", "Botón presionado")
            if (musicService == null) {
                Log.w("MiniReproductor", "musicService es null")
                return@setOnClickListener
            }

            musicService?.let { service ->
                Log.d("MiniReproductor", "isPlaying: ${service.isPlaying()}")
                if (service.isPlaying()) {
                    val progreso = service.getProgress()
                    Preferencias.guardarValorEntero("progresoCancionActual", progreso)
                    service.pause()
                    stopButton.setImageResource(R.drawable.ic_pause)
                    Log.d("MiniReproductor", "Canción pausada en $progreso ms")
                } else {
                    Log.d("MiniReproductor", "Intentando reanudar la canción...")
                    service.resume()
                    stopButton.setImageResource(R.drawable.ic_play)
                    Log.d("MiniReproductor", "Canción reanudada")
                }
            }
        }

        // Añadir un OnTouchListener al ProgressBar para actualizar el progreso
        // Añadir el performClick dentro del OnTouchListener
        progressBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    updateProgressFromTouch(event.x, progressBar)
                    progressBar.performClick()  // Agregar esta línea
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    updateProgressFromTouch(event.x, progressBar)
                    progressBar.performClick()  // Agregar esta línea
                    true
                }
                MotionEvent.ACTION_UP -> {
                    updateProgressFromTouch(event.x, progressBar)
                    progressBar.performClick()  // Agregar esta línea
                    true
                }
                else -> false
            }
        }

    }

    private fun updateProgressFromTouch(x: Float, progressBar: ProgressBar) {
        // Obtener el ancho del ProgressBar
        val width = progressBar.width - progressBar.paddingLeft - progressBar.paddingRight
        // Calcular el progreso basado en la posición del toque (x)
        val progress = ((x / width) * 100).toInt()

        // Actualizar el ProgressBar
        progressBar.progress = progress

        // Actualizar el progreso en el servicio de música
        musicService?.let { service ->
            val duration = service.getDuration()
            val newProgress = (progress * duration) / 100
            service.seekTo(newProgress)  // Mover la canción al nuevo progreso
            Preferencias.guardarValorEntero("progresoCancionActual", newProgress)
            Log.d("MiniReproductor", "Nuevo progreso: $newProgress ms")
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

    private fun onDataLoaded() {
        // Solo iniciar el servicio una vez los datos estén listos
        if (isDataLoaded) {
            val urlCancion = Preferencias.obtenerValorString("audioCancionActual", "")
            val progreso = Preferencias.obtenerValorEntero("progresoCancionActual", 0)

            Log.d("MiniReproductor", "onStart - URL: $urlCancion, Progreso: $progreso")

            if (!urlCancion.isNullOrEmpty()) {
                val startIntent = Intent(this, MusicPlayerService::class.java).apply {
                    action = "PLAY1"
                    putExtra("url", urlCancion)
                    putExtra("progreso", progreso)
                }
                startService(startIntent)
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

                            val primerinicio = Preferencias.obtenerValorBooleano("primerinicio", false)
                            if(primerinicio == false) {
                                updateMiniReproductor()
                                isDataLoaded = true // Los datos ya están listos
                                // Ahora que los datos están listos, puedes iniciar el servicio si es necesario
                                onDataLoaded()
                                Preferencias.guardarValorBooleano("primerinicio", true)
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

    private fun actualizarIconoPlayPause() {
        if (serviceBound && musicService != null) {
            val estaReproduciendo = musicService!!.isPlaying()
            val icono = if (estaReproduciendo) R.drawable.ic_play else R.drawable.ic_pause
            val stopButton = findViewById<ImageButton>(R.id.stopButton)
            stopButton.setImageResource(icono)
        }
    }

    private fun setupNavigation() {
        val buttonPerfil: ImageButton = findViewById(R.id.profileImageButton)
        val buttonNotis: ImageButton = findViewById(R.id.notificationImageButton)
        val buttonHome: ImageButton = findViewById(R.id.nav_home)
        val buttonSearch: ImageButton = findViewById(R.id.nav_search)
        val buttonCrear: ImageButton = findViewById(R.id.nav_create)
        val buttonNoizzys: ImageButton = findViewById(R.id.nav_noizzys)

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

        buttonNoizzys.setOnClickListener {
            startActivity(Intent(this, MisNoizzys::class.java))
        }
    }



    private fun reproducir(id: String) {
        val request = AudioRequest(id)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        val sid = WebSocketManager.getInstance().getSid()
        Log.d("WebSocket", "El SID actual es: $sid")

        if (sid == null) {
            Log.e("MiApp", "No se ha generado un sid para el WebSocket")
            return
        }

        // Llamada a la API con el sid en los headers
        apiService.reproducirCancion(authHeader, sid, request).enqueue(object : Callback<AudioResponse> {
            override fun onResponse(call: Call<AudioResponse>, response: Response<AudioResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { audioResponse ->
                        val respuestaTexto = "Audio: ${audioResponse.audio}, Favorito: ${audioResponse.fav}"

                        // Mostrar en Logcat
                        Log.d("API_RESPONSE", "Respuesta exitosa: $respuestaTexto")

                        // Mostrar en Toast
                        Toast.makeText(this@Home, respuestaTexto, Toast.LENGTH_LONG).show()

                        reproducirAudio(audioResponse.audio)
                        notificarReproduccion()

                        Preferencias.guardarValorString("audioCancionActual", audioResponse.audio)
                        guardarDatoscCancion(id)
                    }
                } else {
                    val errorMensaje = response.errorBody()?.string() ?: "Error desconocido"

                    // Mostrar en Logcat
                    Log.e("API_RESPONSE", "Error en la respuesta: Código ${response.code()} - $errorMensaje")

                    // Mostrar en Toast
                    Toast.makeText(this@Home, "Error: $errorMensaje", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<AudioResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)

                // Mostrar en Toast
                Toast.makeText(this@Home, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun reproducirAudio(audioUrl: String, progreso: Int = 0) {
        try {
            Preferencias.guardarValorEntero("progresoCancionActual", progreso)
            val startIntent = Intent(this, MusicPlayerService::class.java).apply {
                action = "PLAY"
                putExtra("url", audioUrl)
                putExtra("progreso", progreso)
            }
            startService(startIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al reproducir el audio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun notificarReproduccion() {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.addReproduccion(authHeader).enqueue(object : Callback<AddReproduccionResponse> {
            override fun onResponse(call: Call<AddReproduccionResponse>, response: Response<AddReproduccionResponse>) {
                if (response.isSuccessful) {
                    Log.d("MiApp", "Reproducción registrada exitosamente")
                } else {
                    Log.e("MiApp", "Error al registrar la reproducción")
                }
            }

            override fun onFailure(call: Call<AddReproduccionResponse>, t: Throwable) {
                Log.e("MiApp", "Error de conexión al registrar reproducción")
            }
        })
    }

    private fun guardarDatoscCancion(id: String) {
        Preferencias.guardarValorString("cancionActualId", id)

        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        // Llamada a la API con el sid en los headers
        apiService.getInfoCancion(authHeader, id).enqueue(object : Callback<CancionInfoResponse> {
            override fun onResponse(call: Call<CancionInfoResponse>, response: Response<CancionInfoResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { cancionResponse ->
                        val foto = cancionResponse.fotoPortada
                        val nombre = cancionResponse.nombre
                        val artista = cancionResponse.nombreArtisticoArtista

                        // Mostrar en Logcat
                        Log.d("CancionInfo", "Respuesta exitosa Canción")
                        Log.d("CancionInfo", "Canción: $nombre")
                        Log.d("CancionInfo", "Artista: $artista")
                        Log.d("CancionInfo", "Foto: $foto")

                        Preferencias.guardarValorString("nombreCancionActual", nombre)
                        Preferencias.guardarValorString("nombreArtisticoActual", artista)
                        Preferencias.guardarValorString("fotoPortadaActual", foto)

                        updateMiniReproductor()
                        actualizarIconoPlayPause()
                    }
                } else {
                    val errorMensaje = response.errorBody()?.string() ?: "Error desconocido"

                    // Mostrar en Logcat
                    Log.e("API_RESPONSE", "Error en la respuesta: Código ${response.code()} - $errorMensaje")

                    // Mostrar en Toast
                    Toast.makeText(this@Home, "Error: $errorMensaje", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<CancionInfoResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)

                // Mostrar en Toast
                Toast.makeText(this@Home, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
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

    override fun onDestroy() {
        super.onDestroy()
        WebSocketEventHandler.eliminarListenerNovedad(listenerNovedad)
        WebSocketEventHandler.eliminarListenerSeguidor(listenerSeguidor)
        WebSocketEventHandler.eliminarListenerInvitacion(listenerInvitacion)
        WebSocketEventHandler.eliminarListenerInteraccion(listenerInteraccion)
    }

}