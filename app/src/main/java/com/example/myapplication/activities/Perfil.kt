package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var headerPlaylistsRecyclerView: RecyclerView
    private lateinit var playlistsAdapter: PlaylistsAdapter
    private lateinit var usernameTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var profileImageButton: ImageView
    private var imageUri: Uri? = null
    private var profileImageViewDialog: ImageView? = null
    private val openGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            profileImageViewDialog?.setImageURI(imageUri)  // Set the selected image in the dialog ImageView
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


        val headersPlaylists = listOf("Mis playlists")
        val headerPlaylistsAdapter = HeaderAdapter(headersPlaylists)
        headerPlaylistsRecyclerView = findViewById(R.id.recyclerViewHeadersPlaylistsP)
        headerPlaylistsRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        headerPlaylistsRecyclerView.adapter = headerPlaylistsAdapter
        headerPlaylistsRecyclerView.visibility = View.INVISIBLE

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

        findViewById<Button>(R.id.editProfile).setOnClickListener { showEditProfileDialog() }
        findViewById<Button>(R.id.botonLogout).setOnClickListener { startActivity(Intent(this, Logout::class.java)) }

        val buttonDeleteAccount: Button = findViewById(R.id.botonDeleteAccount)
        // Mostrar diálogo para eliminar cuenta
        buttonDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }


        Log.d("MiAppPerfil", "PERFIL 2")
        loadProfileData()
        Log.d("MiAppPerfil", "PERFIL 3")
        setupNavigation()
    }

    private fun showDeleteAccountDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_delete_account)

        val window: Window? = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.setCancelable(true)

        val editPassword = dialog.findViewById<EditText>(R.id.editPassword)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val password = editPassword.text.toString().trim()
            if (password.isNotEmpty()) {
                val intent = Intent(this, DeleteAccount::class.java)
                intent.putExtra("password", password)
                startActivity(intent)
                dialog.dismiss()
            } else {
                //showToast("Introduce tu contraseña")
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
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
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
                    //showToast("Get signature correcto")
                } else {
                    Log.d("Signature", "Signature 2")
                   // showToast("Error al Get signature")
                }
            }

            override fun onFailure(call: Call<GetSignatureResponse>, t: Throwable) {
                Log.d("Signature", "Error en la solicitud: ${t.message}")
                //showToast("Error en la solicitud: ${t.message}")
            }
        })
        Log.d("Signature", "Signature FUERA")
    }

    private fun uploadImageToCloudinary(signatureData: GetSignatureResponse, imagenURI: Uri, folder: String) {
        try {

            Log.d("uploadImageToCloudinary", "uploadImageToCloudinary 1")
            // Obtener el stream del archivo a partir del URI
            val inputStream = contentResolver.openInputStream(imagenURI) ?: run {
                //showToast("Error al abrir la imagen")
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

                            //showToast("Imagen subida con éxito")
                        } //?: showToast("Error: Respuesta vacía de Cloudinary")
                    } else {
                        Log.d("uploadImageToCloudinary", "ERROR 3 ${response.errorBody()?.string()}")
                        //showToast("Error al subir la imagen: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<CloudinaryResponse>, t: Throwable) {
                    Log.d("uploadImageToCloudinary", "ERROR 3 ${t.message}")
                    //showToast("Error en la subida: ${t.message}")
                }
            })
        } catch (e: Exception) {
            Log.d("uploadImageToCloudinary", "ERROR 4 ${e.message}")
            //showToast("Error al procesar la imagen: ${e.message}")
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
                    //showToast("Perfil actualizado")
                } else {
                    Log.d("updateUserProfile", "Error en la solicitud ${response.code()}")
                    //showToast("Error al actualizar perfil")
                }
            }

            override fun onFailure(call: Call<EditarPerfilResponse>, t: Throwable) {
                Log.d("updateUserProfile", "Error en la solicitud2")
                //showToast("Error en la solicitud: ${t.message}")
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
                    } //?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    //showToast("Error en la búsqueda: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<InfoSeguidoresResponse>, t: Throwable) {
                //showToast("Error en la solicitud: ${t.message}")
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
                                headerPlaylistsRecyclerView.visibility = View.VISIBLE
                            } else {
                                recyclerViewPlaylists.visibility = View.GONE
                                headerPlaylistsRecyclerView.visibility = View.GONE
                                //showToast("No hay playlists")
                            }

                        } else {
                            handleErrorCode(it.respuestaHTTP)
                        }
                    } //?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    //showToast("Error en la búsqueda: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<PlaylistsResponse>, t: Throwable) {
                //showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun handleErrorCode(statusCode: Int) {
        val message = when (statusCode) {
            400 -> "Error: Correo o usuario en uso"
            500 -> "Error interno del servidor"
            else -> "Error desconocido ($statusCode)"
        }
        //showToast(message)
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
