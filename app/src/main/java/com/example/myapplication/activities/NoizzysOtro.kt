package com.example.myapplication.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Paint
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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.myapplication.Adapters.Noizzys.MisNoizzysAdapter
import com.example.myapplication.Adapters.Playlist.CancionesBuscadorNoizzyAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.AudioColeccionRequest
import com.example.myapplication.io.request.AudioRequest
import com.example.myapplication.io.request.DarLikeNoizzyRequest
import com.example.myapplication.io.request.LeerNotiSeguidorRequest
import com.example.myapplication.io.request.PostNoizzitoRequest
import com.example.myapplication.io.request.PostNoizzyRequest
import com.example.myapplication.io.response.AddReproduccionResponse
import com.example.myapplication.io.response.AudioResponse
import com.example.myapplication.io.response.Cancion
import com.example.myapplication.io.response.CancionInfoResponse
import com.example.myapplication.io.response.GetInvitacionesResponse
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.InvitacionPlaylist
import com.example.myapplication.io.response.MisNoizzysResponse
import com.example.myapplication.io.response.NoizzitoData
import com.example.myapplication.io.response.Noizzy
import com.example.myapplication.io.response.Novedad
import com.example.myapplication.io.response.SearchPlaylistResponse
import com.example.myapplication.io.response.Seguidor
import com.example.myapplication.services.MusicPlayerService
import com.example.myapplication.services.WebSocketEventHandler
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class NoizzysOtro: AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var dot: View
    private lateinit var adapter: MisNoizzysAdapter
    private lateinit var recyclerNoizzys: RecyclerView
    private lateinit var botonPublicar: Button
    private var cancionAnadidaEnNoizzito: Cancion? = null
    private var nombreUser: String? = null
    private var yaRedirigidoAlLogin = false

    private lateinit var progressBar: ProgressBar
    private var musicService: MusicPlayerService? = null
    private var serviceBound = false
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            actualizarIconoPlayPause()
            updateProgressBar()
            handler.postDelayed(this, 1000) // cada segundo
        }
    }
    private var indexActual = 0
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.MusicBinder
            musicService = binder.getService()
            serviceBound = true
            handler.post(updateRunnable)
            // El servicio ya está listo, ahora actualiza el mini reproductor
            updateMiniReproductor()
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
            handler.removeCallbacks(updateRunnable)
        }
    }

    //EVENTOS PARA LAS NOTIFICACIONES
    private val listenerNovedad: (Novedad) -> Unit = {
        runOnUiThread {
            dot.visibility = View.VISIBLE
        }
    }
    private val listenerSeguidor: (Seguidor) -> Unit = {
        runOnUiThread {
            dot.visibility = View.VISIBLE
        }
    }
    private val listenerInvitacion: (InvitacionPlaylist) -> Unit = {
        runOnUiThread {
            dot.visibility = View.VISIBLE
        }
    }
    private val listenerInteraccion: (Interaccion) -> Unit = {
        runOnUiThread {
            dot.visibility = View.VISIBLE
        }
    }

    private val listenerNoizzy: (Noizzy, Boolean) -> Unit = { noizzy, mio ->
        runOnUiThread {
            Log.d("LOGS_NOTIS", "evento en mis noizzys")
            if (mio) {
                val adapter = recyclerNoizzys.adapter as? MisNoizzysAdapter
                adapter?.agregarNoizzy(noizzy)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mis_noizzys)

        apiService = ApiService.create()
        dot = findViewById<View>(R.id.notificationDot)
        recyclerNoizzys = findViewById(R.id.noizzysRecyclerView)
        recyclerNoizzys.layoutManager = LinearLayoutManager(this)
        botonPublicar = findViewById(R.id.publicarNoizzyButton)
        botonPublicar.visibility = View.GONE
        nombreUser = intent.getStringExtra("nombreUsuario")
        Log.d("INTENT_TEST", "Recibido nombreUsuario: $nombreUser")

        indexActual = Preferencias.obtenerValorEntero("indexColeccionActual", 0)

        val profileImageButton = findViewById<ImageButton>(R.id.profileImageButton)
        val profileImageUrl = Preferencias.obtenerValorString("fotoPerfil", "")

        if (profileImageUrl.isNullOrEmpty() || profileImageUrl == "DEFAULT") {
            profileImageButton.setImageResource(R.drawable.ic_profile)
        } else {
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

        progressBar = findViewById(R.id.progressBar)
        setupNavigation()
        updateMiniReproductor()
        cargarNoizzys()

    }

    private fun cargarNoizzys() {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.getNoizzys(authHeader, nombreUser!!).enqueue(object :
            Callback<MisNoizzysResponse> {
            override fun onResponse(call: Call<MisNoizzysResponse>, response: Response<MisNoizzysResponse>) {
                if (response.isSuccessful) {
                    val respuesta = response.body()
                    respuesta?.let {
                        val noizzys = response.body()?.noizzys ?: emptyList()
                        val noizzysMutable: MutableList<Noizzy> = noizzys.toMutableList()

                        if (noizzys.size == 0) {
                            Toast.makeText(this@NoizzysOtro, nombreUser + " todavía no ha publicado ningún Noizzy", Toast.LENGTH_SHORT).show()
                            finish()
                            return
                        }

                        adapter = MisNoizzysAdapter(
                            noizzysMutable,
                            "noizzysOtro",
                            onItemClicked = { noizzy ->
                                val intent = Intent(this@NoizzysOtro, NoizzyDetail::class.java)
                                intent.putExtra("id", noizzy.id.toString())
                                startActivity(intent)
                            },
                            onLikeClicked = { noizzy -> darLike(noizzy)},
                            onCommentClicked = { noizzy -> comentar(noizzy) },
                            onDeleteClicked = {noizzy -> borrar(noizzy)},
                            onCancionClicked = {noizzy -> reproducir(noizzy.cancion!!.id.toString())}
                        )

                        recyclerNoizzys.adapter = adapter
                    }
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@NoizzysOtro, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@NoizzysOtro, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onFailure(call: Call<MisNoizzysResponse>, t: Throwable) {
                Log.d("Mis Noizzys", "Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun darLike(noizzy: Noizzy) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        val request = DarLikeNoizzyRequest(!noizzy.like, noizzy.id)
        apiService.darLikeNoizzy(authHeader, request).enqueue(object :
            Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    noizzy.like = !noizzy.like
                    if (noizzy.like) {
                        noizzy.num_likes += 1
                    } else {
                        noizzy.num_likes -= 1
                    }
                    adapter.actualizarNoizzy(noizzy)
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@NoizzysOtro, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@NoizzysOtro, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("Dar like", "Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun comentar(noizzy: Noizzy) {
        val dialogView = layoutInflater.inflate(R.layout.popup_publicar_noizzito, null)

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setDimAmount(0.7f)

        val imageView = dialogView.findViewById<ImageView>(R.id.popupProfileImage)
        val url = Preferencias.obtenerValorString("fotoPerfil", "")

        if (url.isNullOrEmpty() || url == "DEFAULT") {
            imageView.setImageResource(R.drawable.ic_profile)
        } else {
            Glide.with(this)
                .load(url)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(imageView)
        }

        val editText = dialogView.findViewById<EditText>(R.id.popupEditText)
        val addSongText= dialogView.findViewById<TextView>(R.id.popupAddSongButton)
        addSongText.paintFlags = addSongText.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        val publishButton = dialogView.findViewById<Button>(R.id.popupPublishButton)
        val cerrarVentana = dialogView.findViewById<ImageButton>(R.id.closeButton)

        val buscador = dialogView.findViewById<LinearLayout>(R.id.buscador)
        val cancionAnadida = dialogView.findViewById<LinearLayout>(R.id.cancionNoizzy)
        val searchEditText = dialogView.findViewById<EditText>(R.id.searchSongEditText)
        val songResults = dialogView.findViewById<RecyclerView>(R.id.songResultsRecyclerView)
        songResults.layoutManager = LinearLayoutManager(this)

        val dividerItemDecoration = DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        ContextCompat.getDrawable(this, R.drawable.recycler_divider)?.let {
            dividerItemDecoration.setDrawable(it)
        }
        songResults.addItemDecoration(dividerItemDecoration)

        addSongText.setOnClickListener {
            addSongText.visibility = View.GONE
            buscador.visibility = View.VISIBLE
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {

                    val token = Preferencias.obtenerValorString("token", "")
                    val authHeader = "Bearer $token"

                    apiService.searchForSongsNoizzy(authHeader, query).enqueue(object :
                        Callback<SearchPlaylistResponse> {
                        override fun onResponse(call: Call<SearchPlaylistResponse>, response: Response<SearchPlaylistResponse>) {
                            if (response.isSuccessful) {
                                val canciones = response.body()?.canciones ?: emptyList()
                                songResults.adapter = CancionesBuscadorNoizzyAdapter(canciones) { cancionSeleccionada ->
                                    buscador.visibility = View.GONE
                                    cancionAnadidaEnNoizzito = cancionSeleccionada
                                    val imagenCancion = dialogView.findViewById<ImageView>(R.id.recuerdoImage)
                                    val nombreCancion = dialogView.findViewById<TextView>(R.id.recuerdoText1)
                                    val nombreArtista = dialogView.findViewById<TextView>(R.id.recuerdoText2)
                                    val botonQuitar = dialogView.findViewById<ImageButton>(R.id.quitarCancion)

                                    Glide.with(this@NoizzysOtro)
                                        .load(cancionSeleccionada.fotoPortada)
                                        .placeholder(R.drawable.no_cancion)
                                        .error(R.drawable.no_cancion)
                                        .into(imagenCancion)

                                    nombreCancion.text = cancionSeleccionada.nombre
                                    nombreArtista.text = cancionSeleccionada.nombreArtisticoArtista

                                    botonQuitar.setOnClickListener {
                                        cancionAnadidaEnNoizzito = null
                                        addSongText.visibility = View.VISIBLE
                                        cancionAnadida.visibility = View.GONE
                                    }

                                    cancionAnadida.visibility = View.VISIBLE
                                }
                            } else {
                                if (response.code() == 401 && !yaRedirigidoAlLogin) {
                                    yaRedirigidoAlLogin = true
                                    val intent = Intent(this@NoizzysOtro, Inicio::class.java)
                                    startActivity(intent)
                                    finish()
                                    Toast.makeText(this@NoizzysOtro, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        override fun onFailure(call: Call<SearchPlaylistResponse>, t: Throwable) {
                            Log.d("Buscar Canciones para Noizzito", "Error en la solicitud: ${t.message}")
                        }
                    })
                }
            }
        })


        cerrarVentana.setOnClickListener {
            cancionAnadidaEnNoizzito = null
            dialog.dismiss()
        }

        publishButton.setOnClickListener {
            val texto = editText.text.toString()

            val token = Preferencias.obtenerValorString("token", "")
            val authHeader = "Bearer $token"
            val request: PostNoizzitoRequest
            if (cancionAnadidaEnNoizzito != null) {
                request = PostNoizzitoRequest(texto, noizzy.id, cancionAnadidaEnNoizzito!!.id)
            } else {
                request = PostNoizzitoRequest(texto, noizzy.id,null)
            }

            apiService.postNoizzito(authHeader, request).enqueue(object :
                Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        noizzy.num_comentarios += 1
                        adapter.actualizarNoizzy(noizzy)
                    } else {
                        if (response.code() == 401 && !yaRedirigidoAlLogin) {
                            yaRedirigidoAlLogin = true
                            val intent = Intent(this@NoizzysOtro, Inicio::class.java)
                            startActivity(intent)
                            finish()
                            Toast.makeText(this@NoizzysOtro, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.d("Post Noizzito", "Error en la solicitud: ${t.message}")
                }
            })
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun borrar(noizzy: Noizzy){}

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
                        //Toast.makeText(this@Home, respuestaTexto, Toast.LENGTH_LONG).show()

                        reproducirAudio(audioResponse.audio)
                        notificarReproduccion()

                        Preferencias.guardarValorString("audioCancionActual", audioResponse.audio)
                        guardarDatoscCancion(id)
                        actualizarIconoPlayPause()
                    }
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@NoizzysOtro, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@NoizzysOtro, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                    val errorMensaje = response.errorBody()?.string() ?: "Error desconocido"

                    // Mostrar en Logcat
                    Log.e("API_RESPONSE", "Error en la respuesta: Código ${response.code()} - $errorMensaje")

                }
            }

            override fun onFailure(call: Call<AudioResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)

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
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@NoizzysOtro, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@NoizzysOtro, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
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
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@NoizzysOtro, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@NoizzysOtro, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
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
                        val intent = Intent(this@NoizzysOtro, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@NoizzysOtro, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                    val errorMensaje = response.errorBody()?.string() ?: "Error desconocido"

                    // Mostrar en Logcat
                    Log.e("API_RESPONSE", "Error en la respuesta: Código ${response.code()} - $errorMensaje")

                    // Mostrar en Toast
                    Toast.makeText(this@NoizzysOtro, "Error: $errorMensaje", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<CancionInfoResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)

                // Mostrar en Toast
                Toast.makeText(this@NoizzysOtro, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        WebSocketEventHandler.registrarListenerNoizzy(listenerNoizzy)
        val intent = Intent(this, MusicPlayerService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        WebSocketEventHandler.eliminarListenerNoizzy(listenerNoizzy)
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
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

    override fun onDestroy() {
        super.onDestroy()
        WebSocketEventHandler.eliminarListenerNovedad(listenerNovedad)
        WebSocketEventHandler.eliminarListenerSeguidor(listenerSeguidor)
        WebSocketEventHandler.eliminarListenerInvitacion(listenerInvitacion)
        WebSocketEventHandler.eliminarListenerInteraccion(listenerInteraccion)
    }

    override fun onResume() {
        super.onResume()
        cargarNoizzys()
    }

}