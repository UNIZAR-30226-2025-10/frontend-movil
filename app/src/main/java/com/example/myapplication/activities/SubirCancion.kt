package com.example.myapplication.activities

import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.myapplication.Adapters.SubirCancion.MisAlbumesListAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.CloudinaryApiService
import com.example.myapplication.io.request.CrearAlbumRequest
import com.example.myapplication.io.response.CloudinaryResponse
import com.example.myapplication.io.response.CrearAlbumResponse
import com.example.myapplication.io.response.GetEtiquetasResponse
import com.example.myapplication.io.response.GetSignatureResponse
import com.example.myapplication.io.response.MiAlbum
import com.example.myapplication.io.response.MisAlbumesResponse
import com.example.myapplication.utils.Preferencias
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.ProgressBar
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.myapplication.io.request.AudioColeccionRequest
import com.example.myapplication.io.request.AudioRequest
import com.example.myapplication.io.request.CrearCancionRequest
import com.example.myapplication.io.response.AddReproduccionResponse
import com.example.myapplication.io.response.AudioResponse
import com.example.myapplication.io.response.CancionInfoResponse
import com.example.myapplication.io.response.CloudinaryAudioResponse
import com.example.myapplication.io.response.DeleteAlbumResponse
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.InvitacionPlaylist
import com.example.myapplication.io.response.Novedad
import com.example.myapplication.io.response.Seguidor
import com.example.myapplication.services.MusicPlayerService
import com.example.myapplication.services.WebSocketEventHandler
import org.json.JSONObject


class SubirCancion : AppCompatActivity() {

