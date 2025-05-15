package com.example.myapplication.activities

import android.app.Activity
import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.Adapters.CrearAlbum.CancionesAnadidasAdapter
import com.example.myapplication.Adapters.EstadisticasAlbum.CancionEstAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.CloudinaryApiService
import com.example.myapplication.io.request.CrearCancionRequest
import com.example.myapplication.io.request.NuevaCancionRequest
import com.example.myapplication.io.response.CancionEst
import com.example.myapplication.io.response.GetEtiquetasResponse
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.InputStream
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.myapplication.io.request.AudioColeccionRequest
import com.example.myapplication.io.request.AudioRequest
import com.example.myapplication.io.request.CrearAlbumRequest
import com.example.myapplication.io.response.AddReproduccionResponse
import com.example.myapplication.io.response.AudioResponse
import com.example.myapplication.io.response.CancionInfoResponse
import com.example.myapplication.io.response.CloudinaryAudioResponse
import com.example.myapplication.io.response.CloudinaryResponse
import com.example.myapplication.io.response.CrearAlbumResponse
import com.example.myapplication.io.response.DeleteAlbumResponse
import com.example.myapplication.io.response.GetSignatureResponse
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.InvitacionPlaylist
import com.example.myapplication.io.response.MiAlbum
import com.example.myapplication.io.response.MisAlbumesResponse
import com.example.myapplication.io.response.Novedad
import com.example.myapplication.io.response.Seguidor
import com.example.myapplication.managers.ReproduccionTracker
import com.example.myapplication.services.MusicPlayerService
import com.example.myapplication.services.WebSocketEventHandler
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject

