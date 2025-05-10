package com.example.myapplication.activities

import HeaderAdapter
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.myapplication.Adapters.Buscador.AlbumAdapter
import com.example.myapplication.Adapters.Buscador.ArtistaAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.BuscadorResponse
import com.example.myapplication.utils.Preferencias
import com.example.myapplication.Adapters.Buscador.CancionAdapter
import com.example.myapplication.Adapters.Buscador.PerfilAdapter
import com.example.myapplication.Adapters.Buscador.PlaylistAdapter
import com.example.myapplication.io.request.AudioColeccionRequest
import com.example.myapplication.io.request.AudioRequest
import com.example.myapplication.io.response.AddReproduccionResponse
import com.example.myapplication.io.response.AudioResponse
import com.example.myapplication.io.response.CancionInfoResponse
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.InvitacionPlaylist
import com.example.myapplication.io.response.Novedad
import com.example.myapplication.io.response.Seguidor
import com.example.myapplication.services.MusicPlayerService
import com.example.myapplication.services.WebSocketEventHandler
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
    private lateinit var headerCancionesTextView: TextView
    private lateinit var headerArtistasTextView: TextView
    private lateinit var headerAlbumesTextView: TextView
    private lateinit var headerPlaylistsTextView: TextView
    private lateinit var headerPerfilesTextView: TextView
    private lateinit var cancionAdapter: CancionAdapter
    private lateinit var artistaAdapter: ArtistaAdapter
    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var playlistAdapter: PlaylistAdapter
    private lateinit var perfilAdapter: PerfilAdapter
    private lateinit var apiService: ApiService
    private lateinit var radioOptions: RadioGroup
    private lateinit var radioTodos: RadioButton
    private lateinit var radioCanciones: RadioButton
    private lateinit var radioArtistas: RadioButton
    private lateinit var radioAlbumes: RadioButton
    private lateinit var radioPlaylists: RadioButton
    private lateinit var radioPerfiles: RadioButton
    private lateinit var dot: View
    private var yaRedirigidoAlLogin = false

    private lateinit var progressBar: ProgressBar
    private var musicService: MusicPlayerService? = null
    private var serviceBound = false

    private var indexActual = 0
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.MusicBinder
            musicService = binder.getService()
            serviceBound = true
            handler.post(updateRunnable)
            actualizarIconoPlayPause()
            MusicPlayerService.setOnCompletionListener {
                runOnUiThread {
                    val idcoleccion = Preferencias.obtenerValorString("coleccionActualId", "")
                    if(idcoleccion == ""){
                        Preferencias.guardarValorEntero("progresoCancionActual", 0)
                        musicService?.resume()
                    }
                    else {
                        Log.d("Reproducción", "Canción finalizada, pasando a la siguiente")
                        indexActual++
                        val ordenAct = Preferencias.obtenerValorString("ordenColeccionActual", "")
                            .split(",")
                            .filter { id -> id.isNotEmpty() }
                        if(indexActual >= ordenAct.size){
                            indexActual=0
                        }
                        Preferencias.guardarValorEntero("indexColeccionActual", indexActual)
                        Preferencias.guardarValorEntero("progresoCancionActual", 0)
                        reproducirColeccion()
                    }
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
            actualizarIconoPlayPause()
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
        setContentView(R.layout.buscador)

        apiService = ApiService.create()

        val profileImageButton = findViewById<ImageButton>(R.id.profileImageButton)

        // Obtener la URL de la imagen de perfil desde SharedPreferences
        val profileImageUrl = Preferencias.obtenerValorString("fotoPerfil", "")

        Log.d("ProfileImage", "URL de la imagen de perfil: $profileImageUrl")

        indexActual = Preferencias.obtenerValorEntero("indexColeccionActual", 0)

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
        headerCancionesTextView = findViewById(R.id.textViewHeaderCanciones)
        headerCancionesTextView.visibility = View.INVISIBLE

        headerAlbumesTextView = findViewById(R.id.textViewHeaderAlbumes)
        headerAlbumesTextView.visibility = View.INVISIBLE

        headerArtistasTextView = findViewById(R.id.textViewHeaderArtistas)
        headerArtistasTextView.visibility = View.INVISIBLE

        headerPlaylistsTextView = findViewById(R.id.textViewHeaderPlaylists)
        headerPlaylistsTextView.visibility = View.INVISIBLE

        headerPerfilesTextView = findViewById(R.id.textViewHeaderPerfiles)
        headerPerfilesTextView.visibility = View.INVISIBLE

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
            val cancionId = Preferencias.obtenerValorString("cancionActualId", "")
            if(cancionId == cancion.id){
                startActivity(Intent(this, CancionReproductorDetail::class.java))
            }
            else {
                reproducir(cancion.id)
            }
        }
        recyclerViewCancion.adapter = cancionAdapter

        artistaAdapter = ArtistaAdapter(mutableListOf()){ artista ->
            val intent = Intent(this, OtroArtista::class.java)
            intent.putExtra("nombreUsuario", artista.nombreUsuario)
            intent.putExtra("nombreArtistico", artista.nombreArtistico)
            Log.d("Album", "Buscador -> OtroArtista")
            startActivity(intent)
        }
        recyclerViewArtista.adapter = artistaAdapter

        albumAdapter = AlbumAdapter(mutableListOf()){ album ->
            val intent = Intent(this, AlbumDetail::class.java)
            intent.putExtra("nombre", album.nombre)
            intent.putExtra("nombreArtista", album.nombreArtisticoArtista)
            intent.putExtra("imagen", album.fotoPortada)
            intent.putExtra("id", album.id)
            Log.d("Album", "Buscador -> Album")
            startActivity(intent)
        }
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

        radioOptions = findViewById(R.id.radioGroupFiltros)
        radioTodos = findViewById(R.id.radioTodo)
        radioCanciones = findViewById(R.id.radioCanciones)
        radioArtistas = findViewById(R.id.radioArtistas)
        radioAlbumes = findViewById(R.id.radioAlbumes)
        radioPlaylists = findViewById(R.id.radioPlaylists)
        radioPerfiles = findViewById(R.id.radioPerfiles)

        progressBar = findViewById(R.id.progressBar)
        setupNavigation()
        updateMiniReproductor()

        // Configurar eventos de cambio en el radioGroup para actualizar la vista
        radioOptions.setOnCheckedChangeListener { _, _ ->
            // Llamas a tu función que actualiza la vista
            aplicarFiltros()
        }

        dot = findViewById<View>(R.id.notificationDot)
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
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@Buscador, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        showToast("Sesión iniciada en otro dispositivo")
                    }
                }
            }

            override fun onFailure(call: Call<BuscadorResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun aplicarFiltros() {
        // Obtener referencias a los TextView para los mensajes
        val noResultsGeneral = findViewById<TextView>(R.id.textViewNoResultsGeneral)
        val noResultsCanciones = findViewById<TextView>(R.id.textViewNoResultsCanciones)
        val noResultsArtistas = findViewById<TextView>(R.id.textViewNoResultsArtistas)
        val noResultsAlbumes = findViewById<TextView>(R.id.textViewNoResultsAlbumes)
        val noResultsPlaylists = findViewById<TextView>(R.id.textViewNoResultsPlaylists)
        val noResultsPerfiles = findViewById<TextView>(R.id.textViewNoResultsPerfiles)

        // Verificar si no hay resultados en ninguna categoría
        val noHayResultados = cancionAdapter.itemCount == 0 &&
                artistaAdapter.itemCount == 0 &&
                albumAdapter.itemCount == 0 &&
                playlistAdapter.itemCount == 0 &&
                perfilAdapter.itemCount == 0

        // Mostrar mensaje general cuando está en "Todo" y no hay resultados
        if (radioTodos.isChecked && noHayResultados) {
            noResultsGeneral.visibility = View.VISIBLE
            // Ocultar todos los RecyclerViews y encabezados
            recyclerViewCancion.visibility = View.GONE
            recyclerViewArtista.visibility = View.GONE
            recyclerViewAlbum.visibility = View.GONE
            recyclerViewPlaylist.visibility = View.GONE
            recyclerViewPerfil.visibility = View.GONE
            headerCancionesTextView.visibility = View.GONE
            headerArtistasTextView.visibility = View.GONE
            headerAlbumesTextView.visibility = View.GONE
            headerPlaylistsTextView.visibility = View.GONE
            headerPerfilesTextView.visibility = View.GONE
            // Ocultar también los mensajes individuales
            noResultsCanciones.visibility = View.GONE
            noResultsArtistas.visibility = View.GONE
            noResultsAlbumes.visibility = View.GONE
            noResultsPlaylists.visibility = View.GONE
            noResultsPerfiles.visibility = View.GONE
            return
        } else {
            noResultsGeneral.visibility = View.GONE
        }

        // Lógica para cada categoría individual (como antes)
        val showCanciones = (radioTodos.isChecked || radioCanciones.isChecked)
        val hasCanciones = cancionAdapter.itemCount > 0
        recyclerViewCancion.visibility = if (showCanciones && hasCanciones) View.VISIBLE else View.GONE
        headerCancionesTextView.visibility = if (showCanciones && hasCanciones) View.VISIBLE else View.GONE
        noResultsCanciones.visibility = if (showCanciones && !hasCanciones) View.VISIBLE else View.GONE

        val showArtistas = (radioTodos.isChecked || radioArtistas.isChecked)
        val hasArtistas = artistaAdapter.itemCount > 0
        recyclerViewArtista.visibility = if (showArtistas && hasArtistas) View.VISIBLE else View.GONE
        headerArtistasTextView.visibility = if (showArtistas && hasArtistas) View.VISIBLE else View.GONE
        noResultsArtistas.visibility = if (showArtistas && !hasArtistas) View.VISIBLE else View.GONE

        val showAlbumes = (radioTodos.isChecked || radioAlbumes.isChecked)
        val hasAlbumes = albumAdapter.itemCount > 0
        recyclerViewAlbum.visibility = if (showAlbumes && hasAlbumes) View.VISIBLE else View.GONE
        headerAlbumesTextView.visibility = if (showAlbumes && hasAlbumes) View.VISIBLE else View.GONE
        noResultsAlbumes.visibility = if (showAlbumes && !hasAlbumes) View.VISIBLE else View.GONE

        val showPlaylists = (radioTodos.isChecked || radioPlaylists.isChecked)
        val hasPlaylists = playlistAdapter.itemCount > 0
        recyclerViewPlaylist.visibility = if (showPlaylists && hasPlaylists) View.VISIBLE else View.GONE
        headerPlaylistsTextView.visibility = if (showPlaylists && hasPlaylists) View.VISIBLE else View.GONE
        noResultsPlaylists.visibility = if (showPlaylists && !hasPlaylists) View.VISIBLE else View.GONE

        val showPerfiles = (radioTodos.isChecked || radioPerfiles.isChecked)
        val hasPerfiles = perfilAdapter.itemCount > 0
        recyclerViewPerfil.visibility = if (showPerfiles && hasPerfiles) View.VISIBLE else View.GONE
        headerPerfilesTextView.visibility = if (showPerfiles && hasPerfiles) View.VISIBLE else View.GONE
        noResultsPerfiles.visibility = if (showPerfiles && !hasPerfiles) View.VISIBLE else View.GONE
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
            startActivity(Intent(this, CrearPlaylist::class.java))
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
                    indexActual = ordenColeccion.size-1
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
                if (indexActual >= ordenColeccion.size){
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
                        Toast.makeText(this@Buscador, respuestaTexto, Toast.LENGTH_LONG).show()

                        reproducirAudio(audioResponse.audio)
                        notificarReproduccion()

                        Preferencias.guardarValorString("audioCancionActual", audioResponse.audio)
                        guardarDatoscCancion(id)
                        actualizarIconoPlayPause()
                    }
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@Buscador, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        showToast("Sesión iniciada en otro dispositivo")
                    }
                    val errorMensaje = response.errorBody()?.string() ?: "Error desconocido"

                    // Mostrar en Logcat
                    Log.e("API_RESPONSE", "Error en la respuesta: Código ${response.code()} - $errorMensaje")

                    // Mostrar en Toast
                    //Toast.makeText(this@Buscador, "Error: $errorMensaje", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<AudioResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)

                // Mostrar en Toast
                Toast.makeText(this@Buscador, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun reproducirColeccion() {
        val ordenColeccion = Preferencias.obtenerValorString("ordenColeccionActual", "")
            .split(",")
            .filter { id -> id.isNotEmpty() }

        val modoColeccion =  Preferencias.obtenerValorString("modoColeccionActual", "")

        val indice = Preferencias.obtenerValorEntero("indexColeccionActual", 0)

        if (indice >= ordenColeccion.size) {
            Log.d("Reproducción", "Fin de la playlist")
            return
        }

        val idcoleccion = Preferencias.obtenerValorString("coleccionActualId", "")

        val listaNatural = Preferencias.obtenerValorString("ordenNaturalColeccionActual", "")
            .split(",")
            .filter { id -> id.isNotEmpty() }

        Log.d("ReproducirPlaylist", "Lista natural ids: ${listaNatural.joinToString(",")}")
        Log.d("ReproducirPlaylist", "Lista ids reproduccion: ${ordenColeccion.joinToString(",")}")
        Log.d("ReproducirPlaylist", "Indice: $indice")
        Log.d("ReproducirPlaylist", "Modo: $modoColeccion")
        Log.d("ReproducirPlaylist", "Id coleccion: $idcoleccion")

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
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@Buscador, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        showToast("Sesión iniciada en otro dispositivo")
                    }
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
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@Buscador, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        showToast("Sesión iniciada en otro dispositivo")
                    }
                    val errorMensaje = response.errorBody()?.string() ?: "Error desconocido"

                    // Mostrar en Logcat
                    Log.e("API_RESPONSE", "Error en la respuesta: Código ${response.code()} - $errorMensaje")

                    // Mostrar en Toast
                    Toast.makeText(this@Buscador, "Error: $errorMensaje", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<CancionInfoResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)

                // Mostrar en Toast
                Toast.makeText(this@Buscador, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
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