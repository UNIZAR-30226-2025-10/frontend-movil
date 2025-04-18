package com.example.myapplication.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.CloudinaryApiService
import com.example.myapplication.io.request.CreatePlaylistRequest
import com.example.myapplication.io.response.CloudinaryResponse
import com.example.myapplication.io.response.CrearPlaylistResponse
import com.example.myapplication.io.response.GetSignatureResponse
import com.example.myapplication.utils.Preferencias
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CrearPlaylist : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var apiServiceCloud: CloudinaryApiService
    private lateinit var nextButton: Button
    private lateinit var nombreEditText: EditText
    private lateinit var logoImageView: ImageView
    private var selectedImageUri: Uri? = null

    private val openGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                logoImageView.setImageURI(it)
                logoImageView.scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crear_playlist)

        apiService = ApiService.create()
        apiServiceCloud = CloudinaryApiService.create()

        nextButton = findViewById(R.id.nextButton)
        nombreEditText = findViewById(R.id.codigo)
        logoImageView = findViewById(R.id.logoImageView)

        logoImageView.setOnClickListener {
            openGalleryLauncher.launch("image/*")
        }

        nextButton.setOnClickListener {
            val nombrePlaylist = nombreEditText.text.toString().trim()

            if (nombrePlaylist.isNotEmpty()) {
                selectedImageUri?.let {
                    obtenerFirmaCloudinary(nombrePlaylist, it)
                } ?: createPlaylist(nombrePlaylist, "DEFAULT")
            } else {
                Toast.makeText(this, "Por favor ingrese un nombre para la playlist", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun obtenerFirmaCloudinary(nombrePlaylist: String, imagenURI: Uri) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        val folder = "playlists"

        apiService.getSignature(authHeader, folder).enqueue(object : Callback<GetSignatureResponse> {
            override fun onResponse(call: Call<GetSignatureResponse>, response: Response<GetSignatureResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        subirImagenCloudinary(it, imagenURI, folder, nombrePlaylist)
                    }
                } else {
                    Toast.makeText(this@CrearPlaylist, "Error al obtener firma de Cloudinary", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GetSignatureResponse>, t: Throwable) {
                Toast.makeText(this@CrearPlaylist, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun subirImagenCloudinary(signatureData: GetSignatureResponse, imagenURI: Uri, folder: String, nombrePlaylist: String) {
        try {
            val inputStream = contentResolver.openInputStream(imagenURI) ?: run {
                Toast.makeText(this, "Error al abrir la imagen", Toast.LENGTH_SHORT).show()
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
                        val imageUrl = response.body()?.secure_url ?: ""
                        createPlaylist(nombrePlaylist, imageUrl)
                    } else {
                        Toast.makeText(this@CrearPlaylist, "Error al subir imagen", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CloudinaryResponse>, t: Throwable) {
                    Toast.makeText(this@CrearPlaylist, "Fallo al subir imagen: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Error al procesar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createPlaylist(nombre: String, imageUrl: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        val request = CreatePlaylistRequest(nombre, imageUrl)

        apiService.crearPlaylist(authHeader, request).enqueue(object : Callback<CrearPlaylistResponse> {
            override fun onResponse(call: Call<CrearPlaylistResponse>, response: Response<CrearPlaylistResponse>) {
                if (response.isSuccessful) {
                    val playlistId = response.body()?.id
                    Toast.makeText(this@CrearPlaylist, "Playlist creada correctamente", Toast.LENGTH_SHORT).show()
                    val intent = Intent()
                    intent.putExtra("playlist_id", playlistId)  // Usa el ID real aquí
                    setResult(RESULT_OK, intent)
                    finish()
                    finish() // opcional: vuelve a la pantalla anterior
                } else {
                    Toast.makeText(this@CrearPlaylist, "Error al crear la playlist", Toast.LENGTH_SHORT).show()
                    Log.e("CrearPlaylist", "Error: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<CrearPlaylistResponse>, t: Throwable) {
                Toast.makeText(this@CrearPlaylist, "Fallo en la conexión: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("CrearPlaylist", "Fallo al crear playlist", t)
            }
        })
    }
}
