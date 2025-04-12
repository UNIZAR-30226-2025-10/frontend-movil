package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
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
import com.example.myapplication.Adapters.Playlist.SongPlaylistSearchAdapter
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.AddToPlaylistRequest
import com.example.myapplication.io.request.PlaylistRequest
import com.example.myapplication.io.response.Cancion
import com.example.myapplication.io.response.CancionP
import com.example.myapplication.io.response.PlaylistResponse
import com.example.myapplication.io.response.SearchPlaylistResponse
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

        // Agregar funcionalidad al botón de añadir canción
        val btnAnadirCancion: ImageButton = findViewById(R.id.btnAnadirCancion)
        btnAnadirCancion.setOnClickListener {
            showSearchSongDialog()
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
                    Toast.makeText(this@PlaylistDetail, "Error al obtener los datos de la playlist", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PlaylistResponse>, t: Throwable) {
                // Manejo de error si ocurre un fallo en la conexión
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
}
