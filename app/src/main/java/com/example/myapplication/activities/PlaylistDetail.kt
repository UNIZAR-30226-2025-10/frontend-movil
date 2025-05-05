package com.example.myapplication.activities

import ParticipantesAdapter
import android.annotation.SuppressLint
import android.app.AlertDialog
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
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.TypefaceSpan
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
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
import com.example.myapplication.R
import com.example.myapplication.Adapters.Playlist.CancionPAdapter
import com.example.myapplication.Adapters.Playlist.PlaylistSelectionAdapter
import com.example.myapplication.Adapters.Playlist.SongPlaylistSearchAdapter
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.CloudinaryApiService
import com.example.myapplication.io.request.ActualizarFavoritoRequest
import com.example.myapplication.io.request.AddToPlaylistRequest
import com.example.myapplication.io.request.AudioColeccionRequest
import com.example.myapplication.io.request.AudioRequest
import com.example.myapplication.io.request.CambiarPrivacidadPlaylistRequest
import com.example.myapplication.io.request.DeleteFromPlaylistRequest
import com.example.myapplication.io.request.DeletePlaylistRequest
import com.example.myapplication.io.request.ExpelUserRequest
import com.example.myapplication.io.request.InvitarPlaylistRequest
import com.example.myapplication.io.request.LeavePlaylistRequest
import com.example.myapplication.io.request.ModoRequest
import com.example.myapplication.io.request.UpdatePlaylistRequest
import com.example.myapplication.io.response.ActualizarFavoritoResponse
import com.example.myapplication.io.response.AddReproduccionResponse
import com.example.myapplication.io.response.AudioResponse
import com.example.myapplication.io.response.Cancion
import com.example.myapplication.io.response.CancionInfoResponse
import com.example.myapplication.io.response.CancionP
import com.example.myapplication.io.response.CloudinaryResponse
import com.example.myapplication.io.response.GetSignatureResponse
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.InvitacionPlaylist
import com.example.myapplication.io.response.MisPlaylist
import com.example.myapplication.io.response.Novedad
import com.example.myapplication.io.response.PlaylistP
import com.example.myapplication.io.response.PlaylistResponse
import com.example.myapplication.io.response.PlaylistsResponse
import com.example.myapplication.io.response.SearchPlaylistResponse
import com.example.myapplication.io.response.Seguidor
import com.example.myapplication.io.response.SeguidoresResponse
import com.example.myapplication.io.response.Usuario
import com.example.myapplication.services.MusicPlayerService
import com.example.myapplication.services.WebSocketEventHandler
import com.example.myapplication.utils.Preferencias
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Collections.addAll

