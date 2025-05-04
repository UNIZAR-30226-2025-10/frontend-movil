package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.myapplication.Adapters.Album.CancionesAlbumAdapter
import com.example.myapplication.Adapters.Album.CancionesAlbumAdapter.Companion.REQUEST_CREATE_PLAYLIST
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.AddToPlaylistRequest
import com.example.myapplication.io.request.AudioColeccionRequest
import com.example.myapplication.io.request.AudioRequest
import com.example.myapplication.io.response.AddReproduccionResponse
import com.example.myapplication.io.response.AudioResponse
import com.example.myapplication.io.response.CancionInfoResponse
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
import com.example.myapplication.services.MusicPlayerService
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


    private var aleatorio = false
    private var modo = "enOrden"
    private var modoCambiado = false
    private var orden: List<String> = listOf()
    private var indexActual: Int = 0
    private var albumIdActual: String? = null

    private lateinit var progressBar: ProgressBar
    private var musicService: MusicPlayerService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.MusicBinder
            musicService = binder.getService()
            serviceBound = true
            handler.post(updateRunnable)
            actualizarIconoPlayPause()
            MusicPlayerService.setOnCompletionListener {
                runOnUiThread {
                    Log.d("Reproducción", "Canción finalizada, pasando a la siguiente")
                    indexActual++
                    Preferencias.guardarValorEntero("indexColeccionActual", indexActual)
                    Preferencias.guardarValorEntero("progresoCancionActual", 0)
                    reproducirColeccion()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
        }
    }
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateProgressBar()
            handler.postDelayed(this, 1000) // cada segundo
        }
    }

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

        cancionesAdapter = CancionesAlbumAdapter(emptyList(), "",
            { cancion ->
                val cancionId = Preferencias.obtenerValorString("cancionActualId", "")
                if(cancionId == cancion.id){
                    startActivity(Intent(this, CancionReproductorDetail::class.java))
                }
                else {
                    reproducir(cancion.id)
                }
            }
        ).apply {
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

        // Detectar el modo actual y actualizar el estado del switch

        Preferencias.guardarValorString("modoColeccionMirada", "enOrden")
        Preferencias.guardarValorEntero("indexColeccionMirada", indexActual)
        progressBar = findViewById(R.id.progressBar)
        updateMiniReproductor()

        val btnPlayPauseAlbum: Button = findViewById(R.id.reproNormal)
        // Agregar funcionalidad al botón de añadir canción
        btnPlayPauseAlbum.setOnClickListener {
            albumId?.let {
                albumIdActual = it
                Preferencias.guardarValorString("coleccionActualId", it)


                orden = Preferencias.obtenerValorString("ordenColeccionMirada", "")
                    .split(",")
                    .filter { id -> id.isNotEmpty() }
                val ordenNatural = Preferencias.obtenerValorString("ordenNaturalColeccionMirada", "")
                    .split(",")
                    .filter { id -> id.isNotEmpty() }
                modo = Preferencias.obtenerValorString("modoColeccionMirada", "enOrden")

                if (indexActual <= -1 || indexActual >= orden.size) {
                    indexActual = 0
                }

                Preferencias.guardarValorString("ordenNaturalColeccionActual", ordenNatural.joinToString(","))
                Preferencias.guardarValorEntero("indexColeccionActual", indexActual)
                Preferencias.guardarValorString("modoColeccionActual", modo)

                if(modo == "enOrden"){
                    Preferencias.guardarValorString("ordenColeccionActual", ordenNatural.joinToString(","))
                }
                else{
                    Preferencias.guardarValorString("ordenColeccionActual", orden.joinToString(","))
                }


                reproducirColeccion()
            }
        }

        val btnAleatorio: ImageButton = findViewById(R.id.aleatorio)
        btnAleatorio.setOnClickListener {
            if (aleatorio == true){
                Preferencias.guardarValorString("modoColeccionMirada", "enOrden")
                btnAleatorio.setImageResource(R.drawable.shuffle_24px)
                aleatorio = false
                modo = "enOrden"

                val idActual = Preferencias.obtenerValorString("cancionActualId", "")
                val idColeccionActual = Preferencias.obtenerValorString("coleccionActualId", "")
                orden = Preferencias.obtenerValorString("ordenNaturalColeccionMirada", "")
                    .split(",")
                    .filter { id -> id.isNotEmpty() }
                if(idColeccionActual == albumId){
                    indexActual = orden.indexOf(idActual)
                }
                else{
                    indexActual = 0
                }

                Preferencias.guardarValorString("ordenColeccionMirada", orden.joinToString(","))
                Log.d("ReproducirAlbum", "IDs en orden normal: ${orden.joinToString(",")}")
                Log.d("ReproducirAlbum", "Indice del id: $indexActual")
            }
            else{
                orden = Preferencias.obtenerValorString("ordenNaturalColeccionMirada", "")
                    .split(",")
                    .filter { id -> id.isNotEmpty() }
                val primerId = orden[indexActual]
                // ordenar aleatoriamente la lista manteniendo primerId al inicio
                orden = orden.shuffled().toMutableList().apply {
                    remove(primerId)
                    add(0, primerId)
                }
                Log.d("ReproducirAlbum", "IDs aleatorios: ${orden.joinToString(",")}")
                indexActual = 0
                Preferencias.guardarValorEntero("indexColeccionMirada", indexActual)
                Preferencias.guardarValorString("ordenColeccionMirada", orden.joinToString(","))
                Preferencias.guardarValorString("modoColeccionMirada", "aleatorio")
                btnAleatorio.setImageResource(R.drawable.shuffle_24px_act)
                aleatorio = true
                modo = "aleatorio"
            }
        }
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
                            .centerCrop()
                            .transform(
                                RoundedCorners(
                                    TypedValue.applyDimension(
                                        TypedValue.COMPLEX_UNIT_DIP,
                                        12f,
                                        this@AlbumDetail.resources.displayMetrics
                                    ).toInt()
                                )
                            )
                            .placeholder(R.drawable.no_cancion)
                            .error(R.drawable.no_cancion)
                            .into(fotoPortada)
                    }

                    val ids: List<String>? = canciones?.map { it.id }
                    ids?.let {
                        Log.d("ReproducirAlbum", "IDs extraídos: ${it.joinToString(",")}")
                        Preferencias.guardarValorString("ordenNaturalColeccionMirada", ids.joinToString(","))
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

    private fun updateMiniReproductor() {
        val songImage = findViewById<ImageView>(R.id.songImage)
        val songTitle = findViewById<TextView>(R.id.songTitle)
        val songArtist = findViewById<TextView>(R.id.songArtist)
        val stopButton = findViewById<ImageButton>(R.id.stopButton)
        val btnAvanzar = findViewById<ImageButton>(R.id.btnAvanzar)
        val btnRetroceder = findViewById<ImageButton>(R.id.btnRetroceder)

        val songImageUrl = Preferencias.obtenerValorString("fotoPortadaActual", "")
        val songTitleText = Preferencias.obtenerValorString("nombreCancionActual", "")
        val songArtistText = Preferencias.obtenerValorString("nombreArtisticoActual", "")
        val songProgress = Preferencias.obtenerValorEntero("progresoCancionActual", 0)

        // Imagen
        if (songImageUrl.isNullOrEmpty()) {
            songImage.setImageResource(R.drawable.no_cancion)
        } else {
            Glide.with(this)
                .load(songImageUrl)
                .transform(
                    MultiTransformation(
                        CenterCrop(),
                        RoundedCorners(
                            TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                6f,
                                this.resources.displayMetrics
                            ).toInt()
                        )
                    )
                )
                .placeholder(R.drawable.no_cancion)
                .error(R.drawable.no_cancion)
                .into(songImage)
        }

        songTitle.text = songTitleText
        songArtist.text = songArtistText
        progressBar.progress = songProgress/1749

        songImage.setOnClickListener {
            startActivity(Intent(this, CancionReproductorDetail::class.java))
        }

        // Configurar botón de play/pause
        btnRetroceder.setOnClickListener {
            val hayColeccion = Preferencias.obtenerValorString("coleccionActualId", "")
            if(hayColeccion == ""){
                val cancionActual = Preferencias.obtenerValorString("cancionActualId", "")
                reproducir(cancionActual)
            }
            else{
                indexActual--
                val ordenColeccion = Preferencias.obtenerValorString("ordenColeccionActual", "")
                    .split(",")
                    .filter { id -> id.isNotEmpty() }
                if (indexActual < 0){
                    indexActual = ordenColeccion.size
                }
                Preferencias.guardarValorEntero("indexColeccionActual", indexActual)
                reproducirColeccion()
            }
        }
        // Configurar botón de play/pause
        btnAvanzar.setOnClickListener {
            val hayColeccion = Preferencias.obtenerValorString("coleccionActualId", "")
            if(hayColeccion == ""){
                val cancionActual = Preferencias.obtenerValorString("cancionActualId", "")
                reproducir(cancionActual)
            }
            else{
                indexActual++
                val ordenColeccion = Preferencias.obtenerValorString("ordenColeccionActual", "")
                    .split(",")
                    .filter { id -> id.isNotEmpty() }
                if (indexActual > ordenColeccion.size){
                    indexActual=0
                }
                Preferencias.guardarValorEntero("indexColeccionActual", indexActual)
                reproducirColeccion()
            }
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

    private fun actualizarIconoPlayPause() {
        if (serviceBound && musicService != null) {
            val estaReproduciendo = musicService!!.isPlaying()
            val icono = if (estaReproduciendo) R.drawable.ic_play else R.drawable.ic_pause
            val stopButton = findViewById<ImageButton>(R.id.stopButton)
            stopButton.setImageResource(icono)
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

                        Preferencias.guardarValorString("coleccionActualId", "")
                        // Mostrar en Logcat
                        Log.d("API_RESPONSE", "Respuesta exitosa: $respuestaTexto")

                        // Mostrar en Toast
                        //Toast.makeText(this@PlaylistDetail, respuestaTexto, Toast.LENGTH_LONG).show()

                        reproducirAudio(audioResponse.audio)
                        notificarReproduccion()

                        Preferencias.guardarValorString("audioCancionActual", audioResponse.audio)
                        guardarDatoscCancion(id)
                        actualizarIconoPlayPause()
                    }
                } else {
                    val errorMensaje = response.errorBody()?.string() ?: "Error desconocido"

                    // Mostrar en Logcat
                    Log.e("API_RESPONSE", "Error en la respuesta: Código ${response.code()} - $errorMensaje")

                    // Mostrar en Toast
                    //Toast.makeText(this@PlaylistDetail, "Error: $errorMensaje", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<AudioResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)

                // Mostrar en Toast
                Toast.makeText(this@AlbumDetail, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun reproducirColeccion() {
        val ordenColeccion = Preferencias.obtenerValorString("ordenColeccionActual", "")
            .split(",")
            .filter { id -> id.isNotEmpty() }

        val modoColeccion =  Preferencias.obtenerValorString("modoColeccionActual", "")

        val indice = Preferencias.obtenerValorEntero("indexColeccionActual", 0)

        val idcoleccion = Preferencias.obtenerValorString("coleccionActualId", "")

        val listaNatural = Preferencias.obtenerValorString("ordenNaturalColeccionActual", "")
            .split(",")
            .filter { id -> id.isNotEmpty() }

        Log.d("ReproducirAlbum", "Lista natural ids: ${listaNatural.joinToString(",")}")
        Log.d("ReproducirAlbum", "Lista ids reproduccion: ${ordenColeccion.joinToString(",")}")
        Log.d("ReproducirAlbum", "Indice: $indice")
        Log.d("ReproducirAlbum", "Modo: $modoColeccion")
        Log.d("ReproducirAlbum", "Id coleccion: $idcoleccion")

        val request = AudioColeccionRequest(idcoleccion, modoColeccion, ordenColeccion, indice)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        val sid = WebSocketManager.getInstance().getSid() ?: run {
            Log.e("WebSocket", "SID no disponible")
            return
        }

        apiService.reproducirColeccion(authHeader, sid, request).enqueue(object : Callback<AudioResponse> {
            override fun onResponse(call: Call<AudioResponse>, response: Response<AudioResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { audioResponse ->
                        reproducirAudioColeccion(audioResponse.audio) // No enviar progreso
                        notificarReproduccion()
                        guardarDatoscCancion(ordenColeccion[indice])
                        actualizarIconoPlayPause()
                    }
                } else {
                    Log.e("API", "Error: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<AudioResponse>, t: Throwable) {
                Log.e("API", "Fallo: ${t.message}")
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
            actualizarIconoPlayPause()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al reproducir el audio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reproducirAudioColeccion(audioUrl: String, progreso: Int = 0) {
        try {
            Preferencias.guardarValorEntero("progresoCancionActual", progreso)
            val intent = Intent(this, MusicPlayerService::class.java).apply {
                action = "PLAY"
                putExtra("url", audioUrl)
                putExtra("progreso", progreso)
            }
            startService(intent)
            actualizarIconoPlayPause()

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
                    Toast.makeText(this@AlbumDetail, "Error: $errorMensaje", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<CancionInfoResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)

                // Mostrar en Toast
                Toast.makeText(this@AlbumDetail, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
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