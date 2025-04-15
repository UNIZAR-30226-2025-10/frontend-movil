package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TypefaceSpan
import android.util.Log
import android.view.Gravity
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
import com.example.myapplication.io.request.EditarPerfilRequest
import com.example.myapplication.io.response.*
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

    @SuppressLint("WrongViewCast", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.perfil_artista)

        apiService = ApiService.create()
        apiServiceCloud = CloudinaryApiService.create()

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
}