package com.example.myapplication.activities

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.Adapters.EstadisticasAlbum.CancionEstAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.CancionEst
import com.example.myapplication.io.response.DeleteAlbumResponse
import com.example.myapplication.io.response.EstadisticasAlbumResponse
import com.example.myapplication.io.response.MiAlbum
import com.example.myapplication.utils.Preferencias
import retrofit2.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.text.NumberFormat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.myapplication.io.CloudinaryApiService
import com.example.myapplication.io.request.AudioColeccionRequest
import com.example.myapplication.io.request.AudioRequest
import com.example.myapplication.io.request.CrearCancionRequest
import com.example.myapplication.io.request.EditarAlbumRequest
import com.example.myapplication.io.response.AddReproduccionResponse
import com.example.myapplication.io.response.AudioResponse
import com.example.myapplication.io.response.CancionInfoResponse
import com.example.myapplication.io.response.CloudinaryResponse
import com.example.myapplication.io.response.GetSignatureResponse
import com.example.myapplication.services.MusicPlayerService
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody


class EstadisticasAlbum : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var text1: TextView
    private lateinit var text2: TextView
    private lateinit var nombreAlbum: TextView
    private lateinit var nCanciones: TextView
    private lateinit var nMegustas: TextView
    private lateinit var nRepros: TextView
    private lateinit var apiService: ApiService
    private lateinit var apiCloudService: CloudinaryApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CancionEstAdapter
    private lateinit var botonEliminar: Button
    private lateinit var botonEditar: Button
    private var idAlbum: String? = null
    private var currentImageUri: Uri? = null
    private var albumImageUrl: String? = null
    private var currentDialog: AlertDialog? = null
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
            //actualizarIconoPlayPause()
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
            //updateProgressBar()
            handler.postDelayed(this, 1000) // cada segundo
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.estadisticas_album)

        idAlbum = intent.getStringExtra("id")
        apiService = ApiService.create()
        apiCloudService = CloudinaryApiService.create()

        indexActual = Preferencias.obtenerValorEntero("indexColeccionActual", 0)

        imageView = findViewById(R.id.centerImage)
        text1 = findViewById(R.id.text1)
        text2 = findViewById(R.id.text2)
        nombreAlbum = findViewById(R.id.nombreAlbum)
        nCanciones = findViewById(R.id.num_canciones)
        nMegustas = findViewById(R.id.me_gustas)
        nRepros = findViewById(R.id.reproducciones)
        botonEliminar = findViewById(R.id.secondButton)
        botonEditar = findViewById(R.id.firstButton)

        botonEditar.setOnClickListener {
            showEditDialog()
        }

        botonEliminar.setOnClickListener {
            eliminarAlbum()
        }

        recyclerView = findViewById(R.id.recyclerCanciones)
        recyclerView.layoutManager = LinearLayoutManager(this)

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

        datosAlbum()
        progressBar = findViewById(R.id.progressBar)
        setupNavigation()
        updateMiniReproductor()
    }

    fun formatearDuracion(segundos: Int): String {
        val minutos = segundos / 60
        val segundosRestantes = segundos % 60
        return "${minutos} minutos ${segundosRestantes} segundos"
    }

    fun formatearFecha(fechaIso: String): String {
        val fecha = LocalDate.parse(fechaIso)
        val formatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        return fecha.format(formatter)
    }

    private fun datosAlbum() {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        if (idAlbum != null) {
            val id = idAlbum as String
            apiService.getEstadisticasAlbum(authHeader, id)
                .enqueue(object : Callback<EstadisticasAlbumResponse> {
                    override fun onResponse(
                        call: Call<EstadisticasAlbumResponse>,
                        response: Response<EstadisticasAlbumResponse>
                    ) {
                        if (response.isSuccessful) {
                            val stats = response.body()
                            stats?.let {
                                val fechaFormateada = formatearFecha(stats.fechaPublicacion).replaceFirstChar { it.uppercase() }
                                text1.text = formatearDuracion(stats.duracion)
                                text2.text = fechaFormateada
                                nombreAlbum.text = stats.nombre
                                albumImageUrl = stats.fotoPortada

                                albumImageUrl?.let { url ->
                                    currentImageUri = Uri.parse(url)
                                }

                                val num = "${stats.canciones.size} canciones"
                                nCanciones.text = num

                                val meGustas = "${stats.favs} Me Gustas"
                                nMegustas.text = meGustas

                                val format = NumberFormat.getInstance(Locale("es", "ES"))
                                val formattedRepros = format.format(stats.reproducciones)
                                val repros = "$formattedRepros Reproducciones"
                                nRepros.text = repros


                                Glide.with(this@EstadisticasAlbum)
                                    .load(it.fotoPortada)
                                    .into(imageView)

                                adapter = CancionEstAdapter(stats.canciones, stats.nombreArtisticoArtista,  object : CancionEstAdapter.OnEstadisticasClickListener{
                                    override fun onVerEstadisticasClick(cancion: CancionEst) {
                                        val intent = Intent(this@EstadisticasAlbum, EstadisticasCancion::class.java).apply {
                                            putExtra("id", cancion.id)
                                            putExtra("nombre", cancion.nombre)
                                            putExtra("album", stats.nombre)
                                            putExtra("duracion", cancion.duracion)
                                            putExtra("reproducciones", cancion.reproducciones)
                                            putExtra("meGustas", cancion.favs)
                                            putExtra("fecha", cancion.fechaPublicacion)
                                            putExtra("foto", cancion.fotoPortada)
                                            putExtra("nPlaylists", cancion.nPlaylists)
                                        }
                                        startActivity(intent)
                                    }
                                })
                                recyclerView.adapter = adapter
                            }
                        } else {
                            if (response.code() == 401 && !yaRedirigidoAlLogin) {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@EstadisticasAlbum, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                Toast.makeText(this@EstadisticasAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                            } else {
                                finish()
                            }
                        }
                    }

                    override fun onFailure(call: Call<EstadisticasAlbumResponse>, t: Throwable) {
                        Log.d("Pedir estadisticas álbum", "Error en la solicitud: ${t.message}")
                        finish()
                    }
                })
        } else {
            finish()
        }
    }

    private fun eliminarAlbum() {

        val builder = AlertDialog.Builder(this)
        builder.setTitle("¿Está seguro de que desea eliminar este álbum?")
        builder.setMessage("Al hacerlo, desaparecerá completamente del sistema. Se perderán todas sus canciones, con sus reproducciones, 'Me gusta' y cualquier playlist en las que hayan sido añadidas. Esta acción es irreversible.")

        builder.setPositiveButton("Eliminar") { _, _ ->
            // Lógica de eliminación real
            val token = Preferencias.obtenerValorString("token", "")
            val authHeader = "Bearer $token"

            if (idAlbum != null) {
                val id = idAlbum as String
                apiService.deleteAlbum(authHeader, id)
                    .enqueue(object : Callback<DeleteAlbumResponse> {
                        override fun onResponse(
                            call: Call<DeleteAlbumResponse>,
                            response: Response<DeleteAlbumResponse>
                        ) {
                            if (response.isSuccessful) {
                                Toast.makeText(this@EstadisticasAlbum, "Álbum eliminado correctamente", Toast.LENGTH_SHORT).show()
                                finish()
                            } else {
                                if (response.code() == 401 && !yaRedirigidoAlLogin) {
                                    yaRedirigidoAlLogin = true
                                    val intent = Intent(this@EstadisticasAlbum, Inicio::class.java)
                                    startActivity(intent)
                                    finish()
                                    Toast.makeText(this@EstadisticasAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        override fun onFailure(call: Call<DeleteAlbumResponse>, t: Throwable) {
                            Log.d("Borrar álbum", "Error en la solicitud: ${t.message}")
                            Toast.makeText(this@EstadisticasAlbum, "Error de red al eliminar el álbum", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()

    }

    private fun showEditDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_album, null)
        val imageViewDialog = dialogView.findViewById<ImageView>(R.id.dialogImageView)
        val editTextDialog = dialogView.findViewById<EditText>(R.id.dialogEditText)

        // Set the current album name and image
        editTextDialog.setText(nombreAlbum.text.toString())
        albumImageUrl?.let { url ->
            Glide.with(this)
                .load(url)
                .into(imageViewDialog)
        }

        val builder = AlertDialog.Builder(this)
            .setTitle("Editar Álbum")
            .setView(dialogView)
            .setPositiveButton("Guardar Cambios") { _, _ ->
                val newName = editTextDialog.text.toString()
                if (currentImageUri != null) {
                    editarAlbum(newName, currentImageUri!!)
                } else {
                    Toast.makeText(this, "Por favor, selecciona una imagen", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)

        val dialog = builder.create()
        currentDialog = dialog

        imageViewDialog.setOnClickListener {
            openGalleryForImage()
        }

        dialog.show()
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryLauncher.launch(intent)
    }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                imageUri?.let {
                    currentImageUri = it
                    currentDialog?.let { dialog ->
                        val imageViewDialog = dialog.findViewById<ImageView>(R.id.dialogImageView)
                        imageViewDialog?.let { imageView ->
                            Glide.with(this)
                                .load(it)
                                .into(imageView)
                        }
                    }
                }

            }
        }


    private fun editarAlbum(nuevoNombre: String, nuevaFoto: Uri) {
        albumImageUrl?.let { url ->
            var actualImageUri = Uri.parse(url)
            if (actualImageUri != nuevaFoto) {
                val token = Preferencias.obtenerValorString("token", "")
                val authHeader = "Bearer $token"
                val folder = "albumes"

                apiService.getSignature(authHeader, folder).enqueue(object : Callback<GetSignatureResponse> {
                    override fun onResponse(call: Call<GetSignatureResponse>, response: Response<GetSignatureResponse>) {
                        if (response.isSuccessful) {
                            val signatureResponse = response.body()
                            signatureResponse?.let {
                                uploadImageToCloudinary(it, nuevaFoto, nuevoNombre, folder)
                            }
                        } else {
                            if (response.code() == 401 && !yaRedirigidoAlLogin) {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@EstadisticasAlbum, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                Toast.makeText(this@EstadisticasAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    override fun onFailure(call: Call<GetSignatureResponse>, t: Throwable) {
                        Log.d("Signature", "Error en la solicitud: ${t.message}")
                    }
                })
            } else {
                guardarCambios(nuevoNombre, albumImageUrl!!)
            }
        }

    }

    private fun uploadImageToCloudinary(signatureData: GetSignatureResponse, imagenURI: Uri?, nuevoNombre: String, folder: String) {
        try {

            if (imagenURI != null) {
                val inputStream = contentResolver.openInputStream(imagenURI)
                if (inputStream == null) {
                    return
                }

                val byteArray = inputStream.readBytes()
                inputStream.close()

                val requestFile = RequestBody.create(MediaType.parse("image/*"), byteArray)
                val filePart = MultipartBody.Part.createFormData("file", "image.jpg", requestFile)

                // Crear request bodies para los parámetros
                val apiKey = RequestBody.create(MediaType.parse("text/plain"), signatureData.apiKey)
                val timestamp = RequestBody.create(MediaType.parse("text/plain"), signatureData.timestamp.toString())
                val signature = RequestBody.create(MediaType.parse("text/plain"), signatureData.signature)
                val folderPart = RequestBody.create(MediaType.parse("text/plain"), folder)

                // Llamada a la API de Cloudinary
                apiCloudService.uploadImage(
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
                                albumImageUrl = it.secure_url
                                guardarCambios(nuevoNombre, albumImageUrl!!)
                            }
                        } else {
                            if (response.code() == 401 && !yaRedirigidoAlLogin) {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@EstadisticasAlbum, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                Toast.makeText(this@EstadisticasAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                            }
                            Log.d("uploadImageToCloudinary", "ERROR ${response.errorBody()?.string()}")
                        }
                    }

                    override fun onFailure(call: Call<CloudinaryResponse>, t: Throwable) {
                        Log.d("uploadImageToCloudinary", "ERROR ${t.message}")
                    }
                })
            }
        } catch (e: Exception) {
            Log.d("uploadImageToCloudinary", "ERROR ${e.message}")
        }
    }

    private fun guardarCambios(nombre: String, foto: String) {
        val request = EditarAlbumRequest(nombre, foto)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        if (idAlbum != null) {
            val id = idAlbum as String
            apiService.changeAlbum(authHeader, id, request)
                .enqueue(object : Callback<DeleteAlbumResponse> {
                    override fun onResponse(
                        call: Call<DeleteAlbumResponse>,
                        response: Response<DeleteAlbumResponse>
                    ) {
                        if (response.isSuccessful) {
                            nombreAlbum.text = nombre
                            Glide.with(this@EstadisticasAlbum)
                                .load(foto)
                                .into(imageView)

                        } else {
                            if (response.code() == 401 && !yaRedirigidoAlLogin) {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@EstadisticasAlbum, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                Toast.makeText(this@EstadisticasAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onFailure(call: Call<DeleteAlbumResponse>, t: Throwable) {
                        Log.d("Borrar álbum", "Error en la solicitud: ${t.message}")
                    }
                })
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
                        val intent = Intent(this@EstadisticasAlbum, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@EstadisticasAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
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
                        val intent = Intent(this@EstadisticasAlbum, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@EstadisticasAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
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
                        val intent = Intent(this@EstadisticasAlbum, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@EstadisticasAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
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

                    }
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@EstadisticasAlbum, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@EstadisticasAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                    val errorMensaje = response.errorBody()?.string() ?: "Error desconocido"

                    // Mostrar en Logcat
                    Log.e("API_RESPONSE", "Error en la respuesta: Código ${response.code()} - $errorMensaje")

                    // Mostrar en Toast
                    Toast.makeText(this@EstadisticasAlbum, "Error: $errorMensaje", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<CancionInfoResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)

                // Mostrar en Toast
                Toast.makeText(this@EstadisticasAlbum, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
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
 }
