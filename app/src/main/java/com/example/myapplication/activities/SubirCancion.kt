package com.example.myapplication.activities

import android.content.DialogInterface
import android.content.Intent
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
import android.widget.ImageButton
import com.example.myapplication.io.request.CrearCancionRequest
import com.example.myapplication.io.response.CloudinaryAudioResponse
import com.example.myapplication.io.response.DeleteAlbumResponse
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.InvitacionPlaylist
import com.example.myapplication.io.response.Novedad
import com.example.myapplication.io.response.Seguidor
import com.example.myapplication.services.WebSocketEventHandler
import org.json.JSONObject


class SubirCancion : AppCompatActivity() {

    private lateinit var spinner: Spinner
    private val listaConPlaceholder = mutableListOf<MiAlbum>()
    private lateinit var albumAdapter: MisAlbumesListAdapter
    private lateinit var layoutCamposCancion: LinearLayout
    private lateinit var apiService: ApiService
    private lateinit var apiServiceCloud: CloudinaryApiService
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
                    Toast.makeText(this@SubirCancion, "Error cargando álbumes", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(this@SubirCancion, "Error al subir la imagen: ${response.errorBody()?.string()}\"", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@SubirCancion, "Error cargando álbumes", Toast.LENGTH_SHORT).show()
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
