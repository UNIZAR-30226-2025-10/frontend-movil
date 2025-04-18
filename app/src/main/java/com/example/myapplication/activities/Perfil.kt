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
import android.provider.MediaStore
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
import com.example.myapplication.Adapters.Home.HeaderAdapter
import com.example.myapplication.Adapters.Home.PlaylistsAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.CloudinaryApiService
import com.example.myapplication.io.request.EditarPerfilRequest
import com.example.myapplication.io.response.CloudinaryResponse
import com.example.myapplication.io.response.EditarPerfilResponse
import com.example.myapplication.io.response.GetSignatureResponse
import com.example.myapplication.io.response.InfoSeguidoresResponse
import com.example.myapplication.io.response.PlaylistsResponse
import com.example.myapplication.services.MusicPlayerService
import com.example.myapplication.utils.Preferencias
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Perfil : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var apiServiceCloud: CloudinaryApiService
    private lateinit var recyclerViewPlaylists: RecyclerView
    private lateinit var headerPlaylistsTextView: TextView
    private lateinit var playlistsAdapter: PlaylistsAdapter
    private lateinit var usernameTextView: TextView
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

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.MusicBinder
            musicService = binder.getService()
            serviceBound = true
            handler.post(updateRunnable)
            actualizarIconoPlayPause()
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

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("MiAppPerfil", "PERFIL 0")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.perfil)

        Log.d("MiAppPerfil", "PERFIL 1")

        apiService = ApiService.create()
        apiServiceCloud = CloudinaryApiService.create()
        Log.d("MiAppPerfil", "PERFIL 1.2")
        usernameTextView = findViewById(R.id.username)
        Log.d("MiAppPerfil", "PERFIL 1.2")
        profileImageButton = findViewById(R.id.profileImageButton)
        profileImageView = findViewById(R.id.profileImage)

        Log.d("MiAppPerfil", "PERFIL 1.2")

        // Obtener la URL de la imagen de perfil desde SharedPreferences
        val profileImageUrl = Preferencias.obtenerValorString("fotoPerfil", "DEFAULT")

        Log.d("ProfileImage", "URL de la imagen de perfil: $profileImageUrl")


        // Verificar si la API devolvió "DEFAULT" o si no hay imagen guardada
        if (profileImageUrl.isNullOrEmpty() || profileImageUrl == "DEFAULT") {
            // Cargar la imagen predeterminada
            profileImageButton.setImageResource(R.drawable.ic_profile)
            profileImageView.setImageResource(R.drawable.ic_profile)
        } else {
            // Cargar la imagen desde la URL con Glide
            Glide.with(this)
                .load(profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile) // Imagen por defecto mientras carga
                .error(R.drawable.ic_profile) // Imagen si hay error
                .into(profileImageButton)
            // Cargar la imagen desde la URL con Glide
            Glide.with(this)
                .load(profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile) // Imagen por defecto mientras carga
                .error(R.drawable.ic_profile) // Imagen si hay error
                .into(profileImageView)
        }
        
        headerPlaylistsTextView = findViewById(R.id.textViewHeadersPlaylistsP)
        headerPlaylistsTextView.visibility = View.INVISIBLE

        Log.d("MiAppPerfil", "PERFIL 1.3")

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

        Log.d("MiAppPerfil", "PERFIL 1.4")

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


        Log.d("MiAppPerfil", "PERFIL 2")
        loadProfileData()
        Log.d("MiAppPerfil", "PERFIL 3")

        progressBar = findViewById(R.id.progressBar)
        setupNavigation()
        updateMiniReproductor()
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
        Log.d("MiAppPerfil", "PERFIL show edit 1")
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_edit_profile)

        val window: Window? = dialog.window
        if (window != null) {
            window.setLayout((Resources.getSystem().displayMetrics.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        dialog.setCancelable(true)
        Log.d("MiAppPerfil", "PERFIL show edit 2")

        val editUsername = dialog.findViewById<EditText>(R.id.editUsername)
        profileImageViewDialog = dialog.findViewById(R.id.profileImageDialog)
        val btnSelectImage = dialog.findViewById<Button>(R.id.btnSelectImage)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)

        Log.d("MiAppPerfil", "PERFIL show edit 3")

        editUsername.setText(usernameTextView.text.toString())
        Glide.with(this)
            .load(Preferencias.obtenerValorString("fotoPerfil", "DEFAULT"))
            .placeholder(R.drawable.ic_profile)
            .circleCrop()
            .error(R.drawable.ic_profile)
            .into(profileImageViewDialog!!)

        Log.d("MiAppPerfil", "PERFIL show edit 4")

        btnSelectImage.setOnClickListener {
            openGalleryLauncher.launch("image/*")
        }

        Log.d("MiAppPerfil", "PERFIL show edit 5")
        btnSave.setOnClickListener {
            imageUri?.let { it1 -> getSignatureCloudinary(it1) }
            Log.d("MiAppPerfil", "PERFIL show edit 5.1")
            updateUserProfile(editUsername.text.toString())
            Log.d("MiAppPerfil", "PERFIL show edit 6")

            // Asegurar que la imagen seleccionada no es nula antes de actualizar
            imageUri?.let {
                profileImageView.setImageURI(it) // Display selected image in main profile image view
            }
            Log.d("MiAppPerfil", "PERFIL show edit 7")
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getSignatureCloudinary(imagenURI: Uri){
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        val folder = "perfiles"

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

                        uploadImageToCloudinary(it, imagenURI, folder)
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

    private fun uploadImageToCloudinary(signatureData: GetSignatureResponse, imagenURI: Uri, folder: String) {
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

                            // Guardar URL en preferencias
                            Preferencias.guardarValorString("fotoPerfil", imageUrl)

                            val profileImageUrl = Preferencias.obtenerValorString("fotoPerfil", "")

                            Log.d("ProfileImage", "URL de la imagen de perfil: $profileImageUrl")


                            // Verificar si la API devolvió "DEFAULT" o si no hay imagen guardada
                            if (profileImageUrl.isNullOrEmpty() || profileImageUrl == "DEFAULT") {
                                // Cargar la imagen predeterminada
                                profileImageButton.setImageResource(R.drawable.ic_profile)
                            } else {
                                // Cargar la imagen desde la URL con Glide
                                Glide.with(applicationContext)
                                    .load(profileImageUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_profile) // Imagen por defecto mientras carga
                                    .error(R.drawable.ic_profile) // Imagen si hay error
                                    .into(profileImageButton)
                            }

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

    private fun updateUserProfile(newUsername: String) {
        Log.d("updateUserProfile", "1")
        val imagen = Preferencias.obtenerValorString("fotoPerfil", "")
        Log.d("updateUserProfile", "imag{$imagen}")
        Log.d("updateUserProfile", "user{$newUsername}")
        val request = EditarPerfilRequest(imagen, newUsername)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        Log.d("updateUserProfile", "2")
        apiService.updateProfile(authHeader, request).enqueue(object : Callback<EditarPerfilResponse> {
            override fun onResponse(call: Call<EditarPerfilResponse>, response: Response<EditarPerfilResponse>) {
                if (response.isSuccessful) {
                    usernameTextView.text = newUsername
                    Preferencias.guardarValorString("username", newUsername)
                    showToast("Perfil actualizado")
                } else {
                    Log.d("updateUserProfile", "Error en la solicitud ${response.code()}")
                    showToast("Error al actualizar perfil")
                }
            }

            override fun onFailure(call: Call<EditarPerfilResponse>, t: Throwable) {
                Log.d("updateUserProfile", "Error en la solicitud2")
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun loadProfileData() {
        Log.d("MiAppPerfil", "PERFIL 4")
        getInfo()
        Log.d("MiAppPerfil", "PERFIL 5")
        getMisPlaylists()
        Log.d("MiAppPerfil", "PERFIL 6")
    }

    private fun getInfo() {
        val token = Preferencias.obtenerValorString("token", "")
        apiService.getMisDatosOyente("Bearer $token").enqueue(object : Callback<InfoSeguidoresResponse> {
            override fun onResponse(call: Call<InfoSeguidoresResponse>, response: Response<InfoSeguidoresResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            val nombreperfil = it.nombreUsuario
                            val seguidores = it.numSeguidores
                            val seguidos = it.numSeguidos

                            Log.d("Mi app", "ha tomado info: $nombreperfil")
                            Log.d("Mi app", "ha tomado info: $seguidores")
                            Log.d("Mi app", "ha tomado info: $seguidos")

                            val usernameTextView = findViewById<TextView>(R.id.username)
                            val followersTextView = findViewById<TextView>(R.id.followers)
                            val followingTextView = findViewById<TextView>(R.id.following)

                            usernameTextView.text = nombreperfil
                            followersTextView.text = "$seguidores Seguidores"
                            followingTextView.text = "$seguidos Seguidos"



                        } else {
                            handleErrorCode(it.respuestaHTTP)
                        }
                    } ?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    showToast("Error en la búsqueda: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<InfoSeguidoresResponse>, t: Throwable) {
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

    private fun actualizarIconoPlayPause() {
        if (serviceBound && musicService != null) {
            val estaReproduciendo = musicService!!.isPlaying()
            val icono = if (estaReproduciendo) R.drawable.ic_play else R.drawable.ic_pause
            val stopButton = findViewById<ImageButton>(R.id.stopButton)
            stopButton.setImageResource(icono)
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
            startActivity(Intent(this, Perfil::class.java))
        }
    }
}
