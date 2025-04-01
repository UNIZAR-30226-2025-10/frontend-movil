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
import com.example.myapplication.io.request.EditarPerfilRequest
import com.example.myapplication.io.response.EditarPerfilResponse
import com.example.myapplication.io.response.InfoSeguidoresResponse
import com.example.myapplication.io.response.PlaylistsResponse
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Perfil : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var recyclerViewPlaylists: RecyclerView
    private lateinit var headerPlaylistsRecyclerView: RecyclerView
    private lateinit var playlistsAdapter: PlaylistsAdapter
    private lateinit var usernameTextView: TextView
    private lateinit var profileImageView: ImageView
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
        Log.d("MiAppPerfil", "PERFIL 1.2")
        usernameTextView = findViewById(R.id.username)
        Log.d("MiAppPerfil", "PERFIL 1.2")
        profileImageView = findViewById(R.id.profileImageButton)

        Log.d("MiAppPerfil", "PERFIL 1.2")

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
            intent.putExtra("usuario", playlist.nombreUsuarioCreador)
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
            .load(Preferencias.obtenerValorString("profile_image", "DEFAULT"))
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(profileImageViewDialog!!)

        Log.d("MiAppPerfil", "PERFIL show edit 4")

        btnSelectImage.setOnClickListener {
            openGalleryLauncher.launch("image/*")
        }

        Log.d("MiAppPerfil", "PERFIL show edit 5")
        btnSave.setOnClickListener {
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

    private fun updateUserProfile(newUsername: String) {
        val request = EditarPerfilRequest(imageUri.toString(), newUsername)
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
                                headerPlaylistsRecyclerView.visibility = View.VISIBLE
                            } else {
                                recyclerViewPlaylists.visibility = View.GONE
                                headerPlaylistsRecyclerView.visibility = View.GONE
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
}
