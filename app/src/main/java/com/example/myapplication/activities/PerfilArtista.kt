package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TypefaceSpan
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.myapplication.Adapters.Buscador.AlbumAdapter
import com.example.myapplication.Adapters.Home.EscuchasAdapter
import com.example.myapplication.Adapters.Home.PlaylistsAdapter
import com.example.myapplication.Adapters.Perfil.TopArtistasAdapter
import com.example.myapplication.Adapters.PerfilArtista.AlbumsAdapter
import com.example.myapplication.Adapters.PerfilArtista.CancionesAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.CloudinaryApiService
import com.example.myapplication.io.request.AudioColeccionRequest
import com.example.myapplication.io.request.AudioRequest
import com.example.myapplication.io.request.ChangePasswordRequest
import com.example.myapplication.io.request.ClaroRequest
import com.example.myapplication.io.request.EditarPerfilArtistaRequest
import com.example.myapplication.io.request.EditarPerfilRequest
import com.example.myapplication.io.response.*
import com.example.myapplication.managers.ReproduccionTracker
import com.example.myapplication.services.MusicPlayerService
import com.example.myapplication.services.WebSocketEventHandler
import com.example.myapplication.utils.Preferencias
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PerfilArtista : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var apiServiceCloud: CloudinaryApiService
    private lateinit var recyclerViewAlbums: RecyclerView
    private lateinit var recyclerViewCanciones: RecyclerView
    private lateinit var albumAdapter: AlbumsAdapter
    private lateinit var cancionesAdapter: CancionesAdapter
    private lateinit var usernameTextView: TextView
    private lateinit var artisticnameTextView: TextView
    private lateinit var biografiaArtistaTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var profileImageButton: ImageView
    private lateinit var artistasAdapter: TopArtistasAdapter
    private lateinit var escuchasAdapter: EscuchasAdapter
    private lateinit var recyclerViewTopArtistas: RecyclerView
    private lateinit var recyclerViewEscuchas: RecyclerView
    private lateinit var headerTopArtistaTextView: TextView
    private lateinit var headerEscuchasTextView: TextView
    private lateinit var recyclerViewPlaylists: RecyclerView
    private lateinit var headerPlaylistsTextView: TextView
    private lateinit var headerCancionesTextView: TextView
    private lateinit var headerAlbumesTextView: TextView
    private lateinit var playlistsAdapter: PlaylistsAdapter
    private var imageUri: Uri? = null
    private var profileImageViewDialog: ImageView? = null
    private var yaRedirigidoAlLogin = false
    private lateinit var dot: View

    private val openGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            // Actualizar el ImageView del diálogo con Glide y circleCrop
            Glide.with(this)
                .load(it)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(profileImageViewDialog!!)
        }
    }

    private lateinit var switchMode: SwitchCompat

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

    @SuppressLint("WrongViewCast", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.perfil_artista)

        apiService = ApiService.create()
        apiServiceCloud = CloudinaryApiService.create()

        indexActual = Preferencias.obtenerValorEntero("indexColeccionActual", 0)

        // Inicializar vistas
        usernameTextView = findViewById(R.id.username)
        artisticnameTextView = findViewById(R.id.artisticname)
        biografiaArtistaTextView = findViewById(R.id.biografiaArtista)
        profileImageButton = findViewById(R.id.profileImageButton)
        profileImageView = findViewById(R.id.profileImage)
        recyclerViewTopArtistas = findViewById(R.id.recyclerViewTopArtistas)
        recyclerViewEscuchas = findViewById(R.id.recyclerViewCancionesReciente)
        headerPlaylistsTextView = findViewById(R.id.textViewHeadersPlaylistsP)
        headerPlaylistsTextView.visibility = View.INVISIBLE

        // Cargar datos
        loadProfileImage()
        loadProfileData()
        //loadArtistAlbums()

        val followers: TextView = findViewById(R.id.followers)
        val following: TextView = findViewById(R.id.following)

        followers.setOnClickListener {
            //Toast.makeText(this, "Abrir lista de seguidores", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Seguidores::class.java))
        }

        following.setOnClickListener {
            //Toast.makeText(this, "Abrir lista de seguidos", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Seguidos::class.java))
        }

        headerTopArtistaTextView = findViewById(R.id.textViewHeadersTopArtistas)
        headerTopArtistaTextView.visibility = View.GONE

        headerCancionesTextView = findViewById(R.id.textViewHeadersCanciones)
        headerCancionesTextView.visibility = View.GONE

        headerAlbumesTextView = findViewById(R.id.textViewHeadersAlbums)
        headerAlbumesTextView.visibility = View.GONE

        // Configurar RecyclerView para álbumes
        recyclerViewAlbums = findViewById(R.id.recyclerViewAlbums)
        recyclerViewAlbums.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        albumAdapter = AlbumsAdapter(mutableListOf()) { album ->
            val intent = Intent(this, EstadisticasAlbum::class.java)
            intent.putExtra("id", album.id)
            startActivity(intent)
        }
        recyclerViewAlbums.adapter = albumAdapter
        headerTopArtistaTextView = findViewById(R.id.textViewHeadersTopArtistas)
        artistasAdapter = TopArtistasAdapter(mutableListOf()) { artista ->
            val intent = Intent(this, OtroArtista::class.java)
            intent.putExtra("nombreUsuario", artista.nombreUsuario)
            intent.putExtra("nombreArtistico", artista.nombreArtistico)
            startActivity(intent)
            Log.d("Click", "Artista seleccionado: ${artista.nombreArtistico}")
        }

        recyclerViewTopArtistas.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewTopArtistas.adapter = artistasAdapter

        getHistorialArtistas()


        headerEscuchasTextView = findViewById(R.id.textViewHeadersCancionesReciente)
        headerEscuchasTextView.visibility = View.GONE
        escuchasAdapter = EscuchasAdapter(mutableListOf()) { escucha ->
            val cancionId = Preferencias.obtenerValorString("cancionActualId", "")
            if(cancionId == escucha.id){
                startActivity(Intent(this, CancionReproductorDetail::class.java))
            }
            else {
                reproducir(escucha.id)
            }
        }
        recyclerViewEscuchas.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewEscuchas.adapter = escuchasAdapter
        getHistorialEscuchas()

        /*cancionesAdapter = CancionesAdapter(mutableListOf(),nombreUsuario) { cancion ->
            val cancionId = Preferencias.obtenerValorString("cancionActualId", "")
            if(cancionId == cancion.id){
                startActivity(Intent(this@PerfilArtista, CancionReproductorDetail::class.java))
            }
            else {
                reproducir(cancion.id)
            }
        }
        recyclerViewCanciones.adapter = cancionesAdapter*/


        recyclerViewPlaylists = findViewById(R.id.recyclerViewPlaylistsP)
        recyclerViewPlaylists.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        playlistsAdapter = PlaylistsAdapter(mutableListOf()){ playlist ->
            val intent = Intent(this, PlaylistDetail::class.java)
            intent.putExtra("nombre", playlist.nombre)
            intent.putExtra("imagen", playlist.fotoPortada)
            intent.putExtra("id", playlist.id)
            startActivity(intent)
        }
        recyclerViewPlaylists.adapter = playlistsAdapter
        getMisPlaylists()



        // Configurar listeners
        findViewById<Button>(R.id.subirCancion).setOnClickListener {
            startActivity(Intent(this, SubirCancion::class.java))
        }

        findViewById<Button>(R.id.subirAlbum).setOnClickListener {
            val intent = Intent(this, CrearAlbum::class.java)
            intent.putExtra("artista", artisticnameTextView.text)
            startActivity(intent)
        }


        val moreOptionsButton = findViewById<ImageButton>(R.id.options)

        moreOptionsButton.setOnClickListener {
            val popupMenu = PopupMenu(this, moreOptionsButton, Gravity.END, 0, R.style.PopupMenuStyle)
            popupMenu.menuInflater.inflate(R.menu.profile_options, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_edit_profile -> {
                        showEditProfileDialog()
                        true
                    }
                    R.id.menu_logout -> {
                        musicService?.pause()
                        startActivity(Intent(this, Logout::class.java))
                        true
                    }
                    R.id.menu_delete_account -> {
                        musicService?.pause()
                        showDeleteAccountDialog()
                        true
                    }
                    R.id.menu_change_password -> {
                        showChangePasswordDialog()
                        true
                    }
                    else -> false
                }
            }

            for (i in 0 until popupMenu.menu.size()) {
                val item = popupMenu.menu.getItem(i)
                val spanString = SpannableString(item.title)
                spanString.setSpan(
                    TypefaceSpan(ResourcesCompat.getFont(this, R.font.poppins_regular)!!),
                    0, spanString.length,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                item.title = spanString
            }

            popupMenu.show()
        }

        switchMode = findViewById(R.id.switchMode)

        val oscuroAct = Preferencias.obtenerValorEntero("modoOscuro", 1)
        if(oscuroAct == 0){
            switchMode.isChecked = true
        }
        else{
            switchMode.isChecked = false
        }

        Log.d("MiAppPerfil", "colorrrr 2")
        switchMode.setOnClickListener {
            if(switchMode.isChecked){
                Preferencias.guardarValorEntero("modoOscuro", 0)
                setDayNight(0)
            }
            else{
                Preferencias.guardarValorEntero("modoOscuro", 1)
                setDayNight(1)
            }
            cambiarModoOsucro()
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

        progressBar = findViewById(R.id.progressBar)
        updateMiniReproductor()
        setupNavigation()
    }

    private fun loadProfileImage() {
        val profileImageUrl = Preferencias.obtenerValorString("fotoPerfil", "DEFAULT")
        if (profileImageUrl.isNullOrEmpty() || profileImageUrl == "DEFAULT") {
            profileImageButton.setImageResource(R.drawable.ic_profile)
            profileImageView.setImageResource(R.drawable.ic_profile)
        } else {
            Glide.with(this)
                .load(profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(profileImageButton)
            Glide.with(this)
                .load(profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(profileImageView)
        }
        Preferencias.guardarValorString("profile_image", profileImageUrl)
    }

    private fun loadArtistAlbums() {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.getMisAlbumes(authHeader).enqueue(object : Callback<GetMisAlbumesResponse> {
            override fun onResponse(call: Call<GetMisAlbumesResponse>, response: Response<GetMisAlbumesResponse>) {
                Log.d("PERFIL_ARTISTA", "entra en on response Album")
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        if (responseBody.respuestaHTTP == 0) {
                            Log.d("PERFIL_ARTISTA", "entra en on response SUCCESS")
                            val misAlbums = responseBody.albumes
                            Log.d("PERFIL_ARTISTA", "misAlbums: $misAlbums")
                            Log.d("PERFIL_ARTISTA", "entra en on response 2")
                            // Actualizar el adaptador con los nuevos álbumes
                            albumAdapter.updateDataMisAlbums(misAlbums)
                            Log.d("PERFIL_ARTISTA", "entra en on response 3")
                            // Mostrar u ocultar el RecyclerView según si hay álbumes
                            if (misAlbums.isNotEmpty()) {
                                recyclerViewAlbums.visibility = View.VISIBLE
                                headerAlbumesTextView.visibility = View.VISIBLE

                            } else {
                                recyclerViewAlbums.visibility = View.GONE
                                headerAlbumesTextView.visibility = View.GONE
                                //showToast("No tienes álbumes aún")
                            }
                        } else {
                            handleErrorCode(responseBody.respuestaHTTP)
                        }
                    } ?: showToast("Error: Respuesta vacía del servidor")
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        val errorBody = response.errorBody()?.string()

                        try {
                            val json = JSONObject(errorBody ?: "")
                            val errorMessage = json.getString("error")

                            if (errorMessage == "Token inválido.") {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@PerfilArtista, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                showToast("Sesión iniciada en otro dispositivo")
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<GetMisAlbumesResponse>, t: Throwable) {
                showToast("Error de conexión: ${t.message}")
            }
        })


        apiService.getMisCanciones(authHeader).enqueue(object : Callback<GetMisCancionesResponse> {
            override fun onResponse(call: Call<GetMisCancionesResponse>, response: Response<GetMisCancionesResponse>) {
                Log.d("PERFIL_ARTISTA", "entra en on response Album")
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        if (responseBody.respuestaHTTP == 0) {
                            Log.d("PERFIL_ARTISTA", "entra en on response SUCCESS")
                            val misCanciones = responseBody.canciones
                            Log.d("PERFIL_ARTISTA", "misAlbums: $misCanciones")
                            Log.d("PERFIL_ARTISTA", "entra en on response 2")
                            // Actualizar el adaptador con los nuevos álbumes
                            cancionesAdapter.updateDataMisCanciones(misCanciones)
                            Log.d("PERFIL_ARTISTA", "entra en on response 3")

                            // Mostrar u ocultar el RecyclerView según si hay álbumes
                            if (misCanciones.isNotEmpty()) {
                                recyclerViewCanciones.visibility = View.VISIBLE
                                headerCancionesTextView.visibility = View.VISIBLE

                            } else {
                                recyclerViewCanciones.visibility = View.GONE
                                headerCancionesTextView.visibility = View.GONE
                                //showToast("No tienes álbumes aún")
                            }
                            //recyclerViewCanciones.visibility = View.VISIBLE

                        } else {
                            handleErrorCode(responseBody.respuestaHTTP)
                        }
                    } ?: showToast("Error: Respuesta vacía del servidor")
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        val errorBody = response.errorBody()?.string()

                        try {
                            val json = JSONObject(errorBody ?: "")
                            val errorMessage = json.getString("error")

                            if (errorMessage == "Token inválido.") {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@PerfilArtista, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                showToast("Sesión iniciada en otro dispositivo")
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<GetMisCancionesResponse>, t: Throwable) {
                showToast("Error de conexión: ${t.message}")
            }
        })
    }

    private fun loadProfileData() {
        val token = Preferencias.obtenerValorString("token", "")
        apiService.getMisDatosArtista("Bearer $token").enqueue(object : Callback<InfoPerfilArtistaResponse> {
            override fun onResponse(call: Call<InfoPerfilArtistaResponse>, response: Response<InfoPerfilArtistaResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            usernameTextView.text = it.nombre
                            artisticnameTextView.text = it.nombreArtistico
                            biografiaArtistaTextView.text = it.biografia
                            findViewById<TextView>(R.id.followers).text = "${it.numSeguidores} Seguidores"
                            findViewById<TextView>(R.id.following).text = "${it.numSeguidos} Seguidos"

                            val nombreUsuario = it.nombreArtistico
                            Log.d("CancionesAdapter", "user22: ${nombreUsuario}")
                            recyclerViewCanciones = findViewById(R.id.recyclerViewCanciones)
                            recyclerViewCanciones.layoutManager = LinearLayoutManager(this@PerfilArtista, LinearLayoutManager.HORIZONTAL, false)
                            cancionesAdapter = CancionesAdapter(mutableListOf(),nombreUsuario) { cancion ->
                                val cancionId = Preferencias.obtenerValorString("cancionActualId", "")
                                if(cancionId == cancion.id){
                                    startActivity(Intent(this@PerfilArtista, CancionReproductorDetail::class.java))
                                }
                                else {
                                    reproducir(cancion.id)
                                }
                            }
                            recyclerViewCanciones.adapter = cancionesAdapter
                            loadArtistAlbums()

                        } else {
                            handleErrorCode(it.respuestaHTTP)
                        }
                    } ?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        val errorBody = response.errorBody()?.string()

                        try {
                            val json = JSONObject(errorBody ?: "")
                            val errorMessage = json.getString("error")

                            if (errorMessage == "Token inválido.") {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@PerfilArtista, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                showToast("Sesión iniciada en otro dispositivo")
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<InfoPerfilArtistaResponse>, t: Throwable) {
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
                                headerPlaylistsTextView.visibility = View.VISIBLE
                            } else {
                                recyclerViewPlaylists.visibility = View.GONE
                                headerPlaylistsTextView.visibility = View.GONE
                            }

                        } else {
                            handleErrorCode(it.respuestaHTTP)
                        }
                    } ?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        val errorBody = response.errorBody()?.string()

                        try {
                            val json = JSONObject(errorBody ?: "")
                            val errorMessage = json.getString("error")

                            if (errorMessage == "Token inválido.") {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@PerfilArtista, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                showToast("Sesión iniciada en otro dispositivo")
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<PlaylistsResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun getHistorialArtistas() {
        val token = Preferencias.obtenerValorString("token", "")
        apiService.getHistorialArtistas("Bearer $token")
            .enqueue(object : Callback<HistorialArtistasResponse> {
                override fun onResponse(
                    call: Call<HistorialArtistasResponse>,
                    response: Response<HistorialArtistasResponse>
                ) {
                    Log.d("MiApp", "onResponse Historial Artistas")

                    if (response.isSuccessful) {
                        response.body()?.let {
                            if (it.respuestaHTTP == 0) {
                                val topArtistas = it.historial_artistas

                                if (topArtistas.isNotEmpty()) {
                                    artistasAdapter.updateData(topArtistas)
                                    recyclerViewTopArtistas.visibility = View.VISIBLE
                                    headerTopArtistaTextView.visibility = View.VISIBLE
                                } else {
                                    recyclerViewTopArtistas.visibility = View.GONE
                                    headerTopArtistaTextView.visibility = View.GONE
                                }
                            } else {
                                handleErrorCode(it.respuestaHTTP)
                            }
                        } ?: showToast("Datos incorrectos en la respuesta")
                    } else {
                        if (response.code() == 401 && !yaRedirigidoAlLogin) {
                            val errorBody = response.errorBody()?.string()

                            try {
                                val json = JSONObject(errorBody ?: "")
                                val errorMessage = json.getString("error")

                                if (errorMessage == "Token inválido.") {
                                    yaRedirigidoAlLogin = true
                                    val intent = Intent(this@PerfilArtista, Inicio::class.java)
                                    startActivity(intent)
                                    finish()
                                    showToast("Sesión iniciada en otro dispositivo")
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
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
                    } ?: showToast("Datos incorrectos en la respuesta")
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        val errorBody = response.errorBody()?.string()

                        try {
                            val json = JSONObject(errorBody ?: "")
                            val errorMessage = json.getString("error")

                            if (errorMessage == "Token inválido.") {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@PerfilArtista, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                showToast("Sesión iniciada en otro dispositivo")
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<HistorialEscuchasResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun showDeleteAccountDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_delete_account)

        val window: Window? = dialog.window
        window?.setLayout((Resources.getSystem().displayMetrics.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.setCancelable(true)

        val editPassword = dialog.findViewById<EditText>(R.id.editPassword)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirm)

        btnConfirm.setOnClickListener {
            val password = editPassword.text.toString().trim()
            if (password.isNotEmpty()) {
                val intent = Intent(this, DeleteAccount::class.java)
                intent.putExtra("password", password)
                startActivity(intent)
                dialog.dismiss()
            } else {
                showToast("Introduce tu contraseña")
            }
        }

        dialog.show()
    }

    private fun showChangePasswordDialog() {
        Log.d("MiAppPerfil", "change pass 1")
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_change_password)

        val window: Window? = dialog.window
        if (window != null) {
            window.setLayout(
                (Resources.getSystem().displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        dialog.setCancelable(true)
        Log.d("MiAppPerfil", "change pass 2")

        val passActual = dialog.findViewById<EditText>(R.id.passActual)
        val passNueva = dialog.findViewById<EditText>(R.id.passNueva)
        val btnChange = dialog.findViewById<Button>(R.id.btnConfirm)
        val btnToggleActual = dialog.findViewById<ImageButton>(R.id.btnToggleActual)
        val btnToggleNueva = dialog.findViewById<ImageButton>(R.id.btnToggleNueva)

        val font = ResourcesCompat.getFont(this, R.font.poppins_regular)
        val typefaceSpan = TypefaceSpan(font!!)

        var isActualVisible = false
        var isNuevaVisible = false

        btnToggleActual.setOnClickListener {
            isActualVisible = !isActualVisible
            val typeface = passActual.typeface
            if (isActualVisible) {
                passActual.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnToggleActual.setImageResource(R.drawable.ic_visibility_off)
            } else {
                passActual.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnToggleActual.setImageResource(R.drawable.ic_visibility_on)
            }
            passActual.typeface = typeface
            passActual.setSelection(passActual.text.length)
        }

        btnToggleNueva.setOnClickListener {
            isNuevaVisible = !isNuevaVisible
            val typeface = passNueva.typeface
            if (isNuevaVisible) {
                passNueva.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnToggleNueva.setImageResource(R.drawable.ic_visibility_off)
            } else {
                passNueva.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnToggleNueva.setImageResource(R.drawable.ic_visibility_on)
            }
            passNueva.typeface = typeface
            passNueva.setSelection(passNueva.text.length)
        }

        // Aplicar fuente personalizada con Spannable (opcional)
        val spannableActual = SpannableString(passActual.text)
        spannableActual.setSpan(typefaceSpan, 0, spannableActual.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        passActual.setText(spannableActual)

        val spannableNueva = SpannableString(passNueva.text)
        spannableNueva.setSpan(typefaceSpan, 0, spannableNueva.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        passNueva.setText(spannableNueva)

        Log.d("MiAppPerfil", "change pass 3")

        btnChange.setOnClickListener {
            val actual = passActual.text.toString()
            val nueva = passNueva.text.toString()

            if(actual.isEmpty()) {
                showToast("Todos los campos son obligatorios.")
                return@setOnClickListener
            }

            if (!isValidPassword(this, nueva)) {
                if (nueva.isEmpty()) {
                    showToast("Todos los campos son obligatorios.")
                }
                return@setOnClickListener
            }

            if (actual == nueva) {
                Toast.makeText(this, "La nueva contraseña debe ser distinta de la actual.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            Log.d("MiAppPerfil", "change pass 5")
            changePassword(actual, nueva, dialog)
            Log.d("MiAppPerfil", "PERFIL show edit 6")
        }

        dialog.show()
    }

    private fun changePassword(passActual: String, passNueva: String, dialog: Dialog) {
        val request = ChangePasswordRequest(passActual, passNueva)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        Log.d("updateUserProfile", "2")
        apiService.changePassword(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    dialog.dismiss()
                    showToast("Contraseña actualizada")
                } else {
                    val errorBodyString = response.errorBody()?.string()

                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        try {
                            val json = JSONObject(errorBodyString ?: "")
                            val errorMessage = json.getString("error")

                            if (errorMessage == "Token inválido.") {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@PerfilArtista, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                showToast("Sesión iniciada en otro dispositivo")
                                return
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    Log.d("updateUserProfile", "Error en la solicitud ${response.code()}")

                    try {
                        val json = JSONObject(errorBodyString ?: "")
                        val errorMessage = json.getString("error")
                        Toast.makeText(this@PerfilArtista, errorMessage, Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@PerfilArtista, "Error desconocido.", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("updateUserProfile", "Error en la solicitud2")
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    // Función para validar la contraseña (mínimo 10 caracteres, 1 letra y 1 carácter especial)
    private fun isValidPassword(context: Context, password: String): Boolean {
        return when {
            password.isEmpty() -> {
                false
            }
            password.length < 10 -> {
                Toast.makeText(context, "La contraseña debe tener al menos 10 caracteres.", Toast.LENGTH_LONG).show()
                false
            }
            !password.any { it.isLetter() } -> {
                Toast.makeText(context, "La contraseña debe contener al menos una letra.", Toast.LENGTH_LONG).show()
                false
            }
            !password.any { it.isDigit() || "!@#\$%^&*()_+-=[]{};':\"\\|,.<>/?".contains(it) } -> {
                Toast.makeText(context, "La contraseña debe contener al menos un número o carácter especial.", Toast.LENGTH_LONG).show()
                false
            }
            else -> true
        }
    }


    private fun showEditProfileDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_edit_profile_artista)

        val window: Window? = dialog.window
        window?.setLayout((Resources.getSystem().displayMetrics.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.setCancelable(true)

        val editUsername = dialog.findViewById<EditText>(R.id.editUsername)
        val editArtisticName = dialog.findViewById<EditText>(R.id.editArtisticName)
        val editBiografia = dialog.findViewById<EditText>(R.id.editBiografia)

        profileImageViewDialog = dialog.findViewById(R.id.profileImageDialog)
        val btnSelectImage = dialog.findViewById<Button>(R.id.btnSelectImage)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)

        editUsername.setText(usernameTextView.text.toString())
        editArtisticName.setText(artisticnameTextView.text.toString())
        editBiografia.setText(biografiaArtistaTextView.text.toString())
        Glide.with(this)
            .load(Preferencias.obtenerValorString("fotoPerfil", "DEFAULT"))
            .circleCrop()
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(profileImageViewDialog!!)

        btnSelectImage.setOnClickListener {
            openGalleryLauncher.launch("image/*")
        }

        btnSave.setOnClickListener {
            imageUri?.let { uri -> getSignatureCloudinary(uri, editUsername.text.toString(), editArtisticName.text.toString(), editBiografia.text.toString()) }
            updateUserProfile(editUsername.text.toString(), editArtisticName.text.toString(), editBiografia.text.toString())
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getSignatureCloudinary(imagenURI: Uri, newUsername: String, newArtisticname: String, newBiografia: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        val folder = "perfiles"

        apiService.getSignature(authHeader, folder).enqueue(object : Callback<GetSignatureResponse> {
            override fun onResponse(call: Call<GetSignatureResponse>, response: Response<GetSignatureResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        uploadImageToCloudinary(it, imagenURI, folder, newUsername, newArtisticname, newBiografia)
                    }
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        val errorBody = response.errorBody()?.string()

                        try {
                            val json = JSONObject(errorBody ?: "")
                            val errorMessage = json.getString("error")

                            if (errorMessage == "Token inválido.") {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@PerfilArtista, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                showToast("Sesión iniciada en otro dispositivo")
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<GetSignatureResponse>, t: Throwable) {
            }
        })
    }

    private fun uploadImageToCloudinary(signatureData: GetSignatureResponse, imagenURI: Uri, folder: String, newUsername: String, newArtisticname: String, newBiografia: String) {
        try {
            val inputStream = contentResolver.openInputStream(imagenURI) ?: run {
                return
            }

            val byteArray = inputStream.readBytes()
            inputStream.close()

            val requestFile = RequestBody.create(MediaType.parse("image/*"), byteArray)
            val filePart = MultipartBody.Part.createFormData("file", "image.jpg", requestFile)

            val apiKey = RequestBody.create(MediaType.parse("text/plain"), signatureData.apiKey)
            val timestamp = RequestBody.create(MediaType.parse("text/plain"), signatureData.timestamp.toString())
            val signature = RequestBody.create(MediaType.parse("text/plain"), signatureData.signature)
            val folderPart = RequestBody.create(MediaType.parse("text/plain"), folder)

            apiServiceCloud.uploadImage(
                signatureData.cloudName,
                filePart,
                apiKey,
                timestamp,
                signature,
                folderPart
            ).enqueue(object : Callback<CloudinaryResponse> {
                override fun onResponse(call: Call<CloudinaryResponse>, response: Response<CloudinaryResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            Preferencias.guardarValorString("profile_image", it.secure_url)
                            updateUserProfile(newUsername, newArtisticname, newBiografia)
                        }
                    } else {
                        if (response.code() == 401 && !yaRedirigidoAlLogin) {
                            val errorBody = response.errorBody()?.string()

                            try {
                                val json = JSONObject(errorBody ?: "")
                                val errorMessage = json.getString("error")

                                if (errorMessage == "Token inválido.") {
                                    yaRedirigidoAlLogin = true
                                    val intent = Intent(this@PerfilArtista, Inicio::class.java)
                                    startActivity(intent)
                                    finish()
                                    showToast("Sesión iniciada en otro dispositivo")
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<CloudinaryResponse>, t: Throwable) {
                    showToast("Error en la subida: ${t.message}")
                }
            })
        } catch (e: Exception) {
            showToast("Error al procesar la imagen: ${e.message}")
        }
    }

    private fun updateUserProfile(newUsername: String, newArtisticname: String, newBiografia: String) {
        val imagen = Preferencias.obtenerValorString("profile_image", "")
        val request = EditarPerfilArtistaRequest(imagen, newUsername, newArtisticname, newBiografia)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        Log.d("ActualizarPerfil", "Foto: $imagen , User: $newUsername , Artist: $newArtisticname , Bio: $newBiografia")

        apiService.updateProfileArtista(authHeader, request).enqueue(object : Callback<EditarPerfilResponse> {
            override fun onResponse(call: Call<EditarPerfilResponse>, response: Response<EditarPerfilResponse>) {
                if (response.isSuccessful) {
                    usernameTextView.text = newUsername
                    Preferencias.guardarValorString("nombreUsuario", newUsername)
                    Preferencias.guardarValorString("fotoPerfil", imagen)
                    usernameTextView.text = newUsername
                    artisticnameTextView.text = newArtisticname
                    biografiaArtistaTextView.text = newBiografia
                    Glide.with(this@PerfilArtista)
                        .load(imagen)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(profileImageButton)
                    Glide.with(this@PerfilArtista)
                        .load(imagen)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(profileImageView)

                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@PerfilArtista, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@PerfilArtista, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                    val errorBody = response.errorBody()?.string()
                    try {
                        val json = JSONObject(errorBody)
                        val errorMessage = json.getString("error")
                        Toast.makeText(this@PerfilArtista, errorMessage, Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@PerfilArtista, "Error desconocido.", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<EditarPerfilResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
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
                        ReproduccionTracker.startTracking(this@PerfilArtista, id) {
                            notificarReproduccion()
                        }

                        Preferencias.guardarValorString("audioCancionActual", audioResponse.audio)
                        guardarDatoscCancion(id)
                        actualizarIconoPlayPause()
                    }
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        val errorBody = response.errorBody()?.string()

                        try {
                            val json = JSONObject(errorBody ?: "")
                            val errorMessage = json.getString("error")

                            if (errorMessage == "Token inválido.") {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@PerfilArtista, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                showToast("Sesión iniciada en otro dispositivo")
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
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
                        ReproduccionTracker.startTracking(this@PerfilArtista, ordenColeccion[indice]) {
                            notificarReproduccion()
                        }
                        guardarDatoscCancion(ordenColeccion[indice])
                        actualizarIconoPlayPause()
                    }
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        val errorBody = response.errorBody()?.string()

                        try {
                            val json = JSONObject(errorBody ?: "")
                            val errorMessage = json.getString("error")

                            if (errorMessage == "Token inválido.") {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@PerfilArtista, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                showToast("Sesión iniciada en otro dispositivo")
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
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

        apiService.addReproduccion(authHeader).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("MiApp", "Reproducción registrada exitosamente")
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        val errorBody = response.errorBody()?.string()

                        try {
                            val json = JSONObject(errorBody ?: "")
                            val errorMessage = json.getString("error")

                            if (errorMessage == "Token inválido.") {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@PerfilArtista, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                showToast("Sesión iniciada en otro dispositivo")
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
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
                        val errorBody = response.errorBody()?.string()

                        try {
                            val json = JSONObject(errorBody ?: "")
                            val errorMessage = json.getString("error")

                            if (errorMessage == "Token inválido.") {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@PerfilArtista, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                showToast("Sesión iniciada en otro dispositivo")
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<CancionInfoResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)

                // Mostrar en Toast
                Toast.makeText(this@PerfilArtista, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun cambiarModoOsucro() {
        var claro = true
        val modoOscuro = Preferencias.obtenerValorEntero("modoOscuro", 1)

        if(modoOscuro == 0){
            claro = false
        }

        val request = ClaroRequest(claro)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.change_claro(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("ModoOscuro", "Cambiado el modo oscuro")

                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        val errorBody = response.errorBody()?.string()

                        try {
                            val json = JSONObject(errorBody ?: "")
                            val errorMessage = json.getString("error")

                            if (errorMessage == "Token inválido.") {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@PerfilArtista, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                showToast("Sesión iniciada en otro dispositivo")
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("Modo", "Fallo: ${t.message}")
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

    private fun setDayNight(mode: Int){
        if(mode == 0){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
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
        loadArtistAlbums()
        updateMiniReproductor()
    }
}