class PlaylistDetail : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var apiServiceCloud: CloudinaryApiService
    private lateinit var recyclerViewCanciones: RecyclerView
    private lateinit var cancionPAdapter: CancionPAdapter
    private lateinit var playlistTextView: TextView
    private lateinit var personas: TextView
    private lateinit var playlistImageView: ImageView
    private lateinit var playlistImageButton: ImageView
    private lateinit var duracion: TextView
    private lateinit var dot: View
    private var currentPlaylist: PlaylistP? = null
    private var imageUri: Uri? = null
    private var playlistImageViewDialog: ImageView? = null
    private val openGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            playlistImageViewDialog?.setImageURI(imageUri)  // Set the selected image in the dialog ImageView
        }
    }
    var playlistId: String? = null
    private var isFavorito = false
    var alguienExpulsado = false

    private var rol: String? = null

    private var aleatorio = false
    private var modo = "enOrden"
    private var modoCambiado = false

    private var orden: List<String> = listOf()
    private var indexActual: Int = 0
    private var playlistIdActual: String? = null

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

    @SuppressLint("MissingInflatedId", "WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_playlist)

        Log.d("Playlist", "Entra en la playlist")

        apiService = ApiService.create()
        apiServiceCloud = CloudinaryApiService.create()

        indexActual = Preferencias.obtenerValorEntero("indexColeccionActual", 0)

        playlistId = intent.getStringExtra("id")
        Log.d("MiAppPlaylist", "id ${playlistId}")
        val nombrePlaylist = intent.getStringExtra("nombre")
        Log.d("MiAppPlaylist", "nombre ${nombrePlaylist}")
        val imagenUrl = intent.getStringExtra("imagen")

        val textViewNombre = findViewById<TextView>(R.id.textViewNombrePlaylist)
        val textViewNumCanciones = findViewById<TextView>(R.id.textViewNumCanciones)
        val imageViewPlaylist = findViewById<ImageView>(R.id.imageViewPlaylist)
        personas = findViewById<TextView>(R.id.participantes)
        dot = findViewById<View>(R.id.notificationDot)
        duracion = findViewById<TextView>(R.id.duracion)

        playlistTextView = findViewById(R.id.textViewNombrePlaylist)

        playlistImageButton = findViewById(R.id.imageViewPlaylist)

        // Configuración del RecyclerView
        recyclerViewCanciones = findViewById(R.id.recyclerViewCanciones)
        recyclerViewCanciones.layoutManager = LinearLayoutManager(this)
        cancionPAdapter = CancionPAdapter(listOf(),
            { cancion ->
                val cancionId = Preferencias.obtenerValorString("cancionActualId", "")
                if(cancionId == cancion.id){
                    startActivity(Intent(this, CancionReproductorDetail::class.java))
                }
                else {
                    playlistId?.let { Preferencias.guardarValorString("coleccionActualId", it) }
                    orden = Preferencias.obtenerValorString("ordenColeccionMirada", "")
                        .split(",")
                        .filter { id -> id.isNotEmpty() }
                    val ordenNatural = Preferencias.obtenerValorString("ordenNaturalColeccionMirada", "")
                        .split(",")
                        .filter { id -> id.isNotEmpty() }
                    modo = "enOrden"
                    indexActual = ordenNatural.indexOf(cancion.id)

                    Preferencias.guardarValorString("ordenNaturalColeccionActual", ordenNatural.joinToString(","))
                    Preferencias.guardarValorEntero("indexColeccionActual", indexActual)
                    Preferencias.guardarValorString("modoColeccionActual", modo)

                    Preferencias.guardarValorString("ordenColeccionActual", ordenNatural.joinToString(","))

                    reproducirColeccion()
                }
            },
            { anchorView, cancion ->
                showSongOptionsPopupMenu(anchorView, cancion)
            }
        )
        recyclerViewCanciones.adapter = cancionPAdapter

        textViewNombre.text = nombrePlaylist
        val foto: Any = when {
            imagenUrl == "DEFAULT" || imagenUrl.isNullOrBlank() -> R.drawable.no_cancion
            else -> imagenUrl
        }
        Glide.with(this)
            .load(foto)
            .transform(MultiTransformation(
                    CenterCrop(),
                    RoundedCorners(
                        TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            12f,
                            this.resources.displayMetrics
                        ).toInt()
                    )
                )
            )
            .placeholder(R.drawable.no_cancion)
            .error(R.drawable.no_cancion)
            .into(imageViewPlaylist)


        val idActual = Preferencias.obtenerValorString("cancionActualId", "")
        val idColeccionActual = Preferencias.obtenerValorString("coleccionActualId", "")
        orden = Preferencias.obtenerValorString("ordenNaturalColeccionMirada", "")
            .split(",")
            .filter { id -> id.isNotEmpty() }
        if(idColeccionActual == playlistId){
            indexActual = orden.indexOf(idActual)
        }
        else{
            indexActual = 0
        }

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

        val btnPlayPausePlaylist: ImageButton = findViewById(R.id.btnPlay)

        // Llamada a la API para obtener los datos de la playlist
        playlistId?.let {
            loadPlaylistData(it, textViewNombre, textViewNumCanciones, imageViewPlaylist)
        }


        Preferencias.guardarValorString("modoColeccionMirada", "enOrden")
        Preferencias.guardarValorEntero("indexColeccionMirada", indexActual)
        progressBar = findViewById(R.id.progressBar)
        updateMiniReproductor()


        // Agregar funcionalidad al botón de añadir canción
        btnPlayPausePlaylist.setOnClickListener {
            playlistId?.let {
                val estaReproduciendo = musicService!!.isPlaying()
                val coleccionactual = Preferencias.obtenerValorString("coleccionActualId", "")
                if(coleccionactual==playlistId){
                    if(estaReproduciendo) {
                        musicService!!.pause()
                        actualizarIconoPlayPause()
                    }
                    else{
                        musicService!!.resume()
                        actualizarIconoPlayPause()
                    }
                }
                else {
                    playlistIdActual = it
                    Preferencias.guardarValorString("coleccionActualId", it)


                    orden = Preferencias.obtenerValorString("ordenColeccionMirada", "")
                        .split(",")
                        .filter { id -> id.isNotEmpty() }
                    val ordenNatural =
                        Preferencias.obtenerValorString("ordenNaturalColeccionMirada", "")
                            .split(",")
                            .filter { id -> id.isNotEmpty() }
                    modo = Preferencias.obtenerValorString("modoColeccionMirada", "enOrden")

                    if (indexActual <= -1 || indexActual >= orden.size) {
                        indexActual = 0
                    }

                    Preferencias.guardarValorString(
                        "ordenNaturalColeccionActual",
                        ordenNatural.joinToString(",")
                    )
                    Preferencias.guardarValorEntero("indexColeccionActual", indexActual)
                    Preferencias.guardarValorString("modoColeccionActual", modo)

                    if (modo == "enOrden") {
                        Preferencias.guardarValorString(
                            "ordenColeccionActual",
                            ordenNatural.joinToString(",")
                        )
                    } else {
                        Preferencias.guardarValorString(
                            "ordenColeccionActual",
                            orden.joinToString(",")
                        )
                    }


                    reproducirColeccion()
                }
            }
        }

        val btnMoreOptions: ImageButton = findViewById(R.id.btnMoreOptions)
        btnMoreOptions.setOnClickListener {
                showMoreOptionsPopupMenu(it)
        }
        btnMoreOptions.visibility = View.VISIBLE
        val btnAnadirCancion: ImageButton = findViewById(R.id.btnAnadirCancion)
        btnAnadirCancion.setOnClickListener {
            showSearchSongDialog()
        }
        val btnAddUser: ImageButton = findViewById(R.id.btnAddUser)
        btnAddUser.setOnClickListener {
            showInvitarUsuarioDialog()
        }
        btnAddUser.visibility = View.VISIBLE

        val btnAleatorio: ImageButton = findViewById(R.id.aleatorio)
        btnAleatorio.setOnClickListener {
            if (aleatorio == true){
                val idColeccionActual = Preferencias.obtenerValorString("coleccionActualId", "")

                btnAleatorio.setImageResource(R.drawable.shuffle_24px)
                aleatorio = false
                modo = "enOrden"

                val idActual = Preferencias.obtenerValorString("cancionActualId", "")

                orden = Preferencias.obtenerValorString("ordenNaturalColeccionMirada", "")
                    .split(",")
                    .filter { id -> id.isNotEmpty() }
                if(idColeccionActual == playlistId){
                    indexActual = orden.indexOf(idActual)
                }
                else{
                    indexActual = 0
                }

                if(idColeccionActual == playlistId){
                    Preferencias.guardarValorString("modoColeccionActual", "enOrden")
                    Preferencias.guardarValorString("ordenColeccionActual", orden.joinToString(","))
                    Preferencias.guardarValorEntero("indexColeccionActual", 0)
                    cambiarModo()
                }
                else {
                    Preferencias.guardarValorString("modoColeccionMirada", "enOrden")
                    Preferencias.guardarValorString("ordenColeccionMirada", orden.joinToString(","))
                }
                Log.d("ReproducirPlaylist", "IDs en orden normal: ${orden.joinToString(",")}")
                Log.d("ReproducirPlaylist", "Indice del id: $indexActual")
            }
            else{
                modo = "aleatorio"
                orden = Preferencias.obtenerValorString("ordenNaturalColeccionMirada", "")
                    .split(",")
                    .filter { id -> id.isNotEmpty() }
                val primerId = orden[indexActual]
                // ordenar aleatoriamente la lista manteniendo primerId al inicio
                orden = orden.shuffled().toMutableList().apply {
                    remove(primerId)
                    add(0, primerId)
                }
                Log.d("ReproducirPlaylist", "IDs aleatorios: ${orden.joinToString(",")}")
                indexActual = 0
                if(idColeccionActual == playlistId){
                    indexActual = orden.indexOf(idActual)
                    Preferencias.guardarValorEntero("indexColeccionActual", indexActual)
                    Preferencias.guardarValorString("ordenColeccionActual", orden.joinToString(","))
                    Preferencias.guardarValorString("modoColeccionActual", "aleatorio")
                    cambiarModo()
                }
                else {
                    Preferencias.guardarValorEntero("indexColeccionMirada", indexActual)
                    Preferencias.guardarValorString("ordenColeccionMirada", orden.joinToString(","))
                    Preferencias.guardarValorString("modoColeccionMirada", "aleatorio")
                }
                btnAleatorio.setImageResource(R.drawable.shuffle_24px_act)
                aleatorio = true
            }
        }

        setupNavigation()

    }

    // Función para realizar la llamada a la API y obtener los datos
    private fun loadPlaylistData(
        playlistId: String,
        textViewNombre: TextView,
        textViewNumCanciones: TextView,
        imageViewPlaylist: ImageView
    ) {
        val token = Preferencias.obtenerValorString("token", "")
        Log.d("MiAppPlaylist", "entra load datos")
        apiService.getDatosPlaylist("Bearer $token", playlistId).enqueue(object : Callback<PlaylistResponse> {
            override fun onResponse(call: Call<PlaylistResponse>, response: Response<PlaylistResponse>) {
                if (response.isSuccessful) {
                    val playlist = response.body()?.playlist
                    val canciones = response.body()?.canciones
                    currentPlaylist = playlist

                    // Actualizar la UI con los datos de la playlist
                    playlist?.let {
                        textViewNombre.text = it.nombrePlaylist
                        rol = response.body()?.rol
                        Log.d("MiAppPlaylist", "Nombre${textViewNombre.text}")
                        Log.d("MiAppPlaylist", "Creador${playlist.creador}")
                        val numCanciones = canciones?.size ?: 0
                        textViewNumCanciones.text = "$numCanciones ${if (numCanciones == 1) "Canción" else "Canciones"}"
                        val segundos = playlist.duracion
                        val minutos = segundos / 60
                        val restoSegundos = segundos % 60
                        //duracion.text = String.format("%d:%02d", minutos, restoSegundos)
                        duracion.text = "$minutos minutos $restoSegundos segundos"

                        val textoParticipantes = if (playlist.colaboradores.isNotEmpty()) {
                            "${playlist.creador}, +${playlist.colaboradores.size} más"
                        } else {
                            "${playlist.creador}"
                        }
                        personas.text = textoParticipantes

                        val participantes = mutableListOf(it.creador).apply {
                            addAll(it.colaboradores)
                        }

                        val creador = it.creador
                        val colaboradores = it.colaboradores


                        personas.setOnClickListener {
                            val participantes = mutableListOf(creador).apply {
                                addAll(colaboradores)
                            }

                            val dialogView = layoutInflater.inflate(R.layout.dialog_participantes, null)

                            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewParticipantes)
                            recyclerView.layoutManager = LinearLayoutManager(this@PlaylistDetail)
                            recyclerView.adapter = ParticipantesAdapter(participantes, creador) { expulsado ->
                                alguienExpulsado = true
                                Toast.makeText(this@PlaylistDetail, "Expulsaste a $expulsado", Toast.LENGTH_SHORT).show()
                                expelUser(playlistId, expulsado)
                            }

                            val titulo = dialogView.findViewById<TextView>(R.id.textViewName)
                            titulo.text = "Participantes"

                            val alertDialog = AlertDialog.Builder(this@PlaylistDetail)
                                .setView(dialogView)
                                .create()

                            alertDialog.setOnDismissListener {
                                if (alguienExpulsado) {
                                    recreate()
                                }
                            }

                            alertDialog.window?.setLayout((Resources.getSystem().displayMetrics.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                            alertDialog.show()
                        }
                        var foto: Any
                        if (it.fotoPortada == "DEFAULT") {
                            foto = R.drawable.no_cancion
                        } else {
                            foto = it.fotoPortada
                        }
                        Glide.with(this@PlaylistDetail)
                            .load(foto)
                            .transform(MultiTransformation(
                                    CenterCrop(),
                                    RoundedCorners(
                                        TypedValue.applyDimension(
                                            TypedValue.COMPLEX_UNIT_DIP,
                                            12f,
                                            this@PlaylistDetail.resources.displayMetrics
                                        ).toInt()
                                    )
                                )
                            )
                            .placeholder(R.drawable.no_cancion)
                            .error(R.drawable.no_cancion)
                            .into(imageViewPlaylist)

                        updateUIForPlaylistOwnership()

                        if (playlist.nombrePlaylist == "Favoritos") {
                            val btnMoreOptions: ImageButton = findViewById(R.id.btnMoreOptions)
                            val btnAddUser: ImageButton = findViewById(R.id.btnAddUser)
                            btnMoreOptions.visibility = View.GONE
                            btnAddUser.visibility = View.GONE
                        }
                    }

                    // Actualizar RecyclerView con la lista de canciones
                    canciones?.let {
                        cancionPAdapter.updateData(it)
                    }

                    val ids: List<String>? = canciones?.map { it.id }
                    ids?.let {
                        Log.d("ReproducirPlaylist", "IDs extraídos: ${it.joinToString(",")}")
                        Preferencias.guardarValorString("ordenNaturalColeccionMirada", ids.joinToString(","))
                    }

                } else {
                    // Manejo de error en caso de que la respuesta no sea exitosa
                    Log.d("MiAppPlaylist", "NO EXITO")
                    Toast.makeText(this@PlaylistDetail, "Error al obtener los datos de la playlist", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PlaylistResponse>, t: Throwable) {
                // Manejo de error si ocurre un fallo en la conexión
                Log.e("MiAppPlaylist", "Error de conexión al obtener la playlist", t)
                Toast.makeText(this@PlaylistDetail, "Error de conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUIForPlaylistOwnership() {
        val btnAnadirCancion: ImageButton = findViewById(R.id.btnAnadirCancion)
        val btnAddUser: ImageButton = findViewById(R.id.btnAddUser)
        val btnMoreOptions: ImageButton = findViewById(R.id.btnMoreOptions)

        when (rol?.lowercase()) {
            "creador" -> {
                // Mostrar todos los botones para el creador
                btnAnadirCancion.visibility = View.VISIBLE
                btnAddUser.visibility = View.VISIBLE
                btnMoreOptions.visibility = View.VISIBLE
            }
            "participante" -> {
                // Ocultar botones de administración para participantes
                btnAnadirCancion.visibility = View.VISIBLE
                btnAddUser.visibility = View.GONE
                btnMoreOptions.visibility = View.VISIBLE
            }
            else -> {
                // Para usuarios que solo están viendo la playlist
                btnAnadirCancion.visibility = View.GONE
                btnAddUser.visibility = View.GONE
                btnMoreOptions.visibility = View.GONE
            }
        }
    }

    private fun showSearchSongDialog() {
        Log.d("MiAppPlaylist", "Abrir diálogo de búsqueda de canciones")

        // Crear el diálogo
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_cancion)

        // Configurar la ventana del diálogo
        val window: Window? = dialog.window
        window?.setLayout((Resources.getSystem().displayMetrics.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Obtener los elementos del diálogo
        val etSearchSong: EditText = dialog.findViewById(R.id.etSearchSong)
        val recyclerViewSongs: RecyclerView = dialog.findViewById(R.id.recyclerViewSongs)

        // Configurar el RecyclerView
        val adapter = SongPlaylistSearchAdapter(emptyList()) { song ->
            // This is called when a song is clicked or the add button is pressed
            addSongToPlaylist(song)
            dialog.dismiss() // Close the dialog after adding
        }
        recyclerViewSongs.layoutManager = LinearLayoutManager(this)
        recyclerViewSongs.adapter = adapter

        // Configurar el TextWatcher para la búsqueda
        etSearchSong.addTextChangedListener(object : TextWatcher {
            private val handler = Handler()
            private var runnable: Runnable? = null

            override fun afterTextChanged(s: Editable?) {
                // Cancelar cualquier tarea previa
                runnable?.let { handler.removeCallbacks(it) }

                // Ejecutar la búsqueda después de 500ms de inactividad
                runnable = Runnable {
                    val searchTerm = s.toString()
                    if (searchTerm.isNotBlank()) {
                        searchSongs(searchTerm, adapter)
                    }
                }
                handler.postDelayed(runnable!!, 500)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Mostrar el diálogo
        dialog.show()
    }

    private fun searchSongs(searchTerm: String, adapter: SongPlaylistSearchAdapter) {
        // Llamada a tu API para buscar canciones con el término de búsqueda
        val token = Preferencias.obtenerValorString("token", "")
        val playlistId = intent.getStringExtra("id") ?: ""

        Log.d("MiAppPlaylist", "searcj")

        // Realizar la llamada a la API
        apiService.searchForSongs("Bearer $token", searchTerm, playlistId).enqueue(object : Callback<SearchPlaylistResponse> {
            override fun onResponse(call: Call<SearchPlaylistResponse>, response: Response<SearchPlaylistResponse>) {
                if (response.isSuccessful) {
                    Log.d("MiAppPlaylist", "2")
                    val songs = response.body()?.canciones ?: listOf()
                    // Actualizar el RecyclerView con las canciones
                    adapter.updateData(songs)
                } else {
                    Toast.makeText(this@PlaylistDetail, "Error al obtener canciones", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SearchPlaylistResponse>, t: Throwable) {
                Toast.makeText(this@PlaylistDetail, "Error de conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addSongToPlaylist(song: Cancion) {
        val token = Preferencias.obtenerValorString("token", "")
        val playlistId = intent.getStringExtra("id") ?: ""

        val request = AddToPlaylistRequest(song.id, playlistId)


        apiService.addSongToPlaylist("Bearer $token", request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                when {
                    response.isSuccessful -> {
                        Toast.makeText(
                            this@PlaylistDetail,
                            "Canción añadida a la playlist",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Refrescar los datos de la playlist
                        loadPlaylistData(
                            playlistId,
                            findViewById(R.id.textViewNombrePlaylist),
                            findViewById(R.id.textViewNumCanciones),
                            findViewById(R.id.imageViewPlaylist)
                        )
                    }
                    response.code() == 403 -> {
                        Toast.makeText(
                            this@PlaylistDetail,
                            "No tienes permiso para modificar esta playlist",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    response.code() == 404 -> {
                        val error = when {
                            response.errorBody()?.string()?.contains("playlist") == true ->
                                "La playlist no existe"
                            else -> "La canción no existe"
                        }
                        Toast.makeText(this@PlaylistDetail, error, Toast.LENGTH_SHORT).show()
                    }
                    response.code() == 409 -> {
                        Toast.makeText(
                            this@PlaylistDetail,
                            "Esta canción ya está en la playlist",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        Toast.makeText(
                            this@PlaylistDetail,
                            "Error al añadir canción: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(
                    this@PlaylistDetail,
                    "Error de conexión: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }


    private fun showMoreOptionsPopupMenu(anchorView: View) {
        Log.d("MiAppPlaylist", "Abrir PopupMenu de Más opciones playlist")

        val privacyOption = if (currentPlaylist?.privacidad == true) "Hacer pública" else "Hacer privada"

        val popup = PopupMenu(this, anchorView, Gravity.START, 0, R.style.PopupMenuStyle)
        val menu = popup.menu

        when (rol?.lowercase()) {
            "creador" -> {
                menu.add(0, 1, 0, "Editar lista")
                menu.add(0, 2, 1, "Eliminar lista")
                menu.add(0, 4, 2, if (currentPlaylist?.privacidad == true) "Hacer pública" else "Hacer privada")
            }
            "participante" -> {
                menu.add(0, 1, 0, "Editar lista")
                menu.add(0, 3, 1, "Abandonar playlist")
                menu.add(0, 4, 2, if (currentPlaylist?.privacidad == true) "Hacer pública" else "Hacer privada")
            }
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> showEditPlaylistDialog()
                2 -> showConfirmDeleteDialog()
                3 -> showConfirmLeaveDialog()
                4 -> changePrivacyPlaylist()
            }
            true
        }

        for (i in 0 until popup.menu.size()) {
            val item = popup.menu.getItem(i)
            val spanString = SpannableString(item.title)
            spanString.setSpan(
                TypefaceSpan(ResourcesCompat.getFont(anchorView.context, R.font.poppins_regular)!!),
                0, spanString.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            item.title = spanString
        }

        popup.show()
    }

    private fun showEditPlaylistDialog() {
        // Log para depuración
        Log.d("MiAppPlaylist", "Mostrar diálogo de edición de lista")

        // Crear el diálogo
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_edit_playlist)

        // Configuración de la ventana del diálogo
        val window: Window? = dialog.window
        window?.setLayout((Resources.getSystem().displayMetrics.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Configurar el comportamiento del diálogo
        dialog.setCancelable(true)

        // Buscar los elementos del diálogo
        val editUsername = dialog.findViewById<EditText>(R.id.editPlaylistName)
        playlistImageViewDialog = dialog.findViewById(R.id.profileImageDialog) // Usar la referencia de ImageView
        val btnSelectImage = dialog.findViewById<Button>(R.id.btnSelectImage)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)

        // Rellenar el EditText con el nombre actual
        editUsername.setText(playlistTextView.text.toString())

        val foto: Any = if (currentPlaylist?.fotoPortada.isNullOrBlank() || currentPlaylist?.fotoPortada == "DEFAULT") {
            R.drawable.no_cancion
        } else {
            currentPlaylist!!.fotoPortada
        }
        // Cargar la imagen de portada de la playlist al ImageView dentro del diálogo
        Glide.with(this)
            .load(foto) // Usar la misma fuente de la foto
            .transform(MultiTransformation(
                    CenterCrop(),
                    RoundedCorners(
                        TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            12f,
                            this@PlaylistDetail.resources.displayMetrics
                        ).toInt()
                    )
                )
            )
            .placeholder(R.drawable.no_cancion) // Imágen por defecto si no hay imagen
            .error(R.drawable.no_cancion) // Imágen de error si no se puede cargar
            .into(playlistImageViewDialog!!) // Colocar la imagen en el ImageView del diálogo

        // Seleccionar una nueva imagen desde la galería
        btnSelectImage.setOnClickListener {
            openGalleryLauncher.launch("image/*")
        }

        // Guardar los cambios
        btnSave.setOnClickListener {
            val newUsername = editUsername.text.toString()
            Log.d("updatePlaylist", "name ${newUsername} 1")
            if (imageUri != null) {
                // Si hay una nueva imagen, se sube a Cloudinary
                imageUri?.let { uri -> getSignatureCloudinary(uri, newUsername) }
            } else {
                // Si no hay imagen nueva, simplemente actualizar el nombre
                currentPlaylist?.let { it1 -> updatePlaylist(newUsername, it1.fotoPortada) }
            }


            dialog.dismiss() // Cerrar el diálogo después de guardar
        }

        dialog.show()
    }

    private fun getSignatureCloudinary(imagenURI: Uri, newUsername: String){
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        val folder = "playlist"

        Log.d("Signature", "Signature 1")
        Log.d("Signature", "Signature 1 token: {$authHeader}")
        Log.d("Signature", "Signature 1 folder {$folder}")
        apiService.getSignature(authHeader, folder).enqueue(object : Callback<GetSignatureResponse> {
            override fun onResponse(call: Call<GetSignatureResponse>, response: Response<GetSignatureResponse>) {
                Log.d("Signature", "Signature 2")
                if (response.isSuccessful) {
                    val signatureResponse = response.body()
                    signatureResponse?.let {
                        // Acceder a los datos de la respuesta
                        val signature = it.signature
                        val apiKey = it.apiKey
                        val timestamp = it.timestamp
                        val cloudName = it.cloudName


                        Log.d("Signature", "Signature: $signature")
                        Log.d("Signature", "API Key: $apiKey")
                        Log.d("Signature", "Timestamp: $timestamp")
                        Log.d("Signature", "Cloud Name: $cloudName")



                        uploadImageToCloudinary(it, imagenURI, folder, newUsername)
                    }
                    showToast("Get signature correcto")
                } else {
                    Log.d("Signature", "Signature 2")
                    showToast("Error al Get signature")
                }
            }

            override fun onFailure(call: Call<GetSignatureResponse>, t: Throwable) {
                Log.d("Signature", "Error en la solicitud: ${t.message}")
                showToast("Error en la solicitud: ${t.message}")
            }
        })
        Log.d("Signature", "Signature FUERA")
    }

    private fun uploadImageToCloudinary(
        signatureData: GetSignatureResponse,
        imagenURI: Uri,
        folder: String,
        newUsername: String
    ) {
        try {

            Log.d("uploadImageToCloudinary", "uploadImageToCloudinary 1")
            // Obtener el stream del archivo a partir del URI
            val inputStream = contentResolver.openInputStream(imagenURI) ?: run {
                showToast("Error al abrir la imagen")
                return
            }

            Log.d("uploadImageToCloudinary", "uploadImageToCloudinary 2")

            val byteArray = inputStream.readBytes()
            inputStream.close()

            Log.d("uploadImageToCloudinary", "uploadImageToCloudinary 3")
            val requestFile = RequestBody.create(MediaType.parse("image/*"), byteArray)
            val filePart = MultipartBody.Part.createFormData("file", "image.jpg", requestFile)

            // Crear request bodies para los parámetros
            val apiKey = RequestBody.create(MediaType.parse("text/plain"), signatureData.apiKey)
            val timestamp = RequestBody.create(MediaType.parse("text/plain"), signatureData.timestamp.toString())
            val signature = RequestBody.create(MediaType.parse("text/plain"), signatureData.signature)
            val folderPart = RequestBody.create(MediaType.parse("text/plain"), folder)

            // Llamada a la API de Cloudinary
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
                            val imageUrl = it.secure_url
                            Log.d("Cloudinary Upload", "Imagen subida correctamente: $imageUrl")

                            // Cargar la imagen desde la URL con Glide
                            Glide.with(applicationContext)
                                .load(imageUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_profile) // Imagen por defecto mientras carga
                                .error(R.drawable.ic_profile) // Imagen si hay error
                                .into(playlistImageButton!!)

                            Log.d("updatePlaylist", "name ${newUsername} 2")
                            updatePlaylist(newUsername, imageUrl)

                            showToast("Imagen subida con éxito")
                        } ?: showToast("Error: Respuesta vacía de Cloudinary")
                    } else {
                        Log.d("uploadImageToCloudinary", "ERROR 3 ${response.errorBody()?.string()}")
                        showToast("Error al subir la imagen: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<CloudinaryResponse>, t: Throwable) {
                    Log.d("uploadImageToCloudinary", "ERROR 3 ${t.message}")
                    showToast("Error en la subida: ${t.message}")
                }
            })
        } catch (e: Exception) {
            Log.d("uploadImageToCloudinary", "ERROR 4 ${e.message}")
            showToast("Error al procesar la imagen: ${e.message}")
        }
    }

    private fun updatePlaylist(newPlaylistName: String, imageUrl: String, ) {
        Log.d("updatePlaylist", "1")
        val playlistId = intent.getStringExtra("id") ?: ""
        val request = UpdatePlaylistRequest(playlistId, imageUrl,newPlaylistName)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        Log.d("updatePlaylist", "name ${newPlaylistName} 3")

        apiService.updatePlaylist(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    playlistTextView.text = newPlaylistName
                    showToast("playlist actualizado")
                } else {
                    Log.d("updatePlaylist", "Error en la solicitud ${response.code()}")
                    showToast("Error al actualizar playlist")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("updateUserProfile", "Error en la solicitud2")
                showToast("Error en la solicitud: ${t.message}")
            }
        })

    }

    private fun showConfirmDeleteDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete, null)

        val titulo = dialogView.findViewById<TextView>(R.id.textViewHeadersEscuchas)
        val mensaje = dialogView.findViewById<TextView>(R.id.txtMensaje)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnRechazar)
        val btnAceptar = dialogView.findViewById<Button>(R.id.btnAceptar)

        titulo.text = "Eliminar Playlist"
        mensaje.text = "¿Estás seguro de que quieres eliminar esta playlist? Esta acción no se puede deshacer."

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        alertDialog.window?.setLayout((Resources.getSystem().displayMetrics.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCancelar.setOnClickListener {
            alertDialog.dismiss()
        }

        btnAceptar.setOnClickListener {
            deletePlaylist()
            showToast("Playlist eliminada con éxito")
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun deletePlaylist() {
        Log.d("deletePlaylist", "1")
        val playlistId = intent.getStringExtra("id") ?: ""
        val request = DeletePlaylistRequest(playlistId)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.deletePlaylist(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("deletePlaylist", "1")
                    navigateInicio()
                    showToast("playlist delete")
                } else {
                    Log.d("deletePlaylist", "Error en la solicitud ${response.code()}")
                    showToast("Error al delete playlist")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("updateUserProfile", "Error en la solicitud2")
                showToast("Error en la solicitud: ${t.message}")
            }
        })

    }

    private fun showConfirmLeaveDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete, null)

        val titulo = dialogView.findViewById<TextView>(R.id.textViewHeadersEscuchas)
        val mensaje = dialogView.findViewById<TextView>(R.id.txtMensaje)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnRechazar)
        val btnAceptar = dialogView.findViewById<Button>(R.id.btnAceptar)

        titulo.text = "Abandonar Playlist"
        mensaje.text = "¿Estás seguro de que quieres abandonar esta playlist?"

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        alertDialog.window?.setLayout((Resources.getSystem().displayMetrics.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCancelar.setOnClickListener {
            alertDialog.dismiss()
        }

        btnAceptar.setOnClickListener {
            abandonarPlaylist()
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun abandonarPlaylist() {
        Log.d("abandonarPlaylist", "1")
        val playlistId = intent.getStringExtra("id") ?: ""
        val request = LeavePlaylistRequest(playlistId)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.leavePlaylist(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("abandonarPlaylist", "1")
                    navigateInicio()
                    showToast("Playlist abandonada")
                } else {
                    Log.d("abandonarPlaylist", "Error en la solicitud ${response.code()}")
                    showToast("Error al abandonar playlist")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("updateUserProfile", "Error en la solicitud2")
                showToast("Error en la solicitud: ${t.message}")
            }
        })

    }

    private fun changePrivacyPlaylist() {
        Log.d("changePrivacyPlaylist", "1")
        val playlistId = intent.getStringExtra("id") ?: ""

        val privacidadNueva = !(currentPlaylist?.privacidad ?: false)
        val request = CambiarPrivacidadPlaylistRequest(playlistId,privacidadNueva)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.changePlaylistPrivacy(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("changePrivacyPlaylist", "1")
                    navigateInicio()
                    showToast("playlist privacy cambiada")
                } else {
                    Log.d("changePrivacyPlaylist", "Error en la solicitud ${response.code()}")
                    showToast("Error al cambiar privacy playlist")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("changePrivacyPlaylist", "Error en la solicitud2")
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun showInvitarUsuarioDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_invitacion)

        val window: Window? = dialog.window
        window?.setLayout((Resources.getSystem().displayMetrics.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.setCancelable(true)

        val searchView = dialog.findViewById<SearchView>(R.id.searchViewSeguidores)
        val listView = dialog.findViewById<ListView>(R.id.listViewSeguidores)

        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter

        val token = Preferencias.obtenerValorString("token", "") ?: ""
        val authHeader = "Bearer $token"

        // Llamar a la API para obtener seguidores
        apiService.getSeguidores(authHeader).enqueue(object : Callback<SeguidoresResponse> {
            override fun onResponse(call: Call<SeguidoresResponse>, response: Response<SeguidoresResponse>) {
                if (response.isSuccessful) {
                    val seguidoresResponse = response.body()
                    val seguidores = seguidoresResponse?.seguidores ?: emptyList()
                    val nombres = seguidores.map { it.nombreUsuario}

                    adapter.clear()
                    adapter.addAll(nombres)

                    // Filtro con SearchView
                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?) = false
                        override fun onQueryTextChange(newText: String?): Boolean {
                            adapter.filter.filter(newText)
                            return true
                        }
                    })

                    listView.setOnItemClickListener { _, _, position, _ ->
                        val seleccionado = adapter.getItem(position)
                        //showToast("Invitado: $seleccionado")
                        // Aquí puedes hacer la petición para invitar
                        if (seleccionado != null) {
                            mandarInvitacion(seleccionado)
                        }
                        dialog.dismiss()
                    }

                    dialog.show()
                } else {
                    showToast("Error al obtener seguidores")
                }
            }

            override fun onFailure(call: Call<SeguidoresResponse>, t: Throwable) {
                showToast("Fallo en conexión")
            }
        })
    }

    private fun mandarInvitacion(usuario: String) {
        val token = Preferencias.obtenerValorString("token", "") ?: ""
        val authHeader = "Bearer $token"

        val request = InvitarPlaylistRequest(playlistId, usuario)

        Log.d("mandarInvitacion", "Token: $token")
        Log.d("mandarInvitacion", "Usuario a invitar: $usuario")
        Log.d("mandarInvitacion", "Request enviado: $request")

        apiService.invitePlaylist(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("mandarInvitacion", "Invitación enviada con éxito")
                    showToast("Se ha enviado la invitación")
                } else {
                    Log.e("mandarInvitacion", "Error en la solicitud: Código ${response.code()}")
                    Log.e("mandarInvitacion", "Mensaje de error: ${response.errorBody()?.string()}")
                    showToast("Error al enviar invitación")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("mandarInvitacion", "Fallo en la conexión: ${t.localizedMessage}", t)
                showToast("Fallo en conexión")
            }
        })
    }
    private fun showSongOptionsPopupMenu(anchorView: View, cancion: CancionP) {
        val popup = PopupMenu(this, anchorView, Gravity.START, 0, R.style.PopupMenuStyle)

        popup.menu.add(0, 1, 0, "Añadir a playlist")
        if (rol?.lowercase() == "creador" || rol?.lowercase() == "participante") {
            popup.menu.add(0, 2, 1, "Eliminar de esta playlist")
        }
        popup.menu.add(0, 3, 2, "Ir al álbum")
        popup.menu.add(0, 4, 3, "Ir al artista")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> addToPlaylist(cancion)
                2 -> removeFromPlaylist(cancion)
                3 -> goToAlbum(cancion)
                4 -> goToArtist(cancion)
            }
            true
        }

        for (i in 0 until popup.menu.size()) {
            val item = popup.menu.getItem(i)
            val spanString = SpannableString(item.title)
            spanString.setSpan(
                TypefaceSpan(ResourcesCompat.getFont(anchorView.context, R.font.poppins_regular)!!),
                0, spanString.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            item.title = spanString
        }

        popup.show()
    }

    private fun addToPlaylist(cancion: CancionP) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.getMisPlaylists(authHeader).enqueue(object : Callback<PlaylistsResponse> {
            override fun onResponse(call: Call<PlaylistsResponse>, response: Response<PlaylistsResponse>) {
                if (response.isSuccessful) {
                    val playlists = response.body()
                    // Filtrar la playlist actual para no mostrarla
                    val filteredPlaylists = playlists?.playlists?.filter { it.id != playlistId }
                    if (filteredPlaylists != null) {
                        showPlaylistSelectionDialog(cancion, filteredPlaylists)
                    }
                } else {
                    showToast("Error al obtener tus playlists")
                }
            }

            override fun onFailure(call: Call<PlaylistsResponse>, t: Throwable) {
                showToast("Error de conexión: ${t.message}")
            }
        })
    }


    private fun showPlaylistSelectionDialog(cancion: CancionP, playlists: List<MisPlaylist>) {
        val dialog = Dialog(this@PlaylistDetail)
        dialog.setContentView(R.layout.dialog_select_playlist)
        dialog.window?.setLayout((Resources.getSystem().displayMetrics.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val searchView = dialog.findViewById<SearchView>(R.id.searchViewPlaylists)
        val recyclerView = dialog.findViewById<RecyclerView>(R.id.recyclerViewPlaylists)

        // Configurar el adaptador
        val adapter = PlaylistSelectionAdapter(playlists) { selectedPlaylist ->
            addSongToSelectedPlaylist(cancion, selectedPlaylist.id)
            dialog.dismiss()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Configurar el buscador
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText)
                return true
            }
        })

        dialog.show()
    }

    private fun addSongToSelectedPlaylist(cancion: CancionP, playlistId: String) {
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


    private fun removeFromPlaylist(cancion: CancionP) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete, null)

        val titulo = dialogView.findViewById<TextView>(R.id.textViewHeadersEscuchas)
        val mensaje = dialogView.findViewById<TextView>(R.id.txtMensaje)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnRechazar)
        val btnEliminar = dialogView.findViewById<Button>(R.id.btnAceptar)

        titulo.text = "Eliminar canción"
        mensaje.text = "¿Estás seguro de que quieres eliminar '${cancion.nombre}' de esta playlist?"

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        alertDialog.window?.setLayout((Resources.getSystem().displayMetrics.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCancelar.setOnClickListener {
            alertDialog.dismiss()
        }

        btnEliminar.setOnClickListener {
            currentPlaylist?.let {
                playlistId?.let { playlistId ->
                    performRemoveFromPlaylist(cancion.id, playlistId)
                }
            }
            alertDialog.dismiss()
        }

        alertDialog.show()
    }




    private fun performRemoveFromPlaylist(cancion: String, playlist: String) {
        val token = Preferencias.obtenerValorString("token", "") ?: ""
        val authHeader = "Bearer $token"
        val request = DeleteFromPlaylistRequest(cancion, playlist)

        apiService.removeSongFromPlaylist(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    showToast("Canción eliminada")
                    // Actualizar la lista
                    playlistId?.let {
                        loadPlaylistData(
                            it,
                            findViewById(R.id.textViewNombrePlaylist),
                            findViewById(R.id.textViewNumCanciones),
                            findViewById(R.id.imageViewPlaylist))
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "sin detalles"
                    Log.e("PlaylistDetail", "Error en la respuesta: $errorBody")
                    Log.e("PlaylistDetail", "Error en la respuesta: ${response.code()}")
                    showToast("Error al eliminar canción")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                showToast("Error de conexión")
            }
        })
    }



    private fun goToAlbum(cancion: CancionP) {
        val intent = Intent(this, AlbumDetail::class.java)
        Log.d("GoToAlbum", "Navegando al album de la cancion: ${cancion.nombre}")
        intent.putExtra("id", cancion.album)
        startActivity(intent)
    }




    private fun goToArtist(cancion: CancionP) {
        val intent = Intent(this, OtroArtista::class.java)
        Log.d("GoToArtist", "Navegando al artista con nombre de usuario: ${cancion.nombreUsuarioArtista}")
        intent.putExtra("nombreUsuario", cancion.nombreUsuarioArtista)
        startActivity(intent)
    }



    private fun expelUser(idPlaylist: String, user: String) {
        val request = ExpelUserRequest(idPlaylist, user)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        // Llamar al servicio API para actualizar el estado del favorito
        apiService.expulsarUsuario(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.e("expulsar", "Expulsado correcto")
                } else {
                    Log.e("expulsar", "Error al expulsar")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("expulsar", "Error al conexion", t)
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
            val coleccionactual = Preferencias.obtenerValorString("coleccionActualId", "")
            if(coleccionactual==playlistId){
                val btnPlayPauseAlbum: ImageButton = findViewById(R.id.btnPlay)
                val icono2 = if (estaReproduciendo) R.drawable.ic_pause_playlist else R.drawable.ic_play_playlist
                btnPlayPauseAlbum.setImageResource(icono2)
            }
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
                Toast.makeText(this@PlaylistDetail, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
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
                    val errorMensaje = response.errorBody()?.string() ?: "Error desconocido"

                    // Mostrar en Logcat
                    Log.e("API_RESPONSE", "Error en la respuesta: Código ${response.code()} - $errorMensaje")

                    // Mostrar en Toast
                    Toast.makeText(this@PlaylistDetail, "Error: $errorMensaje", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<CancionInfoResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)

                // Mostrar en Toast
                Toast.makeText(this@PlaylistDetail, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun cambiarModo() {
        val ordenColeccion = Preferencias.obtenerValorString("ordenColeccionActual", "")
            .split(",")
            .filter { id -> id.isNotEmpty() }

        val modoColeccion =  modo

        val indice = Preferencias.obtenerValorEntero("indexColeccionActual", 0)


        Log.d("Modo", "Lista ids reproduccion: ${ordenColeccion.joinToString(",")}")
        Log.d("Modo", "Indice: $indice")
        Log.d("Modo", "Modo: $modoColeccion")

        val request = ModoRequest(modoColeccion, ordenColeccion, indice)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.change_modo(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("Modo", "Cambiado el modo")

                } else {
                    Log.e("Modo", "Error: ${response.code()} - ${response.errorBody()?.string()}")
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateInicio() {
        Log.d("Delete", "Navegando a la pantalla de inicio")
        val intent = Intent(this, Home::class.java)
        startActivity(intent)
        finish()
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

    override fun onDestroy() {
        super.onDestroy()
        WebSocketEventHandler.eliminarListenerNovedad(listenerNovedad)
        WebSocketEventHandler.eliminarListenerSeguidor(listenerSeguidor)
        WebSocketEventHandler.eliminarListenerInvitacion(listenerInvitacion)
        WebSocketEventHandler.eliminarListenerInteraccion(listenerInteraccion)
    }
}


