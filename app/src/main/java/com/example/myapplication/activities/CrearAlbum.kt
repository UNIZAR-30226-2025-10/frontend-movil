package com.example.myapplication.activities

import android.app.Activity
import android.app.Dialog
import android.content.Intent
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
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import com.bumptech.glide.Glide
import com.example.myapplication.io.request.CrearAlbumRequest
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
                    Log.d("Etiquetas", "Error al obtener las etiquetas: ${response.code()} - ${response.message()}")
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
                            Log.d("uploadImageToCloudinary", "Error: ${response.errorBody()?.string()}")
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
                    Log.d("Crear album", "Error en crear album")
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
                    Log.d("Mis albumes", "Error al obtener los álbumes: ${response.code()} - ${response.message()}")
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
                            Log.d("uploadAudioToCloudinary", "ERROR ${response.errorBody()?.string()}")
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
                }
            }

            override fun onFailure(call: Call<MisAlbumesResponse>, t: Throwable) {
                Log.d("Mis albumes", "Error en la solicitud: ${t.message}")
                callback(false)
            }
        })
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
}
