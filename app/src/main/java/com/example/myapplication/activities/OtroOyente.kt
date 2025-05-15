package com.example.myapplication.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
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
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.myapplication.Adapters.Buscador.PerfilAdapter
import com.example.myapplication.Adapters.OtroOyente.PlaylistOtroOyenteAdapter
import com.example.myapplication.Adapters.Seguidores.SeguidoresAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.AudioColeccionRequest
import com.example.myapplication.io.request.AudioRequest
import com.example.myapplication.io.request.ChangeFollowRequest
import com.example.myapplication.io.response.AddReproduccionResponse
import com.example.myapplication.io.response.AudioResponse
import com.example.myapplication.io.response.CancionInfoResponse
import com.example.myapplication.io.response.CancionesArtistaResponse
import com.example.myapplication.io.response.GetDatosOyenteResponse
import com.example.myapplication.io.response.GetPlaylistOyenteResponse
import com.example.myapplication.io.response.InfoSeguidoresResponse
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.InvitacionPlaylist
import com.example.myapplication.io.response.Novedad
import com.example.myapplication.io.response.Seguidor
import com.example.myapplication.io.response.Noizzy
import com.example.myapplication.managers.ReproduccionTracker
import com.example.myapplication.services.MusicPlayerService
import com.example.myapplication.services.WebSocketEventHandler
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OtroOyente : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var btnFollow: Button
    private lateinit var allNoizzys: Button
    private lateinit var usernameText: TextView
    private lateinit var profileImage: ImageView
    private lateinit var fotoPerfil: ImageView
    private lateinit var cvLastNoizzy: CardView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlaylistOtroOyenteAdapter
    private lateinit var dot: View
    private var yaRedirigidoAlLogin = false
    private var seguidores : String = ""

    private var isFollowing = false

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

    private lateinit var perfilNoizzy: ImageView
    private lateinit var userNoizzy: TextView
    private lateinit var contentNoizzy: TextView
    private lateinit var cancionNoizzy: LinearLayout
    private lateinit var fotoCancion: ImageView
    private lateinit var textoCancion: TextView
    private lateinit var textoArtista: TextView
    private lateinit var numLikes: TextView
    private lateinit var numComments: TextView
    private lateinit var btnLike: ImageView
    private lateinit var btnComment: ImageView
    private var idLastNoizzy: Int = 0

    private var nombreUser: String? = null


    private val listenerNoizzy: (Noizzy, Boolean) -> Unit = { noizzy, mio ->
        runOnUiThread {
            Log.d("LOGS_NOTIS", "evento en perfil otro")
            if (nombreUser != null) {
                if (noizzy.nombreUsuario == nombreUser) {
                    idLastNoizzy = noizzy.id
                    Glide.with(this@OtroOyente)
                        .load(noizzy.fotoPerfil)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(perfilNoizzy)

                    userNoizzy.text = noizzy.nombreUsuario
                    contentNoizzy.text = noizzy.texto

                    if (noizzy.cancion != null) {
                        cancionNoizzy.visibility = View.VISIBLE
                        Glide.with(this@OtroOyente)
                            .load(noizzy.cancion.fotoPortada)
                            .placeholder(R.drawable.no_cancion)
                            .error(R.drawable.no_cancion)
                            .into(fotoCancion)

                        val idcancion = noizzy.cancion.id
                        cancionNoizzy.setOnClickListener{
                            reproducir(idcancion.toString())
                        }

                        textoCancion.text = noizzy.cancion.nombre
                        textoArtista.text = noizzy.cancion.nombreArtisticoArtista
                    } else {
                        cancionNoizzy.visibility = View.GONE
                    }

                    numLikes.text = noizzy.num_likes.toString()
                    numComments.text = noizzy.num_comentarios.toString()

                    Glide.with(this@OtroOyente)
                        .load(R.drawable.like_noizzy)
                        .placeholder(R.drawable.no_cancion)
                        .error(R.drawable.no_cancion)
                        .into(btnLike)

                }
            }
        }
    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.perfil_otro_oyente)

        apiService = ApiService.create()

        indexActual = Preferencias.obtenerValorEntero("indexColeccionActual", 0)

        nombreUser = intent.getStringExtra("nombre")
        val imagenUrl = intent.getStringExtra("imagen")

        // Inicialización de vistas
        btnFollow = findViewById(R.id.btnFollow)
        allNoizzys = findViewById(R.id.allNoizzys)
        usernameText = findViewById(R.id.username)
        profileImage = findViewById(R.id.profileImage)
        recyclerView = findViewById(R.id.recyclerViewHeadersPlaylistsP)
        dot = findViewById<View>(R.id.notificationDot)

        val profileImageButton = findViewById<ImageButton>(R.id.profileImageButton)
        val profileImageUrl = Preferencias.obtenerValorString("fotoPerfil", "")
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

        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapter = PlaylistOtroOyenteAdapter(mutableListOf()){ playlist ->
            val intent = Intent(this, PlaylistDetail::class.java)
            intent.putExtra("nombre", playlist.nombre)
            intent.putExtra("imagen", playlist.fotoPortada)
            intent.putExtra("id", playlist.id)
            Log.d("MiAppPlaylist", "id mando ${playlist.id}")
            Log.d("Playlist", "Buscador -> Playlist")
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        perfilNoizzy = findViewById(R.id.noizzyProfileImage)
        userNoizzy = findViewById(R.id.noizzyUserName)
        contentNoizzy = findViewById(R.id.noizzyContent)
        cancionNoizzy = findViewById(R.id.cancionNoizzy)
        fotoCancion = findViewById(R.id.recuerdoImage)
        textoCancion = findViewById(R.id.recuerdoText1)
        textoArtista = findViewById(R.id.recuerdoText2)
        numLikes = findViewById(R.id.likeCount)
        numComments = findViewById(R.id.commentCount)
        btnLike = findViewById(R.id.likeButton)
        btnComment = findViewById(R.id.commentButton)


        // Evento de clic para el botón de seguir
        btnFollow.setOnClickListener {
            // Cambiar el estado de seguir/no seguir
            isFollowing = !isFollowing
            updateFollowButtonState()
            val followersTextView = findViewById<TextView>(R.id.followers)
            // Aquí puedes añadir la lógica para realizar una acción, como seguir al oyente en la base de datos
            if (isFollowing) {
                if (nombreUser != null) {
                    onFollowStatusChanged(nombreUser!!,true)
                    val seg = seguidores.toInt() +1
                    seguidores = seg.toString()
                    followersTextView.text = "$seguidores Seguidores"
                }
            } else {
                if (nombreUser != null) {
                    onFollowStatusChanged(nombreUser!!,false)
                    val seg = seguidores.toInt() -1
                    seguidores = seg.toString()
                    followersTextView.text = "$seguidores Seguidores"
                }
            }
        }

        allNoizzys.setOnClickListener {
            val intent = Intent(this, NoizzysOtro::class.java)
            intent.putExtra("nombreUsuario", nombreUser)
            startActivity(intent)
        }

        if (nombreUser != null) {
            getDatosOyente(nombreUser!!)
            getPlaylistOyente(nombreUser!!)
        }

        progressBar = findViewById(R.id.progressBar)
        updateMiniReproductor()
        setupNavigation()
    }

    private fun getDatosOyente(nombreUser: String) {
        Log.d("OtroOyente", "1")
        val token = Preferencias.obtenerValorString("token", "")
        apiService.getDatosOyente("Bearer $token", nombreUser).enqueue(object : Callback<GetDatosOyenteResponse> {
            override fun onResponse(call: Call<GetDatosOyenteResponse>, response: Response<GetDatosOyenteResponse>) {
                Log.d("OtroOyente", "1")
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            val nombreperfil = it.oyente.nombreUsuario
                            seguidores = it.oyente.numSeguidores
                            val seguidos = it.oyente.numSeguidos

                            var foto: Any
                            if (it.oyente.fotoPerfil == "DEFAULT" || it.oyente.fotoPerfil.isNullOrEmpty()) {
                                foto = R.drawable.ic_profile
                            } else {
                                foto = it.oyente.fotoPerfil
                            }

                            val siguiendo = it.oyente.siguiendo


                            Log.d("OtroOyente", "ha tomado info: $nombreperfil")
                            Log.d("OtroOyente", "ha tomado info: $seguidores")
                            Log.d("OtroOyente", "ha tomado info: $seguidos")

                            val usernameTextView = findViewById<TextView>(R.id.username)
                            val followersTextView = findViewById<TextView>(R.id.followers)
                            val followingTextView = findViewById<TextView>(R.id.following)
                            val fotoImageView = findViewById<ImageView>(R.id.profileImage)

                            isFollowing = siguiendo
                            updateFollowButtonState()

                            usernameTextView.text = nombreperfil
                            followersTextView.text = "$seguidores Seguidores"
                            followingTextView.text = "$seguidos Seguidos"

                            // Cargar la imagen con Glide
                            Glide.with(this@OtroOyente)
                                .load(foto)
                                .placeholder(R.drawable.ic_profile) // Imagen por defecto mientras carga
                                .error(R.drawable.ic_profile) // Imagen por defecto si hay error
                                .circleCrop() // Para que la imagen sea circular
                                .into(fotoImageView)

                            //ÚLTIMO NOIZZY
                            idLastNoizzy = it.ultimoNoizzy.id
                            Glide.with(this@OtroOyente)
                                .load(it.oyente.fotoPerfil)
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_profile)
                                .circleCrop()
                                .into(perfilNoizzy)

                            userNoizzy.text = it.oyente.nombreUsuario
                            contentNoizzy.text = it.ultimoNoizzy.texto

                            if (it.ultimoNoizzy.cancion != null) {
                                cancionNoizzy.visibility = View.VISIBLE
                                Glide.with(this@OtroOyente)
                                    .load(it.ultimoNoizzy.cancion.fotoPortada)
                                    .placeholder(R.drawable.no_cancion)
                                    .error(R.drawable.no_cancion)
                                    .into(fotoCancion)

                                val idcancion = it.ultimoNoizzy.cancion.id
                                cancionNoizzy.setOnClickListener{
                                    reproducir(idcancion.toString())
                                }

                                textoCancion.text = it.ultimoNoizzy.cancion.nombre
                                textoArtista.text = it.ultimoNoizzy.cancion.nombreArtisticoArtista
                            } else {
                                cancionNoizzy.visibility = View.GONE
                            }

                            numLikes.text = it.ultimoNoizzy.num_likes.toString()
                            numComments.text = it.ultimoNoizzy.num_comentarios.toString()

                            if (it.ultimoNoizzy.like) {
                                Glide.with(this@OtroOyente)
                                    .load(R.drawable.like_noizzy_selected)
                                    .placeholder(R.drawable.no_cancion)
                                    .error(R.drawable.no_cancion)
                                    .into(btnLike)
                            }
                            btnLike.setOnClickListener { /*darLike()*/  }
                            btnComment.setOnClickListener { /*comentar()*/ }
                        } else {
                            handleErrorCode(it.respuestaHTTP)
                        }
                    } ?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@OtroOyente, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@OtroOyente, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<GetDatosOyenteResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }
    fun onFollowStatusChanged(userId: String, isFollowing: Boolean) {
        val token = Preferencias.obtenerValorString("token", "") ?: ""
        val authHeader = "Bearer $token"
        val request = ChangeFollowRequest(isFollowing, userId)

        apiService.changeFollow(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    val message = if (isFollowing) "Ahora sigues a este usuario" else "Dejaste de seguir al usuario"
                    Toast.makeText(this@OtroOyente, message, Toast.LENGTH_SHORT).show()
                } else {
                    // Revertir el cambio si falla la API
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@OtroOyente, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@OtroOyente, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Revertir el cambio si falla la conexión
                Log.e("API Error", "Error en change-follow", t)
                Toast.makeText(this@OtroOyente, "Fallo de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getPlaylistOyente(nombreUsuario: String) {
        Log.d("otroOyente1", "ENTRA PLAYLIST")
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.getPlaylistOyente(authHeader,nombreUsuario).enqueue(object : Callback<GetPlaylistOyenteResponse> {
            override fun onResponse(call: Call<GetPlaylistOyenteResponse>, response: Response<GetPlaylistOyenteResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val num = response.body()?.n_playlists
                    val playlist = response.body()?.playlists
                    val publicPlaylists = findViewById<TextView>(R.id.publicPlaylists)
                    publicPlaylists.text = "$num playlist públicas"
                    Log.d("otroOyente1", "HAY ${num}")
                    if (playlist != null) {
                        Log.d("otroOyente1", "HAY playlist")
                        adapter.submitList(playlist)

                    }else{
                        Log.d("otroOyente1", "NO HAY playlist")
                    }
                }else{
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@OtroOyente, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@OtroOyente, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<GetPlaylistOyenteResponse>, t: Throwable) {
                Log.d("otroOyente", "error conexion")
            }
        })
    }


    // Método para actualizar el estado del botón de seguir
    private fun updateFollowButtonState() {
        if (isFollowing) {
            btnFollow.text = "Dejar de seguir"
        } else {
            btnFollow.text = "Seguir"
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

        val  minirep = findViewById<LinearLayout>(R.id.miniPlayer)
        minirep.setOnClickListener{
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
                    ReproduccionTracker.pauseTracking()
                    stopButton.setImageResource(R.drawable.ic_pause)
                    Log.d("MiniReproductor", "Canción pausada en $progreso ms")
                } else {
                    Log.d("MiniReproductor", "Intentando reanudar la canción...")
                    service.resume()
                    stopButton.setImageResource(R.drawable.ic_play)
                    ReproduccionTracker.resumeTracking()
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
                        ReproduccionTracker.startTracking(this@OtroOyente, id) {
                            notificarReproduccion()
                        }

                        Preferencias.guardarValorString("audioCancionActual", audioResponse.audio)
                        guardarDatoscCancion(id)
                        actualizarIconoPlayPause()
                    }
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@OtroOyente, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@OtroOyente, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
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
                        ReproduccionTracker.startTracking(this@OtroOyente, ordenColeccion[indice]) {
                            notificarReproduccion()
                        }
                        guardarDatoscCancion(ordenColeccion[indice])
                        actualizarIconoPlayPause()
                    }
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@OtroOyente, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@OtroOyente, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
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

        apiService.addReproduccion(authHeader).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("MiApp", "Reproducción registrada exitosamente")
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@OtroOyente, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@OtroOyente, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("MiApp", "Error al registrar la reproducción")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
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
                        val intent = Intent(this@OtroOyente, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@OtroOyente, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                    val errorMensaje = response.errorBody()?.string() ?: "Error desconocido"

                    // Mostrar en Logcat
                    Log.e("API_RESPONSE", "Error en la respuesta: Código ${response.code()} - $errorMensaje")

                    // Mostrar en Toast
                    Toast.makeText(this@OtroOyente, "Error: $errorMensaje", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<CancionInfoResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)

                // Mostrar en Toast
                Toast.makeText(this@OtroOyente, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
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
        updateMiniReproductor()
    }
}
