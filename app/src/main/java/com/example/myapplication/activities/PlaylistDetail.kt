package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.Adapters.Playlist.CancionPAdapter
import com.example.myapplication.Adapters.Playlist.PlaylistSelectionAdapter
import com.example.myapplication.Adapters.Playlist.SongPlaylistSearchAdapter
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.CloudinaryApiService
import com.example.myapplication.io.request.ActualizarFavoritoRequest
import com.example.myapplication.io.request.AddToPlaylistRequest
import com.example.myapplication.io.request.CambiarPrivacidadPlaylistRequest
import com.example.myapplication.io.request.DeleteFromPlaylistRequest
import com.example.myapplication.io.request.DeletePlaylistRequest
import com.example.myapplication.io.request.InvitarPlaylistRequest
import com.example.myapplication.io.request.UpdatePlaylistRequest
import com.example.myapplication.io.response.ActualizarFavoritoResponse
import com.example.myapplication.io.response.Cancion
import com.example.myapplication.io.response.CancionP
import com.example.myapplication.io.response.CloudinaryResponse
import com.example.myapplication.io.response.GetSignatureResponse
import com.example.myapplication.io.response.MisPlaylist
import com.example.myapplication.io.response.PlaylistP
import com.example.myapplication.io.response.PlaylistResponse
import com.example.myapplication.io.response.PlaylistsResponse
import com.example.myapplication.io.response.SearchPlaylistResponse
import com.example.myapplication.io.response.SeguidoresResponse
import com.example.myapplication.io.response.Usuario
import com.example.myapplication.services.MusicPlayerService
import com.example.myapplication.utils.Preferencias
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlaylistDetail : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var apiServiceCloud: CloudinaryApiService
    private lateinit var recyclerViewCanciones: RecyclerView
    private lateinit var cancionPAdapter: CancionPAdapter
    private lateinit var playlistTextView: TextView
    private lateinit var playlistImageView: ImageView
    private lateinit var playlistImageButton: ImageView
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

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_playlist)

        Log.d("Playlist", "Entra en la playlist")

        apiService = ApiService.create()
        apiServiceCloud = CloudinaryApiService.create()

        playlistId = intent.getStringExtra("id")
        val nombrePlaylist = intent.getStringExtra("nombre")
        val imagenUrl = intent.getStringExtra("imagen")

        val textViewNombre = findViewById<TextView>(R.id.textViewNombrePlaylist)
        val textViewNumCanciones = findViewById<TextView>(R.id.textViewNumCanciones)
        val imageViewPlaylist = findViewById<ImageView>(R.id.imageViewPlaylist)

        playlistTextView = findViewById(R.id.textViewNombrePlaylist)

        playlistImageButton = findViewById(R.id.imageViewPlaylist)


        // Configuración del RecyclerView
        recyclerViewCanciones = findViewById(R.id.recyclerViewCanciones)
        recyclerViewCanciones.layoutManager = LinearLayoutManager(this)
        cancionPAdapter = CancionPAdapter(listOf(),
            { cancion ->
                val intent = Intent(this, CancionDetail::class.java)
                intent.putExtra("nombre", cancion.nombre)
                intent.putExtra("artista", cancion.nombreArtisticoArtista)
                intent.putExtra("imagen", cancion.fotoPortada)
                intent.putExtra("id", cancion.id)
                startActivity(intent)
            },
            { cancion ->
                showSongOptionsDialog(cancion)
            },
            { cancion, isFavorite ->
                actualizarFavorito(cancion.id, isFavorite)
            }
        )
        recyclerViewCanciones.adapter = cancionPAdapter

        textViewNombre.text = nombrePlaylist
        Glide.with(this).load(imagenUrl).into(imageViewPlaylist)

        // Llamada a la API para obtener los datos de la playlist
        playlistId?.let {
            loadPlaylistData(it, textViewNombre, textViewNumCanciones, imageViewPlaylist)
        }

        progressBar = findViewById(R.id.progressBar)
        updateMiniReproductor()

        // Agregar funcionalidad al botón de añadir canción
        val btnMoreOptions: ImageButton = findViewById(R.id.btnMoreOptions)
        btnMoreOptions.setOnClickListener {
                showMoreOptionsDialog()
        }
        val btnAnadirCancion: ImageButton = findViewById(R.id.btnAnadirCancion)
        btnAnadirCancion.setOnClickListener {
            showSearchSongDialog()
        }
        val btnAddUser: ImageButton = findViewById(R.id.btnAddUser)
        btnAddUser.setOnClickListener {
            showInvitarUsuarioDialog()
        }





        // Botones de navegación
        val buttonHome: ImageButton = findViewById(R.id.nav_home)
        val buttonSearch: ImageButton = findViewById(R.id.nav_search)
        val buttonCrear: ImageButton = findViewById(R.id.nav_create)

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

    // Función para realizar la llamada a la API y obtener los datos
    private fun loadPlaylistData(
        playlistId: String,
        textViewNombre: TextView,
        textViewNumCanciones: TextView,
        imageViewPlaylist: ImageView
    ) {
        val token = Preferencias.obtenerValorString("token", "")

        apiService.getDatosPlaylist("Bearer $token", playlistId).enqueue(object : Callback<PlaylistResponse> {
            override fun onResponse(call: Call<PlaylistResponse>, response: Response<PlaylistResponse>) {
                if (response.isSuccessful) {
                    val playlist = response.body()?.playlist
                    val canciones = response.body()?.canciones
                    currentPlaylist = playlist

                    // Actualizar la UI con los datos de la playlist
                    playlist?.let {
                        textViewNombre.text = it.nombrePlaylist
                        Log.d("MiAppPlaylist", "Nombre${textViewNombre.text}")
                        val numCanciones = canciones?.size ?: 0
                        textViewNumCanciones.text = "$numCanciones ${if (numCanciones == 1) "Canción" else "Canciones"}"
                        Glide.with(this@PlaylistDetail).load(it.fotoPortada).into(imageViewPlaylist)
                    }

                    // Actualizar RecyclerView con la lista de canciones
                    canciones?.let {
                        cancionPAdapter.updateData(it)
                    }

                } else {
                    // Manejo de error en caso de que la respuesta no sea exitosa
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

    private fun showSearchSongDialog() {
        Log.d("MiAppPlaylist", "Abrir diálogo de búsqueda de canciones")

        // Crear el diálogo
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_cancion)

        // Configurar la ventana del diálogo
        val window: Window? = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))

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


    private fun showMoreOptionsDialog() {
        Log.d("MiAppPlaylist", "Abrir diálogo de Mas opciones playlist")

        val privacyOption = if (currentPlaylist?.privacidad == true) "Hacer pública" else "Hacer privada"


        val options = arrayOf("Editar lista", "Eliminar lista", privacyOption)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Más opciones")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> showEditPlaylistDialog()
                1 -> showConfirmDeleteDialog()
                2 -> changePrivacyPlaylist()
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun showEditPlaylistDialog() {
        // Log para depuración
        Log.d("MiAppPlaylist", "Mostrar diálogo de edición de lista")

        // Crear el diálogo
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_edit_playlist)

        // Configuración de la ventana del diálogo
        val window: Window? = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
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

        // Cargar la imagen de portada de la playlist al ImageView dentro del diálogo
        Glide.with(this)
            .load(currentPlaylist?.fotoPortada) // Usar la misma fuente de la foto
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
        // Crear el AlertDialog.Builder
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Eliminar Playlist")
        builder.setMessage("¿Estás seguro de que quieres eliminar esta playlist? Esta acción no se puede deshacer.")

        // Botón "Aceptar"
        builder.setPositiveButton("Aceptar") { _, _ ->
            // Lógica para eliminar la playlist
            deletePlaylist()  // Aquí llamas a la función que elimina la playlist
            showToast("Playlist eliminada con éxito")
        }

        // Botón "Cancelar"
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()  // Cerrar el diálogo sin hacer nada
        }

        // Crear y mostrar el diálogo
        val dialog = builder.create()
        dialog.show()
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
        val dialogView = layoutInflater.inflate(R.layout.dialog_invitacion, null)
        val searchView = dialogView.findViewById<SearchView>(R.id.searchViewSeguidores)
        val listView = dialogView.findViewById<ListView>(R.id.listViewSeguidores)

        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setTitle("Invitar usuario")
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .create()

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
                        showToast("Invitado: $seleccionado")
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

    private fun mandarInvitacion(usuario: String){
        val token = Preferencias.obtenerValorString("token", "") ?: ""
        val authHeader = "Bearer $token"

        val request = InvitarPlaylistRequest(currentPlaylist,usuario)

        // Llamar a la API para obtener seguidores
        apiService.invitePlaylist(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    showToast("Se ha enviado la invitacion")

                } else {
                    Log.d("changePrivacyPlaylist", "Error en la solicitud ${response.code()}")
                    showToast("Error al enviar invitacion")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                showToast("Fallo en conexión")
            }
        })
    }

    private fun showSongOptionsDialog(cancion: CancionP) {
        val options = arrayOf(
            "Añadir a playlist",
            "Eliminar de esta playlist",
            "Ir al álbum",
            "Ir al artista"
        )

        AlertDialog.Builder(this)
            .setTitle(cancion.nombre)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> addToPlaylist(cancion)
                    1 -> removeFromPlaylist(cancion)
                    2 -> goToAlbum(cancion)
                    3 -> goToArtist(cancion)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_select_playlist)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val searchView = dialog.findViewById<SearchView>(R.id.searchViewPlaylists)
        val recyclerView = dialog.findViewById<RecyclerView>(R.id.recyclerViewPlaylists)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

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

        btnCancel.setOnClickListener { dialog.dismiss() }

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
        AlertDialog.Builder(this)
            .setTitle("Eliminar canción")
            .setMessage("¿Estás seguro de que quieres eliminar '${cancion.nombre}' de esta playlist?")
            .setPositiveButton("Eliminar") { _, _ ->
                currentPlaylist?.let { playlistId?.let { it1 ->
                    performRemoveFromPlaylist(cancion.id,
                        it1
                    )
                } }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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



    private fun goToAlbum(cancion: CancionP) {}




    private fun goToArtist(cancion: CancionP) {
        val intent = Intent(this, OtroArtista::class.java)
        Log.d("GoToArtist", "Navegando al artista con nombre de usuario: ${cancion.nombreUsuarioArtista}")
        intent.putExtra("nombreUsuario", cancion.nombreUsuarioArtista)
        startActivity(intent)
    }



    private fun actualizarFavorito(id: String, fav: Boolean) {
        val request = ActualizarFavoritoRequest(id, fav)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        // Llamar al servicio API para actualizar el estado del favorito
        apiService.actualizarFavorito(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    showToast("Estado de favorito actualizado")
                    // Regresar a la pantalla anterior
                    val resultIntent = Intent()
                    resultIntent.putExtra("es_favorito", fav) // Devolver el nuevo estado del favorito
                    setResult(RESULT_OK, resultIntent)
                    finish()
                } else {
                    showToast( "Error al actualizar el estado")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                showToast("Error de conexión")
                showToast("Error en la solicitud: ${t.message}")
                Log.e("Favoritos", "Error al actualizar favorito", t)
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateInicio() {
        Log.d("Delete", "Navegando a la pantalla de inicio")
        val intent = Intent(this, Home::class.java)
        startActivity(intent)
        finish()
    }
}


