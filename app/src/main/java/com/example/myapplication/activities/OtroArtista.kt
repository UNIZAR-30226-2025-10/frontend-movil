package com.example.myapplication.activities

import android.annotation.SuppressLint
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
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.myapplication.Adapters.Buscador.CancionAdapter
import com.example.myapplication.R
import com.example.myapplication.Adapters.OtroArtista.CancionesArtistaAdapter
import com.example.myapplication.Adapters.OtroArtista.CancionesPopularesAdapter
import com.example.myapplication.Adapters.OtroArtista.DiscografiaDiscosAdapter
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.ActualizarFavoritoRequest
import com.example.myapplication.io.request.AudioColeccionRequest
import com.example.myapplication.io.request.AudioRequest
import com.example.myapplication.io.request.ChangeFollowRequest
import com.example.myapplication.io.response.AddReproduccionResponse
import com.example.myapplication.io.response.Artista
import com.example.myapplication.io.response.AudioResponse
import com.example.myapplication.io.response.CancionInfoResponse
import com.example.myapplication.io.response.CancionesArtistaResponse
import com.example.myapplication.io.response.DatosArtista
import com.example.myapplication.io.response.DatosArtistaResponse
import com.example.myapplication.io.response.DiscografiaAlbumArtistaResponse
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.InvitacionPlaylist
import com.example.myapplication.io.response.Novedad
import com.example.myapplication.io.response.NumFavoritasArtistaResponse
import com.example.myapplication.io.response.PopularesArtistaResponse
import com.example.myapplication.io.response.Seguidor
import com.example.myapplication.managers.ReproduccionTracker
import com.example.myapplication.services.MusicPlayerService
import com.example.myapplication.services.WebSocketEventHandler
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OtroArtista : AppCompatActivity() {

    private lateinit var nombreArtistico: TextView
    private lateinit var biografia: TextView
    private lateinit var seguidores: TextView
    private lateinit var fotoPerfil: ImageView
    private lateinit var fotoPerfilFavoritos: ImageButton
    private lateinit var numCanciones: TextView
    private lateinit var artistaLike: TextView
    private lateinit var apiService: ApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAlbumes: RecyclerView
    private lateinit var recyclerViewCanciones: RecyclerView
    private lateinit var cancionesAdapter: CancionesPopularesAdapter
    private lateinit var albumesAdapter: DiscografiaDiscosAdapter
    private lateinit var cancionesArtistaAdapter: CancionesArtistaAdapter
    private lateinit var btnFollow: Button
    private lateinit var allNoizzys: Button
    private var artista:  DatosArtista? = null
    private lateinit var dot: View
    private var yaRedirigidoAlLogin = false

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

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.otro_artista)

        apiService = ApiService.create()

        indexActual = Preferencias.obtenerValorEntero("indexColeccionActual", 0)

        // Inicializar las vistas
        nombreArtistico = findViewById(R.id.artisticname)
        biografia = findViewById(R.id.biografia)
        seguidores = findViewById(R.id.followers)
        fotoPerfil = findViewById(R.id.profileImage)
        fotoPerfilFavoritos =  findViewById(R.id.profileImage2)
        recyclerView = findViewById(R.id.recyclerViewPopulares)
        recyclerViewCanciones = findViewById(R.id.recyclerViewCanciones)
        recyclerViewAlbumes = findViewById(R.id.recyclerViewDiscografia)
        numCanciones = findViewById(R.id.numCanciones)
        artistaLike = findViewById(R.id.artistaLike)
        btnFollow = findViewById(R.id.seguir)
        allNoizzys = findViewById(R.id.allNoizzys)
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



        // Obtener el nombre de usuario del intent
        val nombreUsuario = intent.getStringExtra("nombreUsuario") ?: ""

        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        cancionesAdapter = CancionesPopularesAdapter(
            { cancion ->
                val cancionId = Preferencias.obtenerValorString("cancionActualId", "")
                if (cancionId == cancion.id) {
                    startActivity(Intent(this, CancionReproductorDetail::class.java))
                } else {
                    reproducir(cancion.id)
                }
            },
            { cancion, isFavorite, position ->
                actualizarFavorito(cancion.id, isFavorite, position, nombreUsuario)
            }
        )
        recyclerView.adapter = cancionesAdapter

        recyclerViewCanciones.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewAlbumes.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        cancionesArtistaAdapter = CancionesArtistaAdapter{ cancion ->
            val cancionId = Preferencias.obtenerValorString("cancionActualId", "")
            if(cancionId == cancion.id.toString()){
                startActivity(Intent(this, CancionReproductorDetail::class.java))
            }
            else {
                reproducir(cancion.id.toString())
            }
        }
        albumesAdapter = DiscografiaDiscosAdapter { album ->
            // Intent para abrir AlbumDetail
            val intent = Intent(this, AlbumDetail::class.java).apply {
                Log.d("Album", "Id: ${album.id}")
                putExtra("id", album.id.toString())
            }
            startActivity(intent)
        }

        recyclerViewCanciones.adapter = cancionesArtistaAdapter
        recyclerViewAlbumes.adapter = albumesAdapter

        fotoPerfilFavoritos.setOnClickListener {
            val nombreArtista = nombreArtistico.text.toString()
            val nombreUsuario = intent.getStringExtra("nombreUsuario") ?: ""

            val intent = Intent(this, CancionesFavoritasArtista::class.java).apply {
                putExtra("nombreArtista", nombreArtista)
                putExtra("nombreUsuario", nombreUsuario)
            }
            startActivity(intent)
        }

        btnFollow.setOnClickListener {
            // Cambiar el estado de seguir/no seguir
            isFollowing = !isFollowing
            updateFollowButtonState()
            // Aquí puedes añadir la lógica para realizar una acción, como seguir al oyente en la base de datos
            if (isFollowing) {
                if (nombreUsuario != null) {
                    artista?.numSeguidores = (artista?.numSeguidores ?: 0) + 1
                    onFollowStatusChanged(nombreUsuario,true)
                }

            } else {
                if (nombreUsuario != null) {
                    artista?.numSeguidores = maxOf(0, (artista?.numSeguidores ?: 1) - 1)
                    onFollowStatusChanged(nombreUsuario,false)
                }
            }
            seguidores.text = "${artista?.numSeguidores} Seguidores"
        }

        allNoizzys.setOnClickListener {
            val intent = Intent(this, NoizzysOtro::class.java)
            intent.putExtra("nombreUsuario", nombreUsuario)
            startActivity(intent)
        }

        val radioGroupDiscografia = findViewById<RadioGroup>(R.id.radioGroupDiscografia)
        radioGroupDiscografia.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.cancionesArtista -> {
                    // Mostrar las canciones y ocultar los álbumes
                    recyclerViewCanciones.visibility = View.VISIBLE
                    recyclerViewAlbumes.visibility = View.GONE
                    obtenerCanciones(nombreUsuario)  // Llamar para obtener canciones
                }
                R.id.discosEPs -> {
                    // Mostrar los álbumes y ocultar las canciones
                    recyclerViewCanciones.visibility = View.GONE
                    recyclerViewAlbumes.visibility = View.VISIBLE
                    obtenerAlbumesDelArtista(nombreUsuario)  // Llamar para obtener álbumes
                }
            }
        }

        // Llamar a la API para obtener los datos del artista
        getDatosArtista(nombreUsuario)
        getCancionesPopulares(nombreUsuario)
        getNumFavoritas(nombreUsuario)
        findViewById<RadioGroup>(R.id.radioGroupDiscografia).check(R.id.cancionesArtista)
        recyclerViewCanciones.visibility = View.VISIBLE
        recyclerViewAlbumes.visibility = View.GONE
        obtenerCanciones(nombreUsuario)

        progressBar = findViewById(R.id.progressBar)
        updateMiniReproductor()
        setupNavigation()
    }

    private fun getDatosArtista(nombreUsuario: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.getDatosArtista(authHeader,nombreUsuario).enqueue(object : Callback<DatosArtistaResponse> {
            override fun onResponse(
                call: Call<DatosArtistaResponse>,
                response: Response<DatosArtistaResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    artista = response.body()?.artista
                    val siguiendo = artista?.siguiendo

                    // Mostrar la información en los TextViews
                    nombreArtistico.text = artista?.nombreArtistico
                    artistaLike.text = " De ${artista?.nombreArtistico}"
                    biografia.text = artista?.biografia
                    seguidores.text = "${artista?.numSeguidores} Seguidores"

                    if (siguiendo != null) {
                        isFollowing = siguiendo
                    }
                    updateFollowButtonState()

                    // Cargar la imagen usando Glide
                    val foto = artista?.fotoPerfil
                    if (!foto.isNullOrEmpty()) {
                        Glide.with(this@OtroArtista)
                            .load(foto)
                            .placeholder(R.drawable.ic_profile) // Imagen por defecto mientras carga
                            .error(R.drawable.ic_profile) // Imagen por defecto si hay error
                            .circleCrop() // Para que la imagen sea circular
                            .into(fotoPerfil)
                    }
                    if (!foto.isNullOrEmpty()) {
                        Glide.with(this@OtroArtista)
                            .load(foto)
                            .placeholder(R.drawable.ic_profile) // Imagen por defecto mientras carga
                            .error(R.drawable.ic_profile) // Imagen por defecto si hay error
                            .circleCrop() // Para que la imagen sea circular
                            .into(fotoPerfilFavoritos)
                    }
                    artista?.nombreArtistico?.let { nombreArtista ->
                        // Actualizar el nombre artístico en el adapter
                        albumesAdapter.actualizarNombreArtista(nombreArtista)
                        cancionesArtistaAdapter.actualizarNombreArtista(nombreArtista)
                        cancionesAdapter.actualizarNombreArtista(nombreArtista)
                    }

                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@OtroArtista, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@OtroArtista, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                    Toast.makeText(this@OtroArtista, "Error al obtener los datos del artista", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<DatosArtistaResponse>, t: Throwable) {
                Toast.makeText(this@OtroArtista, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun getCancionesPopulares(nombreUsuario: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.getCancionesPopulares(authHeader,nombreUsuario).enqueue(object : Callback<PopularesArtistaResponse> {
            override fun onResponse(call: Call<PopularesArtistaResponse>, response: Response<PopularesArtistaResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val cancionesResponse = response.body()!!
                    val canciones = cancionesResponse.canciones_populares

                    // Pasar las canciones al adaptador
                    cancionesAdapter.submitList(canciones)
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@OtroArtista, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@OtroArtista, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                    Toast.makeText(this@OtroArtista, "Error al obtener las canciones populares", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PopularesArtistaResponse>, t: Throwable) {
                Toast.makeText(this@OtroArtista, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getNumFavoritas(nombreUsuario: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.getNumeroCancionesFavoritas(authHeader,nombreUsuario).enqueue(object : Callback<NumFavoritasArtistaResponse> {
            override fun onResponse(call: Call<NumFavoritasArtistaResponse>, response: Response<NumFavoritasArtistaResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val totalFavoritas = response.body()!!.total_favoritas
                    if(totalFavoritas == 1){
                        numCanciones.text = "Te gusta ${totalFavoritas} canción"
                    }else{
                        numCanciones.text = "Te gustan ${totalFavoritas} canciones"
                    }
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@OtroArtista, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@OtroArtista, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<NumFavoritasArtistaResponse>, t: Throwable) {
                Toast.makeText(this@OtroArtista, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun obtenerAlbumesDelArtista(nombreUsuario: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.albumesArtista(authHeader,nombreUsuario).enqueue(object : Callback<DiscografiaAlbumArtistaResponse> {
            override fun onResponse(call: Call<DiscografiaAlbumArtistaResponse>, response: Response<DiscografiaAlbumArtistaResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    // Obtener los álbumes de la respuesta
                    val albumes = response.body()?.albumes

                    // Verificar que los álbumes no sean nulos
                    if (!albumes.isNullOrEmpty()) {
                        // Pasar los álbumes al adaptador
                        albumesAdapter.submitList(albumes)
                    } else {
                        Toast.makeText(this@OtroArtista, "No se encontraron álbumes para este artista.", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@OtroArtista, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@OtroArtista, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<DiscografiaAlbumArtistaResponse>, t: Throwable) {
                Toast.makeText(this@OtroArtista, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun obtenerCanciones(nombreUsuario: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.cancionesArtista(authHeader,nombreUsuario).enqueue(object : Callback<CancionesArtistaResponse> {
            override fun onResponse(call: Call<CancionesArtistaResponse>, response: Response<CancionesArtistaResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val canciones = response.body()?.canciones
                    if (canciones != null) {
                        Log.d("GoToArtist", "HAY CANCIONES")
                        cancionesArtistaAdapter.submitList(canciones)
                    }else{
                        Log.d("GoToArtist", "NO HAY CANCIONES")
                    }
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@OtroArtista, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@OtroArtista, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<CancionesArtistaResponse>, t: Throwable) {
                Toast.makeText(this@OtroArtista, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun actualizarFavorito(id: String, fav: Boolean, position: Int, nombreUsuario: String) {
        val request = ActualizarFavoritoRequest(id, fav)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.actualizarFavorito(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (!response.isSuccessful) {
                    cancionesAdapter.updateFavoriteState(position, !fav)
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@OtroArtista, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@OtroArtista, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                }
                // Actualizar contador
                getNumFavoritas(nombreUsuario)
                Log.d("FavoritoArtista", "Cambiado fav con exito")
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Revertir si hay error
                cancionesAdapter.updateFavoriteState(position, !fav)
                Log.d("FavoritoArtista", "Fallo al conectar fav artista")
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
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@OtroArtista, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@OtroArtista, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Revertir el cambio si falla la conexión
                Log.e("API Error", "Error en change-follow", t)
            }
        })
    }

    private fun updateFollowButtonState() {
        if (isFollowing) {
            btnFollow.text = "Dejar de seguir"
        } else {
            btnFollow.text = "Seguir"
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
                        ReproduccionTracker.startTracking(this@OtroArtista, id) {
                            notificarReproduccion()
                        }

                        Preferencias.guardarValorString("audioCancionActual", audioResponse.audio)
                        guardarDatoscCancion(id)
                        actualizarIconoPlayPause()
                    }
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@OtroArtista, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@OtroArtista, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
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
                        ReproduccionTracker.startTracking(this@OtroArtista, ordenColeccion[indice]) {
                            notificarReproduccion()
                        }
                        guardarDatoscCancion(ordenColeccion[indice])
                        actualizarIconoPlayPause()
                    }
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@OtroArtista, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@OtroArtista, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
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
                        val intent = Intent(this@OtroArtista, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@OtroArtista, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
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
                        val intent = Intent(this@OtroArtista, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@OtroArtista, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                    val errorMensaje = response.errorBody()?.string() ?: "Error desconocido"

                    // Mostrar en Logcat
                    Log.e("API_RESPONSE", "Error en la respuesta: Código ${response.code()} - $errorMensaje")

                    // Mostrar en Toast
                    Toast.makeText(this@OtroArtista, "Error: $errorMensaje", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<CancionInfoResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)

                // Mostrar en Toast
                Toast.makeText(this@OtroArtista, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
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