package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TypefaceSpan
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.Adapters.Buscador.AlbumAdapter
import com.example.myapplication.Adapters.PerfilArtista.AlbumsAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.CloudinaryApiService
import com.example.myapplication.io.request.AudioColeccionRequest
import com.example.myapplication.io.request.AudioRequest
import com.example.myapplication.io.request.EditarPerfilRequest
import com.example.myapplication.io.response.*
import com.example.myapplication.services.MusicPlayerService
import com.example.myapplication.services.WebSocketEventHandler
import com.example.myapplication.utils.Preferencias
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PerfilArtista : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var apiServiceCloud: CloudinaryApiService
    private lateinit var recyclerViewAlbums: RecyclerView
    private lateinit var albumAdapter: AlbumsAdapter
    private lateinit var usernameTextView: TextView
    private lateinit var artisticnameTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var profileImageButton: ImageView
    private var imageUri: Uri? = null
    private var profileImageViewDialog: ImageView? = null

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
        profileImageButton = findViewById(R.id.profileImageButton)
        profileImageView = findViewById(R.id.profileImage)

        // Configurar RecyclerView para álbumes
        recyclerViewAlbums = findViewById(R.id.recyclerViewAlbums)
        recyclerViewAlbums.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        albumAdapter = AlbumsAdapter(mutableListOf()) { album ->
            val intent = Intent(this, EstadisticasAlbum::class.java)
            intent.putExtra("id", album.id)
            startActivity(intent)
        }
        recyclerViewAlbums.adapter = albumAdapter

        // Cargar datos
        loadProfileImage()
        loadProfileData()
        loadArtistAlbums()

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
                        startActivity(Intent(this, Logout::class.java))
                        true
                    }
                    R.id.menu_delete_account -> {
                        showDeleteAccountDialog()
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
                            //if (misAlbums.isNotEmpty()) {
                            recyclerViewAlbums.visibility = View.VISIBLE

                            //} else {
                            //recyclerViewAlbums.visibility = View.GONE
                            //showToast("No tienes álbumes aún")
                            //}
                        } else {
                            handleErrorCode(responseBody.respuestaHTTP)
                        }
                    } ?: showToast("Error: Respuesta vacía del servidor")
                } else {
                    showToast("Error al cargar álbumes: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<GetMisAlbumesResponse>, t: Throwable) {
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
                            findViewById<TextView>(R.id.followers).text = "${it.numSeguidores} Seguidores"
                            findViewById<TextView>(R.id.following).text = "${it.numSeguidos} Seguidos"
                        } else {
                            handleErrorCode(it.respuestaHTTP)
                        }
                    } ?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    showToast("Error en la búsqueda: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<InfoPerfilArtistaResponse>, t: Throwable) {
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

    private fun showEditProfileDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_edit_profile)

        val window: Window? = dialog.window
        window?.setLayout((Resources.getSystem().displayMetrics.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.setCancelable(true)

        val editUsername = dialog.findViewById<EditText>(R.id.editUsername)
        profileImageViewDialog = dialog.findViewById(R.id.profileImageDialog)
        val btnSelectImage = dialog.findViewById<Button>(R.id.btnSelectImage)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)

        editUsername.setText(usernameTextView.text.toString())
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
            imageUri?.let { uri -> getSignatureCloudinary(uri) }
            updateUserProfile(editUsername.text.toString())
            imageUri?.let { uri -> profileImageView.setImageURI(uri) }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getSignatureCloudinary(imagenURI: Uri) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        val folder = "perfiles"

        apiService.getSignature(authHeader, folder).enqueue(object : Callback<GetSignatureResponse> {
            override fun onResponse(call: Call<GetSignatureResponse>, response: Response<GetSignatureResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        uploadImageToCloudinary(it, imagenURI, folder)
                    }
                } else {
                    showToast("Error al obtener firma")
                }
            }

            override fun onFailure(call: Call<GetSignatureResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun uploadImageToCloudinary(signatureData: GetSignatureResponse, imagenURI: Uri, folder: String) {
        try {
            val inputStream = contentResolver.openInputStream(imagenURI) ?: run {
                showToast("Error al abrir la imagen")
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
                            showToast("Imagen subida con éxito")
                        }
                    } else {
                        showToast("Error al subir la imagen")
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

    private fun updateUserProfile(newUsername: String) {
        val imagen = Preferencias.obtenerValorString("profile_image", "")
        val request = EditarPerfilRequest(imagen, newUsername)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.updateProfile(authHeader, request).enqueue(object : Callback<EditarPerfilResponse> {
            override fun onResponse(call: Call<EditarPerfilResponse>, response: Response<EditarPerfilResponse>) {
                if (response.isSuccessful) {
                    usernameTextView.text = newUsername
                    Preferencias.guardarValorString("username", newUsername)
                    showToast("Perfil actualizado")
                } else {
                    showToast("Error al actualizar perfil")
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
                .centerCrop()
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
                        //Toast.makeText(this@Home, respuestaTexto, Toast.LENGTH_LONG).show()

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
                    Toast.makeText(this@PerfilArtista, "Error: $errorMensaje", Toast.LENGTH_LONG).show()
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
        findViewById<ImageButton>(R.id.profileImageButton).setOnClickListener {
            startActivity(Intent(this, PerfilArtista::class.java))
        }
        findViewById<ImageButton>(R.id.nav_home).setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }
        findViewById<ImageButton>(R.id.nav_search).setOnClickListener {
            startActivity(Intent(this, Buscador::class.java))
        }
        findViewById<ImageButton>(R.id.nav_create).setOnClickListener {
            startActivity(Intent(this, PerfilArtista::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadArtistAlbums()
    }
}