class CrearAlbum : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var apiServiceCloud: CloudinaryApiService

    private var artista: String? = null
    private lateinit var tvTitulo: TextView
    private lateinit var tvAlbumAsociado: TextView
    private lateinit var editNombreAlbum: EditText
    private lateinit var layoutSeleccionImagen: LinearLayout
    private lateinit var tvSeleccionarImagen: TextView
    private lateinit var previewImageView: ImageView
    private lateinit var layoutCamposCancion: LinearLayout
    private lateinit var tituloCanciones: TextView
    private lateinit var tvNombreCancion: TextView
    private lateinit var editNombreCancion: EditText
    private lateinit var tvFeatCancion: TextView
    private lateinit var editFeaturings: EditText
    private lateinit var tvEtiquetas: TextView
    private lateinit var btnSeleccionarEtiquetas: Button
    private lateinit var tvAudio: TextView
    private lateinit var btnSeleccionarAudio: Button
    private lateinit var btnSubirCancion: Button
    private lateinit var btnCrearAlbum: Button
    private lateinit var tvAnadirOtraCancion: TextView
    private lateinit var textAnadidas: TextView
    private var etiquetasSeleccionadas = mutableSetOf<String>()
    private var etiquetasDisponibles = mutableListOf<String>()
    private val PICK_AUDIO_REQUEST = 2
    private var audioUri: Uri? = null
    private var imageUri: Uri? = null
    private var yaRedirigidoAlLogin = false

    private val cancionesList = mutableListOf<NuevaCancionRequest>()
    private lateinit var cancionAdapter: CancionesAnadidasAdapter
    private lateinit var recyclerCanciones: RecyclerView
    private var loadingDialog: AlertDialog? = null
    private var nombreFallida: String? = null
    private var featFallida: String? = null
    private lateinit var dot: View

    private val seleccionarImagenLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val uri: Uri? = data?.data
            if (data?.data != null) {
                imageUri = data.data
            }
            uri?.let {
                val inputStream: InputStream? = contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                previewImageView.setImageBitmap(bitmap)
                inputStream?.close()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crear_album)

        apiService = ApiService.create()
        apiServiceCloud = CloudinaryApiService.create()
        dot = findViewById<View>(R.id.notificationDot)
        indexActual = Preferencias.obtenerValorEntero("indexColeccionActual", 0)

        artista = intent.getStringExtra("artista")
        tvTitulo = findViewById(R.id.tvTitulo)
        tvAlbumAsociado = findViewById(R.id.tvAlbumAsociado)
        editNombreAlbum = findViewById(R.id.editNombreAlbum)
        layoutSeleccionImagen = findViewById(R.id.layoutSeleccionImagen)
        tvSeleccionarImagen = findViewById(R.id.tvSeleccionarImagen)
        previewImageView = findViewById(R.id.previewImageView)
        layoutCamposCancion = findViewById(R.id.layoutCamposCancion)
        tituloCanciones = findViewById(R.id.tituloCanciones)
        tvNombreCancion = findViewById(R.id.tvNombreCancion)
        editNombreCancion = findViewById(R.id.editNombreCancion)
        tvFeatCancion = findViewById(R.id.tvFeatCancion)
        editFeaturings = findViewById(R.id.editFeaturings)
        tvEtiquetas = findViewById(R.id.tvEtiquetas)
        btnSeleccionarEtiquetas = findViewById(R.id.btnSeleccionarEtiquetas)
        tvAudio = findViewById(R.id.tvAudio)
        btnSeleccionarAudio = findViewById(R.id.btnSeleccionarAudio)
        btnSubirCancion = findViewById(R.id.btnSubirCancion)
        btnCrearAlbum = findViewById(R.id.btnCrearAlbum)
        tvAnadirOtraCancion = findViewById(R.id.tvAnadirOtraCancion)
        textAnadidas = findViewById(R.id.anadidas)
        recyclerCanciones = findViewById<RecyclerView>(R.id.recyclerCanciones)

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

        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerCanciones.addItemDecoration(divider)


        // Listener para abrir archivos al pulsar el layout de selección de imagen
        layoutSeleccionImagen.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            seleccionarImagenLauncher.launch(intent)
        }

        cancionAdapter = CancionesAnadidasAdapter(cancionesList, artista!!) { position ->
            cancionAdapter.eliminarCancion(position)
            if(cancionesList.size == 0) {
                btnCrearAlbum.visibility = Button.GONE
                layoutCamposCancion.visibility = LinearLayout.VISIBLE
                textAnadidas.visibility = TextView.GONE
                recyclerCanciones.visibility = TextView.GONE
                tvAnadirOtraCancion.visibility = TextView.GONE
            }
        }

        recyclerCanciones.apply {
            layoutManager = LinearLayoutManager(this@CrearAlbum)
            adapter = cancionAdapter
        }

        progressBar = findViewById(R.id.progressBar)
        updateMiniReproductor()
        setupNavigation()

        val btnSeleccionarEtiquetas = findViewById<Button>(R.id.btnSeleccionarEtiquetas)
        btnSeleccionarEtiquetas.setOnClickListener {
            mostrarSeleccionMultipleEtiquetas(etiquetasDisponibles)
        }

        btnSeleccionarAudio.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "audio/*"
            startActivityForResult(intent, PICK_AUDIO_REQUEST)
        }

        // Puedes agregar más listeners aquí, por ejemplo:
        btnCrearAlbum.setOnClickListener {
            subirFotoAlbum()
        }

        btnSubirCancion.setOnClickListener {
            anadirCancion()
        }

        tvAnadirOtraCancion.setOnClickListener {
            layoutCamposCancion.visibility = LinearLayout.VISIBLE
            textAnadidas.visibility = TextView.VISIBLE
            tvAnadirOtraCancion.visibility = TextView.GONE
        }

        obtenerEtiquetas()
    }

    private fun anadirCancion() {
        val nombre = editNombreCancion.text.toString().trim()
        var featsFormateado: String? = null

        if (nombre.isEmpty() || audioUri == null || etiquetasSeleccionadas.isEmpty()) {
            Toast.makeText(this, "Faltan campos obligatorios en la canción", Toast.LENGTH_SHORT).show()
            return
        }

        if (editFeaturings.text.toString().trim().isNotEmpty()) {
            val textoOriginal = editFeaturings.text.toString().trim()

            featsFormateado = textoOriginal
                .split(",")
                .map { it.trim() }
                .joinToString(", ")
        }

        val duracion = obtenerDuracionDesdeUri(audioUri!!)

        val nuevaCancion = NuevaCancionRequest(
            nombre = nombre,
            duracion = duracion,
            audio_file = audioUri!!,
            tags = etiquetasSeleccionadas.toList(),
            artistasFt = featsFormateado
        )

        cancionesList.add(nuevaCancion)
        cancionAdapter.notifyItemInserted(cancionesList.size - 1)

        // Reset campos
        editNombreCancion.text.clear()
        editFeaturings.text.clear()
        etiquetasSeleccionadas.clear()
        audioUri = null
        btnSeleccionarAudio.text = "Ningún archivo seleccionado"

        if (cancionesList.size == 1) {
            btnCrearAlbum.visibility = Button.VISIBLE
        }

        layoutCamposCancion.visibility = LinearLayout.GONE
        textAnadidas.visibility = TextView.GONE
        recyclerCanciones.visibility = TextView.VISIBLE
        tvAnadirOtraCancion.visibility = TextView.VISIBLE

        actualizarBotonEtiquetas()
    }


    private fun obtenerEtiquetas() {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        // Hacemos la solicitud para obtener las etiquetas
        apiService.getEtiquetas(authHeader).enqueue(object : Callback<GetEtiquetasResponse> {
            override fun onResponse(call: Call<GetEtiquetasResponse>, response: Response<GetEtiquetasResponse>) {
                if (response.isSuccessful) {
                    val etiquetas = response.body()?.tags ?: emptyList()

                    etiquetasDisponibles.clear()
                    etiquetasDisponibles.addAll(etiquetas)

                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        val errorBody = response.errorBody()?.string()

                        try {
                            val json = JSONObject(errorBody ?: "")
                            val errorMessage = json.getString("error")

                            if (errorMessage == "Token inválido.") {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@CrearAlbum, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                Toast.makeText(this@CrearAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<GetEtiquetasResponse>, t: Throwable) {
                Log.d("Etiquetas", "Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun mostrarSeleccionMultipleEtiquetas(etiquetas: List<String>) {
        val selectedTags = BooleanArray(etiquetas.size) { index ->
            // Marcar como seleccionado si la etiqueta está en la lista de etiquetas seleccionadas
            etiquetasSeleccionadas.contains(etiquetas[index])
        }


        // Crear el diálogo de selección múltiple
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Selecciona hasta 3 etiquetas")

        // Lista de opciones de selección múltiple
        builder.setMultiChoiceItems(
            etiquetas.toTypedArray(), // Convertir la lista a un array
            selectedTags // Array para guardar el estado de selección de cada ítem
        ) { dialog, which, isChecked ->
            val selectedCount = selectedTags.count { it }

            if (selectedCount > 3) {
                // Si hay más de 3 etiquetas seleccionadas, desmarcar la última opción
                selectedTags[which] = false
                (dialog as AlertDialog).listView.setItemChecked(which, false)
                Toast.makeText(this, "Solo puedes seleccionar hasta 3 etiquetas.", Toast.LENGTH_SHORT).show()
            }
        }

        // Configuración del botón de "Aceptar"
        builder.setPositiveButton("Aceptar") { dialog, which ->
            // Filtrar las etiquetas seleccionadas
            val selectedEtiquetaList = mutableListOf<String>()
            for (i in selectedTags.indices) {
                if (selectedTags[i]) {
                    selectedEtiquetaList.add(etiquetas[i])
                }
            }

            // Aquí puedes hacer lo que quieras con las etiquetas seleccionadas
            if (selectedEtiquetaList.size in 1..3) {
                etiquetasSeleccionadas.clear()
                etiquetasSeleccionadas.addAll(selectedEtiquetaList)
                actualizarBotonEtiquetas()
            } else {
                Toast.makeText(this, "Por favor selecciona entre 1 y 3 etiquetas.", Toast.LENGTH_SHORT).show()
            }
        }

        // Configuración del botón de "Cancelar"
        builder.setNegativeButton("Cancelar") { dialog, which ->
            dialog.dismiss()
        }

        // Mostrar el diálogo
        builder.show()
    }

    fun actualizarBotonEtiquetas() {
        if (etiquetasSeleccionadas.isNotEmpty()) {
            // Si hay etiquetas seleccionadas, muestra las etiquetas
            btnSeleccionarEtiquetas.text = etiquetasSeleccionadas.joinToString(", ")
        } else {
            // Si no hay etiquetas, muestra "Seleccionar Etiquetas"
            btnSeleccionarEtiquetas.text = "Seleccionar Etiquetas"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_AUDIO_REQUEST && resultCode == Activity.RESULT_OK) {
            audioUri = data?.data

            audioUri?.let {
                val fileName = obtenerNombreArchivo(it)
                btnSeleccionarAudio.text  = fileName
            }
        }
    }

    private fun obtenerNombreArchivo(uri: Uri): String {
        var nombre = "Audio seleccionado"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex("_display_name")
                if (index >= 0) {
                    nombre = it.getString(index)
                }
            }
        }
        return nombre
    }

    fun obtenerDuracionDesdeUri(uri: Uri): Int {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, uri)

        val duracionStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()

        return duracionStr?.toIntOrNull()?.div(1000) ?: 0
    }

    private fun subirFotoAlbum() {
        if (editNombreAlbum.text.isEmpty() || imageUri == null) {
            Toast.makeText(this, "Falta el nombre o la portada del álbum", Toast.LENGTH_SHORT).show()
            return
        } else {
            comprobarNombreAlbum(editNombreAlbum.text.toString()) { nombreExiste ->
                if (nombreExiste) {
                    Toast.makeText(this, "Ya existe un álbum con ese nombre", Toast.LENGTH_SHORT).show()
                } else {
                    val builder = AlertDialog.Builder(this)
                    val view = layoutInflater.inflate(R.layout.dialog_loading, null)
                    builder.setView(view)
                    builder.setCancelable(false)

                    loadingDialog = builder.create()
                    loadingDialog?.show()

                    val token = Preferencias.obtenerValorString("token", "")
                    val authHeader = "Bearer $token"
                    val folder = "albumes"

                    apiService.getSignature(authHeader, folder).enqueue(object : Callback<GetSignatureResponse> {
                        override fun onResponse(call: Call<GetSignatureResponse>, response: Response<GetSignatureResponse>) {
                            if (response.isSuccessful) {
                                val signatureResponse = response.body()
                                signatureResponse?.let {
                                    uploadImageToCloudinary(it, imageUri, editNombreAlbum.text.toString(), folder)
                                }
                            } else {
                                if (response.code() == 401 && !yaRedirigidoAlLogin) {
                                    val errorBody = response.errorBody()?.string()

                                    try {
                                        val json = JSONObject(errorBody ?: "")
                                        val errorMessage = json.getString("error")

                                        if (errorMessage == "Token inválido.") {
                                            yaRedirigidoAlLogin = true
                                            val intent = Intent(this@CrearAlbum, Inicio::class.java)
                                            startActivity(intent)
                                            finish()
                                            Toast.makeText(this@CrearAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                                        }

                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                        override fun onFailure(call: Call<GetSignatureResponse>, t: Throwable) {
                            Log.d("Signature", "Error en la solicitud: ${t.message}")
                        }
                    })
                }
            }
        }
    }

    private fun uploadImageToCloudinary(signatureData: GetSignatureResponse, imagenURI: Uri?, nombreAlbum: String, folder: String) {
        try {
            if (imagenURI != null) {
                val inputStream = contentResolver.openInputStream(imagenURI)
                if (inputStream == null) {
                    Toast.makeText(this@CrearAlbum, "Error al abrir la imagen", Toast.LENGTH_SHORT).show()
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
                                val urlDevuelta = it.secure_url
                                Log.d("Cloudinary Upload", "Imagen subida correctamente: $urlDevuelta")
                                crearAlbum(nombreAlbum, urlDevuelta)
                            }
                        } else {
                            if (response.code() == 401 && !yaRedirigidoAlLogin) {
                                val errorBody = response.errorBody()?.string()

                                try {
                                    val json = JSONObject(errorBody ?: "")
                                    val errorMessage = json.getString("error")

                                    if (errorMessage == "Token inválido.") {
                                        yaRedirigidoAlLogin = true
                                        val intent = Intent(this@CrearAlbum, Inicio::class.java)
                                        startActivity(intent)
                                        finish()
                                        Toast.makeText(this@CrearAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                                    }

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }

                    override fun onFailure(call: Call<CloudinaryResponse>, t: Throwable) {
                        Log.d("uploadImageToCloudinary", "Error en la subida ${t.message}")
                    }
                })
            }

        } catch (e: Exception) {
            Log.d("uploadImageToCloudinary", "ERROR ${e.message}")
        }
    }

    private fun crearAlbum(nombreAlbum: String, imageUrl: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        val request = CrearAlbumRequest(nombreAlbum, imageUrl, true)
        apiService.crearAlbum(authHeader, request).enqueue(object : Callback<CrearAlbumResponse> {
            override fun onResponse(call: Call<CrearAlbumResponse>, response: Response<CrearAlbumResponse>) {
                if (response.isSuccessful) {
                    Log.d("Crear album", "Album creado con éxito")
                    obtenerAlbumsActualizado(nombreAlbum)
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        val errorBody = response.errorBody()?.string()

                        try {
                            val json = JSONObject(errorBody ?: "")
                            val errorMessage = json.getString("error")

                            if (errorMessage == "Token inválido.") {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@CrearAlbum, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                Toast.makeText(this@CrearAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            override fun onFailure(call: Call<CrearAlbumResponse>, t: Throwable) {
                Log.d("Crear album", "Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun obtenerAlbumsActualizado (nombreAlbum: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.getMisAlbumesArtista(authHeader).enqueue(object : Callback<MisAlbumesResponse> {
            override fun onResponse(
                call: Call<MisAlbumesResponse>,
                response: Response<MisAlbumesResponse>
            ) {
                if (response.isSuccessful) {
                    val misAlbumes = response.body()?.albumes ?: emptyList()
                    val albumNuevo = misAlbumes.firstOrNull { it.nombre == nombreAlbum }
                    subirCanciones(albumNuevo!!.id)

                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        val errorBody = response.errorBody()?.string()

                        try {
                            val json = JSONObject(errorBody ?: "")
                            val errorMessage = json.getString("error")

                            if (errorMessage == "Token inválido.") {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@CrearAlbum, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                Toast.makeText(this@CrearAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<MisAlbumesResponse>, t: Throwable) {
                Log.d("Mis albumes", "Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun subirCanciones (idAlbum:String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        val folder = "canciones"

        apiService.getSignature(authHeader, folder).enqueue(object : Callback<GetSignatureResponse> {
            override fun onResponse(call: Call<GetSignatureResponse>, response: Response<GetSignatureResponse>) {
                if (response.isSuccessful) {
                    val signatureResponse = response.body()
                    signatureResponse?.let {
                        cancionesList.forEachIndexed { index, cancion ->
                            val esUltimo = index == cancionesList.lastIndex
                            uploadAudioToCloudinary(it, folder, cancion.audio_file, cancion.nombre, cancion.artistasFt, cancion.duracion, cancion.tags, idAlbum, esUltimo)
                        }
                    }
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        val errorBody = response.errorBody()?.string()

                        try {
                            val json = JSONObject(errorBody ?: "")
                            val errorMessage = json.getString("error")

                            if (errorMessage == "Token inválido.") {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@CrearAlbum, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                Toast.makeText(this@CrearAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            override fun onFailure(call: Call<GetSignatureResponse>, t: Throwable) {
                Log.d("Signature", "Error en la solicitud: ${t.message}")
            }
        })

    }

    private fun uploadAudioToCloudinary(signatureData: GetSignatureResponse, folder: String, audioURI: Uri, nombreCancion: String, feats: String?, duracion: Int, tags: List<String>, idAlbum: String, esUltimo: Boolean) {
        try {
            if (audioURI != null) {
                val inputStream = contentResolver.openInputStream(audioURI)
                if (inputStream == null) {
                    Toast.makeText(this@CrearAlbum, "Error al abrir el audio", Toast.LENGTH_SHORT).show()
                    return
                }

                val byteArray = inputStream.readBytes()
                inputStream.close()

                val requestFile = RequestBody.create(MediaType.parse("audio/*"), byteArray)
                val filePart = MultipartBody.Part.createFormData("file", "audio.mp3", requestFile)

                // Crear request bodies para los parámetros
                val apiKey = RequestBody.create(MediaType.parse("text/plain"), signatureData.apiKey)
                val timestamp = RequestBody.create(MediaType.parse("text/plain"), signatureData.timestamp.toString())
                val signature = RequestBody.create(MediaType.parse("text/plain"), signatureData.signature)
                val folderPart = RequestBody.create(MediaType.parse("text/plain"), folder)

                // Llamada a la API de Cloudinary
                apiServiceCloud.uploadAudio(
                    signatureData.cloudName,
                    filePart,
                    apiKey,
                    timestamp,
                    signature,
                    folderPart
                ).enqueue(object : Callback<CloudinaryAudioResponse> {
                    override fun onResponse(call: Call<CloudinaryAudioResponse>, response: Response<CloudinaryAudioResponse>) {
                        if (response.isSuccessful) {
                            response.body()?.let {
                                val audioCloudinaryUrl = it.secure_url
                                Log.d("Cloudinary Upload", "Audio subido correctamente: $audioCloudinaryUrl")
                                crearCancion(nombreCancion, audioCloudinaryUrl, feats, duracion, tags, idAlbum, esUltimo)
                            }
                        } else {
                            if (response.code() == 401 && !yaRedirigidoAlLogin) {
                                val errorBody = response.errorBody()?.string()

                                try {
                                    val json = JSONObject(errorBody ?: "")
                                    val errorMessage = json.getString("error")

                                    if (errorMessage == "Token inválido.") {
                                        yaRedirigidoAlLogin = true
                                        val intent = Intent(this@CrearAlbum, Inicio::class.java)
                                        startActivity(intent)
                                        finish()
                                        Toast.makeText(this@CrearAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                                    }

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }

                    override fun onFailure(call: Call<CloudinaryAudioResponse>, t: Throwable) {
                        Log.d("uploadAudioToCloudinary", "ERROR  ${t.message}")
                    }
                })
            }

        } catch (e: Exception) {
            Log.d("uploadAudioToCloudinary", "ERROR ${e.message}")
        }
    }

    private fun crearCancion (nombreCancion: String, audioUrl:String, feats: String?, duracion: Int, tags: List<String>, idAlbum: String, esUltimo: Boolean) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        val listaFt: List<String> = if (feats != null) {
            feats.split(",").map { it.trim() }
        } else {
            listOf("")
        }

        val request = CrearCancionRequest(nombreCancion, duracion, audioUrl, idAlbum, tags, listaFt, false)
        apiService.crearCancion(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("Crear cancion", "Cancion creada con éxito")
                    if (esUltimo) {
                        if (nombreFallida == null) {
                            loadingDialog?.dismiss()
                            finish()
                        } else {
                            loadingDialog?.dismiss()
                            val dialog = Dialog(this@CrearAlbum)
                            dialog.setContentView(R.layout.dialog_aviso_album)
                            dialog.setCancelable(true)

                            val txtMensaje = dialog.findViewById<TextView>(R.id.txtMensaje)
                            val btnCerrar = dialog.findViewById<ImageView>(R.id.btnCerrar)


                            val palabras = featFallida!!.split(" ")
                            val terceraPalabra = if (palabras.size >= 3) palabras[2] else ""

                            txtMensaje.text = "El álbum se ha creado sin la canción '" + nombreFallida + "' porque el artista " + terceraPalabra + " no existe."

                            btnCerrar.setOnClickListener {
                                nombreFallida = null
                                featFallida = null
                                dialog.dismiss()
                                finish()
                            }

                            dialog.show()
                        }
                    }
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@CrearAlbum, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@CrearAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                    if (cancionesList.size == 1) {
                        apiService.deleteAlbum(authHeader, idAlbum)
                        .enqueue(object : Callback<DeleteAlbumResponse> {
                            override fun onResponse(
                                call: Call<DeleteAlbumResponse>,
                                response: Response<DeleteAlbumResponse>
                            ) {
                                if (response.isSuccessful) {
                                    loadingDialog?.dismiss()
                                    Toast.makeText(
                                        this@CrearAlbum,
                                        "Álbum no creado",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                                        yaRedirigidoAlLogin = true
                                        val intent = Intent(this@CrearAlbum, Inicio::class.java)
                                        startActivity(intent)
                                        finish()
                                        Toast.makeText(this@CrearAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            override fun onFailure(
                                call: Call<DeleteAlbumResponse>,
                                t: Throwable
                            ) {
                                Log.d("Borrar álbum", "Error en la solicitud: ${t.message}")
                            }
                        })

                        val errorBody = response.errorBody()?.string()
                        try {
                            val json = JSONObject(errorBody)
                            val mensajeError = json.getString("error")
                            Toast.makeText(this@CrearAlbum, mensajeError, Toast.LENGTH_LONG).show()

                        } catch (e: Exception) {
                            Toast.makeText(
                                this@CrearAlbum,
                                "Error inesperado al crear canción",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        try {
                            val json = JSONObject(errorBody)
                            val mensajeError = json.getString("error")
                            nombreFallida = nombreCancion
                            featFallida = mensajeError

                        } catch (e: Exception) {
                            Toast.makeText(
                                this@CrearAlbum,
                                "Error inesperado al crear canción",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("Crear cancion", "Error en la solicitud crear: ${t.message}")
            }
        })
    }

    private fun comprobarNombreAlbum(nombreAlbum: String, callback: (Boolean) -> Unit) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.getMisAlbumesArtista(authHeader).enqueue(object : Callback<MisAlbumesResponse> {
            override fun onResponse(call: Call<MisAlbumesResponse>, response: Response<MisAlbumesResponse>) {
                if (response.isSuccessful) {
                    val misAlbumes = response.body()?.albumes ?: emptyList()
                    val nombreExiste = misAlbumes.any { it.nombre == nombreAlbum }
                    callback(nombreExiste)
                } else {
                    Log.d("Mis albumes", "Error al obtener los álbumes: ${response.code()} - ${response.message()}")
                    callback(false)
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@CrearAlbum, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@CrearAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<MisAlbumesResponse>, t: Throwable) {
                Log.d("Mis albumes", "Error en la solicitud: ${t.message}")
                callback(false)
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
                        ReproduccionTracker.startTracking(this@CrearAlbum, id) {
                            notificarReproduccion()
                        }

                        Preferencias.guardarValorString("audioCancionActual", audioResponse.audio)
                        guardarDatoscCancion(id)
                        actualizarIconoPlayPause()
                    }
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@CrearAlbum, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@CrearAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
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
                        ReproduccionTracker.startTracking(this@CrearAlbum, ordenColeccion[indice]) {
                            notificarReproduccion()
                        }
                        guardarDatoscCancion(ordenColeccion[indice])
                        actualizarIconoPlayPause()
                    }
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@CrearAlbum, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@CrearAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
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
                        val intent = Intent(this@CrearAlbum, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@CrearAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
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
                        val intent = Intent(this@CrearAlbum, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@CrearAlbum, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                    val errorMensaje = response.errorBody()?.string() ?: "Error desconocido"

                    // Mostrar en Logcat
                    Log.e("API_RESPONSE", "Error en la respuesta: Código ${response.code()} - $errorMensaje")

                    // Mostrar en Toast
                    Toast.makeText(this@CrearAlbum, "Error: $errorMensaje", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<CancionInfoResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)

                // Mostrar en Toast
                Toast.makeText(this@CrearAlbum, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
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
