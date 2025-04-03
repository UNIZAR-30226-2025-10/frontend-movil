package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.Adapters.Playlist.CancionPAdapter
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.PlaylistRequest
import com.example.myapplication.io.response.CancionP
import com.example.myapplication.io.response.PlaylistResponse
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlaylistDetail : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var recyclerViewCanciones: RecyclerView
    private lateinit var cancionPAdapter: CancionPAdapter

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_playlist)

        Log.d("Playlist", "Entra en la playlist")

        apiService = ApiService.create()

        val playlistId = intent.getStringExtra("id")
        val nombrePlaylist = intent.getStringExtra("nombre")
        val imagenUrl = intent.getStringExtra("imagen")

        val textViewNombre = findViewById<TextView>(R.id.textViewNombrePlaylist)
        val textViewNumCanciones = findViewById<TextView>(R.id.textViewNumCanciones)
        val imageViewPlaylist = findViewById<ImageView>(R.id.imageViewPlaylist)

        // Configuración del RecyclerView
        recyclerViewCanciones = findViewById(R.id.recyclerViewCanciones)
        recyclerViewCanciones.layoutManager = LinearLayoutManager(this)
        cancionPAdapter = CancionPAdapter(listOf()) { cancion ->
            val intent = Intent(this, CancionDetail::class.java)
            intent.putExtra("nombre", cancion.nombre)
            intent.putExtra("artista", cancion.nombreArtisticoArtista)
            intent.putExtra("imagen", cancion.fotoPortada)
            intent.putExtra("id", cancion.id)
            startActivity(intent)
        }
        recyclerViewCanciones.adapter = cancionPAdapter

        textViewNombre.text = nombrePlaylist
        Glide.with(this).load(imagenUrl).into(imageViewPlaylist)

        // Llamada a la API para obtener los datos de la playlist
        playlistId?.let {
            loadPlaylistData(it, textViewNombre, textViewNumCanciones, imageViewPlaylist)
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

        // Cambié el uso del request a pasar solo el ID en la URL con @Query
        apiService.getDatosPlaylist("Bearer $token", playlistId).enqueue(object : Callback<PlaylistResponse> {
            override fun onResponse(call: Call<PlaylistResponse>, response: Response<PlaylistResponse>) {
                if (response.isSuccessful) {
                    val playlist = response.body()?.playlist
                    val canciones = response.body()?.canciones

                    // Actualizar la UI con los datos de la playlist
                    playlist?.let {
                        textViewNombre.text = it.nombrePlaylist
                        textViewNumCanciones.text = "${canciones?.size ?: 0} Canciones"
                        Glide.with(this@PlaylistDetail).load(it.fotoPortada).into(imageViewPlaylist)
                    }

                    // Actualizar RecyclerView con la lista de canciones
                    canciones?.let {
                        cancionPAdapter.updateData(it)
                    }

                } else {
                    // Manejo de error en caso de que la respuesta no sea exitosa
                    //Toast.makeText(this@PlaylistDetail, "Error al obtener los datos de la playlist", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PlaylistResponse>, t: Throwable) {
                // Manejo de error si ocurre un fallo en la conexión
                //Toast.makeText(this@PlaylistDetail, "Error de conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}