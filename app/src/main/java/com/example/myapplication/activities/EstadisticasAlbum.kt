package com.example.myapplication.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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
import com.example.myapplication.io.CloudinaryApiService
import com.example.myapplication.io.request.CrearCancionRequest
import com.example.myapplication.io.request.EditarAlbumRequest
import com.example.myapplication.io.response.CloudinaryResponse
import com.example.myapplication.io.response.GetSignatureResponse
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.estadisticas_album)

        idAlbum = intent.getStringExtra("id")
        apiService = ApiService.create()
        apiCloudService = CloudinaryApiService.create()

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

        datosAlbum()
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
                            finish()
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
                                Toast.makeText(this@EstadisticasAlbum, "Error al eliminar el álbum", Toast.LENGTH_SHORT).show()
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

                        }
                    }

                    override fun onFailure(call: Call<DeleteAlbumResponse>, t: Throwable) {
                        Log.d("Borrar álbum", "Error en la solicitud: ${t.message}")
                    }
                })
        }
    }
 }