    private lateinit var spinner: Spinner
    private val listaConPlaceholder = mutableListOf<MiAlbum>()
    private lateinit var albumAdapter: MisAlbumesListAdapter
    private lateinit var layoutCamposCancion: LinearLayout
    private lateinit var apiService: ApiService
    private lateinit var apiServiceCloud: CloudinaryApiService
    private var yaRedirigidoAlLogin = false
    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }
    private var selectedImageView: ImageView? = null
    private var imageUri: Uri? = null

    private lateinit var editNombreCancion: EditText
    private lateinit var editFeaturings: EditText
    private var etiquetasDisponibles = mutableListOf<String>()
    private var etiquetasSeleccionadas = mutableSetOf<String>()
    private lateinit var btnSeleccionarEtiquetas: Button
    private lateinit var btnSeleccionarAudio: Button
    private lateinit var btnSubirCancion: Button
    private val PICK_AUDIO_REQUEST = 2
    private var audioUriGlobal: Uri? = null
    private var duracionGlobal: Double = 0.0
    private var albumSeleccionado: MiAlbum? = null
    private lateinit var dot: View
    private var loadingDialog: AlertDialog? = null

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
        setContentView(R.layout.subir_cancion)

        indexActual = Preferencias.obtenerValorEntero("indexColeccionActual", 0)

        dot = findViewById<View>(R.id.notificationDot)
        spinner = findViewById(R.id.spinnerAlbums)
        layoutCamposCancion = findViewById(R.id.layoutCamposCancion)

        editNombreCancion = findViewById(R.id.editNombreCancion)
        editFeaturings = findViewById(R.id.editFeaturings)
        btnSeleccionarEtiquetas = findViewById(R.id.btnSeleccionarEtiquetas)
        btnSeleccionarAudio = findViewById(R.id.btnSeleccionarAudio)
        btnSubirCancion = findViewById(R.id.btnSubirCancion)

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

        val btnSeleccionarEtiquetas = findViewById<Button>(R.id.btnSeleccionarEtiquetas)
        btnSeleccionarEtiquetas.setOnClickListener {
            mostrarSeleccionMultipleEtiquetas(etiquetasDisponibles)
        }

        apiService = ApiService.create()
        apiServiceCloud = CloudinaryApiService.create()
        progressBar = findViewById(R.id.progressBar)
        updateMiniReproductor()
        setupNavigation()
        obtenerAlbums()

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedAlbum = parentView.selectedItem as MiAlbum
                layoutCamposCancion.visibility = View.GONE

                if (selectedAlbum.id == "") {
                    albumSeleccionado = null
                    layoutCamposCancion.visibility = View.GONE
                    if (selectedAlbum.nombre == " + Crear Álbum") {
                        mostrarCrearAlbumDialog()
                    }
                } else {
                    albumSeleccionado = selectedAlbum
                    obtenerEtiquetas()
                    layoutCamposCancion.visibility = View.VISIBLE
                }
            }
            override fun onNothingSelected(parentView: AdapterView<*>) {}
        }


        btnSeleccionarAudio.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "audio/*"
            startActivityForResult(intent, PICK_AUDIO_REQUEST)
        }

        btnSubirCancion.setOnClickListener {
            if (albumSeleccionado == null) {
                Toast.makeText(this, "Selecciona un álbum", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val nombreCancion = editNombreCancion.text.toString().trim()

            if (nombreCancion.isEmpty() || etiquetasSeleccionadas.isEmpty() || audioUriGlobal == null) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val builder = AlertDialog.Builder(this)
            val view = layoutInflater.inflate(R.layout.dialog_loading, null)
            val txt: TextView = view.findViewById(R.id.loadingText)
            txt.text = "Subiendo canción"

            builder.setView(view)
            builder.setCancelable(false)

            loadingDialog = builder.create()
            loadingDialog?.show()

            val inputText = editFeaturings.text.toString()
            val artistasFt = inputText.split(",").map { it.trim() }
            val artistasFtFinal = if (artistasFt.isEmpty()) listOf("") else artistasFt

            //Subir audio a cloudinary y despues crear la cancion
            val token = Preferencias.obtenerValorString("token", "")
            val authHeader = "Bearer $token"
            val folder = "canciones"

            apiService.getSignature(authHeader, folder).enqueue(object : Callback<GetSignatureResponse> {
                override fun onResponse(call: Call<GetSignatureResponse>, response: Response<GetSignatureResponse>) {
                    if (response.isSuccessful) {
                        val signatureResponse = response.body()
                        signatureResponse?.let {
                            uploadAudioToCloudinary(it, audioUriGlobal, folder, nombreCancion, artistasFtFinal)
                        }
                    } else {
                        if (response.code() == 401 && !yaRedirigidoAlLogin) {
                            yaRedirigidoAlLogin = true
                            val intent = Intent(this@SubirCancion, Inicio::class.java)
                            startActivity(intent)
                            finish()
                            Toast.makeText(this@SubirCancion, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onFailure(call: Call<GetSignatureResponse>, t: Throwable) {
                    loadingDialog?.dismiss()
                    Log.d("Signature", "Error en la solicitud: ${t.message}")
                }
            })
        }
    }

    private fun obtenerAlbums() {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.getMisAlbumesArtista(authHeader).enqueue(object : Callback<MisAlbumesResponse> {
            override fun onResponse(
                call: Call<MisAlbumesResponse>,
                response: Response<MisAlbumesResponse>
            ) {
                if (response.isSuccessful) {
                    val placeholderAlbum = MiAlbum(
                        id = "",
                        nombre = "Seleccione el álbum al que desea asociar la canción",
                        fotoPortada = ""
                    )

                    val crearAlbumOption = MiAlbum(
                        id = "",
                        nombre = " + Crear Álbum",
                        fotoPortada = ""
                    )

                    val misAlbumes = response.body()?.albumes ?: emptyList()
                    listaConPlaceholder.clear()
                    listaConPlaceholder.addAll(listOf(placeholderAlbum, crearAlbumOption))
                    listaConPlaceholder.addAll(misAlbumes)

                    albumAdapter = MisAlbumesListAdapter(this@SubirCancion, listaConPlaceholder)
                    spinner.adapter = albumAdapter

                    spinner.post {
                        spinner.setDropDownWidth(950)
                    }
                } else {
                    Log.d("Mis albumes", "Error al obtener los álbumes: ${response.code()} - ${response.message()}")
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@SubirCancion, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@SubirCancion, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<MisAlbumesResponse>, t: Throwable) {
                Log.d("Mis albumes", "Error en la solicitud: ${t.message}")
                Toast.makeText(this@SubirCancion, "Error en la solicitud: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun mostrarCrearAlbumDialog() {
        val builder = AlertDialog.Builder(this)
        val titleTextView = TextView(this)
        titleTextView.text = "Crear Álbum"
        titleTextView.setPadding(34, 34, 34, 24)
        titleTextView.setTextColor(resources.getColor(R.color.fondo))  // Cambia #fondo por el color real que deseas
        titleTextView.textSize = 18f
        titleTextView.setTypeface(null, Typeface.BOLD)
        builder.setCustomTitle(titleTextView)

        // Crear el campo de texto para el nombre del álbum
        val input = EditText(this)
        input.hint = "Ingrese el nombre del álbum"
        input.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        input.setPadding(34, 34, 34, 34) // Agregar padding al EditText

        // Crear el ImageView para mostrar la imagen seleccionada
        val imageView = ImageView(this)
        imageView.id = View.generateViewId()
        imageView.layoutParams = ViewGroup.LayoutParams(400, 400)
        imageView.setPadding(34, 34, 34, 34)
        imageView.setImageResource(R.drawable.no_cancion)
        val drawablePreview = resources.getDrawable(R.drawable.rounded_image_border)
        imageView.background = drawablePreview
        selectedImageView = imageView

        // Crear el botón para seleccionar una foto
        val selectImageButton = Button(this)
        selectImageButton.text = "Seleccionar Foto"
        selectImageButton.setBackgroundColor(resources.getColor(R.color.blueNuestro)) // Color de fondo
        selectImageButton.setTextColor(resources.getColor(android.R.color.white)) // Color del texto
        selectImageButton.setPadding(34, 34, 34, 34) // Agregar padding al botón
        selectImageButton.gravity = Gravity.CENTER // Centrar el texto dentro del botón

        val drawable = resources.getDrawable(R.drawable.button_rounded)
        selectImageButton.background = drawable

        // Configurar el click del botón para seleccionar una imagen
        selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
            selectImageButton.gravity = Gravity.CENTER
            selectImageButton.requestLayout()
        }

        // Crear el LinearLayout que contendrá los elementos
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(34, 34, 34, 34) // Agregar padding general al layout
        layout.gravity = Gravity.CENTER
        layout.addView(input)
        layout.addView(imageView)
        layout.addView(selectImageButton)

        builder.setView(layout)

        // Establecer los botones del diálogo
        builder.setPositiveButton("Crear") { _, _ ->
            val nombreAlbum = input.text.toString()
            if (nombreAlbum.isNotEmpty() && imageView.drawable != resources.getDrawable(R.drawable.no_cancion)) {

                comprobarNombreAlbum(nombreAlbum) { nombreExiste ->
                    if (nombreExiste) {
                        Toast.makeText(this, "Ya existe un álbum con ese nombre", Toast.LENGTH_SHORT).show()
                    } else {
                        //Subir foto a cloudinary y después crear el álbum
                        val token = Preferencias.obtenerValorString("token", "")
                        val authHeader = "Bearer $token"
                        val folder = "albumes"

                        apiService.getSignature(authHeader, folder).enqueue(object : Callback<GetSignatureResponse> {
                            override fun onResponse(call: Call<GetSignatureResponse>, response: Response<GetSignatureResponse>) {
                                if (response.isSuccessful) {
                                    val signatureResponse = response.body()
                                    signatureResponse?.let {
                                        uploadImageToCloudinary(it, imageUri, nombreAlbum, folder)
                                    }
                                } else {
                                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                                        yaRedirigidoAlLogin = true
                                        val intent = Intent(this@SubirCancion, Inicio::class.java)
                                        startActivity(intent)
                                        finish()
                                        Toast.makeText(this@SubirCancion, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            override fun onFailure(call: Call<GetSignatureResponse>, t: Throwable) {
                                Log.d("Signature", "Error en la solicitud: ${t.message}")
                            }
                        })
                    }
                }
            } else {
                Toast.makeText(this, "El nombre y la foto del álbum no pueden estar vacíos", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar") { _, _ ->
            spinner.setSelection(0)
        }

        builder.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.data
            selectedImageView?.setImageURI(imageUri) // Mostrar la imagen seleccionada en el ImageView
        }

        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null) {
            val audioUri = data?.data
            audioUriGlobal = audioUri

            if (audioUri != null) {

                /*val duracion = obtenerDuracionAudio(audioUri)
                val duracionEnSegundos = duracion / 1000 // Convertir de milisegundos a segundos
                duracionGlobal = duracionEnSegundos*/

                // Intentar obtener el nombre del archivo desde la URI
                val cursor = contentResolver.query(
                    audioUri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                        if (columnIndex != -1) {
                            val fileName = it.getString(columnIndex)
                            btnSeleccionarAudio.text = fileName
                        } else {
                            btnSeleccionarAudio.text = "Archivo seleccionado"
                        }
                    }
                }
            }
        }

    }

    private fun crearAlbum(nombreAlbum: String, imageUrl: String, nombreCancion: String, audioUrl: String, feats: List<String>) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        Log.d("Crear album", "En crear album")

        val request = CrearAlbumRequest(nombreAlbum, imageUrl, false)
        apiService.crearAlbum(authHeader, request).enqueue(object : Callback<CrearAlbumResponse> {
            override fun onResponse(call: Call<CrearAlbumResponse>, response: Response<CrearAlbumResponse>) {
                if (response.isSuccessful) {
                    Log.d("Crear album", "Album creado con éxito")
                    obtenerAlbumsActualizado(nombreAlbum, nombreCancion, audioUrl, feats)
                } else {
                    loadingDialog?.dismiss()
                    Log.d("Crear album", "Error en crear album")
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@SubirCancion, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@SubirCancion, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onFailure(call: Call<CrearAlbumResponse>, t: Throwable) {
                loadingDialog?.dismiss()
                Log.d("Crear album", "Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun uploadImageToCloudinary(signatureData: GetSignatureResponse, imagenURI: Uri?, nombreAlbum: String, folder: String) {
        try {

            if (imagenURI != null) {
                val inputStream = contentResolver.openInputStream(imagenURI)
                if (inputStream == null) {
                    Toast.makeText(this@SubirCancion, "Error al abrir la imagen", Toast.LENGTH_SHORT).show()
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
                                val imageUrl = it.secure_url
                                Log.d("Cloudinary Upload", "Imagen subida correctamente: $imageUrl")
                                simularCrearAlbum(nombreAlbum, imageUrl)
                            } ?: Toast.makeText(this@SubirCancion, "Error: respuesta vacía de Cloudinary", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.d("uploadImageToCloudinary", "ERROR 3 ${response.errorBody()?.string()}")
                            if (response.code() == 401 && !yaRedirigidoAlLogin) {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@SubirCancion, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                Toast.makeText(this@SubirCancion, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onFailure(call: Call<CloudinaryResponse>, t: Throwable) {
                        Log.d("uploadImageToCloudinary", "ERROR 3 ${t.message}")
                        Toast.makeText(this@SubirCancion, "Error en la subida", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this@SubirCancion, "La URI de la imagen es nula", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.d("uploadImageToCloudinary", "ERROR 4 ${e.message}")
            Toast.makeText(this@SubirCancion, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun simularCrearAlbum(nombreAlbum: String, imageUrl: String) {
        val nuevoAlbum = MiAlbum(
            id = "idProvisional",
            nombre = nombreAlbum,
            fotoPortada = imageUrl
        )

        listaConPlaceholder.add(nuevoAlbum)
        albumAdapter.notifyDataSetChanged()
        spinner.setSelection(listaConPlaceholder.indexOf(nuevoAlbum))
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
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@SubirCancion, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@SubirCancion, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
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

    private fun uploadAudioToCloudinary(signatureData: GetSignatureResponse, audioURI: Uri?, folder: String, nombreCancion: String, feats:List<String>) {
        try {
            if (audioURI != null) {
                val inputStream = contentResolver.openInputStream(audioURI)
                if (inputStream == null) {
                    loadingDialog?.dismiss()
                    Toast.makeText(this@SubirCancion, "Error al abrir el audio", Toast.LENGTH_SHORT).show()
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
                                duracionGlobal = it.duration
                                Log.d("Cloudinary Upload", "Audio subido correctamente: $audioCloudinaryUrl")

                                if (albumSeleccionado is MiAlbum) {
                                    val album = albumSeleccionado as MiAlbum
                                    if (album.id == "idProvisional") {
                                        crearAlbum(album.nombre, album.fotoPortada, nombreCancion, audioCloudinaryUrl, feats)
                                    } else {
                                        crearCancion(nombreCancion, audioCloudinaryUrl, feats, true)
                                    }
                                }
                            } ?: Toast.makeText(this@SubirCancion, "Error: respuesta vacía de Cloudinary", Toast.LENGTH_SHORT).show()
                        } else {
                            loadingDialog?.dismiss()
                            Log.d("uploadAudioToCloudinary", "ERROR 3 ${response.errorBody()?.string()}")
                            if (response.code() == 401 && !yaRedirigidoAlLogin) {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@SubirCancion, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                Toast.makeText(this@SubirCancion, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onFailure(call: Call<CloudinaryAudioResponse>, t: Throwable) {
                        loadingDialog?.dismiss()
                        Log.d("uploadAudioToCloudinary", "ERROR 3 ${t.message}")
                    }
                })
            } else {
                loadingDialog?.dismiss()
                Toast.makeText(this@SubirCancion, "La URI de la imagen es nula", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            loadingDialog?.dismiss()
            Log.d("uploadAudioToCloudinary", "ERROR 4 ${e.message}")
            Toast.makeText(this@SubirCancion, "Error al procesar el audio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun crearCancion(nombreCancion: String, audioUrl: String, feats: List<String>, existia: Boolean) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        Log.d("Crear canción", "En crear cancion")

        val listaEtiquetas: List<String> = etiquetasSeleccionadas.toList()
        val durationEntera = duracionGlobal.toInt()

        if (albumSeleccionado is MiAlbum) {

            val album = albumSeleccionado as MiAlbum
            val request = CrearCancionRequest(nombreCancion, durationEntera, audioUrl, album.id, listaEtiquetas, feats, true)
            Log.d("Crear cancion", album.id)
            apiService.crearCancion(authHeader, request).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Log.d("Crear cancion", "Cancion creada con éxito")
                        Toast.makeText(this@SubirCancion, "Canción subida con éxito", Toast.LENGTH_SHORT).show()
                        loadingDialog?.dismiss()
                        finish()
                    } else {
                        albumSeleccionado = spinner.selectedItem as MiAlbum
                        if (existia == false) {
                            apiService.deleteAlbum(authHeader, albumSeleccionado!!.id)
                                .enqueue(object : Callback<DeleteAlbumResponse> {
                                    override fun onResponse(
                                        call: Call<DeleteAlbumResponse>,
                                        response: Response<DeleteAlbumResponse>
                                    ) {
                                        if (response.isSuccessful) {}
                                    }

                                    override fun onFailure(
                                        call: Call<DeleteAlbumResponse>,
                                        t: Throwable
                                    ) {
                                        Log.d("Borrar álbum", "Error en la solicitud: ${t.message}")
                                    }
                                })
                        }

                        loadingDialog?.dismiss()
                        val errorBody = response.errorBody()?.string()
                        try {
                            val json = JSONObject(errorBody)
                            val mensajeError = json.getString("error")
                            Toast.makeText(this@SubirCancion, mensajeError, Toast.LENGTH_LONG).show()

                        } catch (e: Exception) {
                            Toast.makeText(
                                this@SubirCancion,
                                "Error inesperado al crear canción",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        if (response.code() == 401 && !yaRedirigidoAlLogin) {
                            yaRedirigidoAlLogin = true
                            val intent = Intent(this@SubirCancion, Inicio::class.java)
                            startActivity(intent)
                            finish()
                            Toast.makeText(this@SubirCancion, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    loadingDialog?.dismiss()
                    Log.d("Crear cancion", "Error en la solicitud crear: ${t.message}")
                }
            })
        }
    }

    private fun obtenerAlbumsActualizado(nombreAlbum: String, nombreCancion: String, audioUrl: String, feats: List<String>) {
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
                    albumSeleccionado = albumNuevo
                    crearCancion(nombreCancion,audioUrl,feats, false)

                } else {
                    loadingDialog?.dismiss()
                    Log.d("Mis albumes", "Error al obtener los álbumes: ${response.code()} - ${response.message()}")
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@SubirCancion, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@SubirCancion, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<MisAlbumesResponse>, t: Throwable) {
                loadingDialog?.dismiss()
                Log.d("Mis albumes", "Error en la solicitud: ${t.message}")
                Toast.makeText(this@SubirCancion, "Error en la solicitud: ${t.message}", Toast.LENGTH_SHORT).show()
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
                        val intent = Intent(this@SubirCancion, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@SubirCancion, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
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
                        val intent = Intent(this@SubirCancion, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@SubirCancion, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
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
                        val intent = Intent(this@SubirCancion, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@SubirCancion, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
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
                        val intent = Intent(this@SubirCancion, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@SubirCancion, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
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
                        val intent = Intent(this@SubirCancion, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@SubirCancion, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                    val errorMensaje = response.errorBody()?.string() ?: "Error desconocido"

                    // Mostrar en Logcat
                    Log.e("API_RESPONSE", "Error en la respuesta: Código ${response.code()} - $errorMensaje")

                    // Mostrar en Toast
                    Toast.makeText(this@SubirCancion, "Error: $errorMensaje", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<CancionInfoResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)

                // Mostrar en Toast
                Toast.makeText(this@SubirCancion, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
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
}
