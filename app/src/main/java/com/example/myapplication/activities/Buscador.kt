package com.example.myapplication.activities

import HeaderAdapter
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.Adapters.Buscador.AlbumAdapter
import com.example.myapplication.Adapters.Buscador.ArtistaAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.BuscadorResponse
import com.example.myapplication.utils.Preferencias
import com.example.myapplication.Adapters.Buscador.CancionAdapter
import com.example.myapplication.Adapters.Buscador.PerfilAdapter
import com.example.myapplication.Adapters.Buscador.PlaylistAdapter
import com.example.myapplication.services.MusicPlayerService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Buscador : AppCompatActivity() {
    private lateinit var searchEditText: EditText
    private lateinit var recyclerViewCancion: RecyclerView
    private lateinit var recyclerViewArtista: RecyclerView
    private lateinit var recyclerViewAlbum: RecyclerView
    private lateinit var recyclerViewPlaylist: RecyclerView
    private lateinit var recyclerViewPerfil: RecyclerView
    private lateinit var headerCancionesTextView: TextView
    private lateinit var headerArtistasTextView: TextView
    private lateinit var headerAlbumesTextView: TextView
    private lateinit var headerPlaylistsTextView: TextView
    private lateinit var headerPerfilesTextView: TextView
    private lateinit var cancionAdapter: CancionAdapter
    private lateinit var artistaAdapter: ArtistaAdapter
    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var playlistAdapter: PlaylistAdapter
    private lateinit var perfilAdapter: PerfilAdapter
    private lateinit var apiService: ApiService
    private lateinit var radioOptions: RadioGroup
    private lateinit var radioTodos: RadioButton
    private lateinit var radioCanciones: RadioButton
    private lateinit var radioArtistas: RadioButton
    private lateinit var radioAlbumes: RadioButton
    private lateinit var radioPlaylists: RadioButton
    private lateinit var radioPerfiles: RadioButton

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
        setContentView(R.layout.buscador)

        apiService = ApiService.create()

        val profileImageButton = findViewById<ImageButton>(R.id.profileImageButton)

        // Obtener la URL de la imagen de perfil desde SharedPreferences
        val profileImageUrl = Preferencias.obtenerValorString("fotoPerfil", "")

        Log.d("ProfileImage", "URL de la imagen de perfil: $profileImageUrl")


        // Verificar si la API devolvió "DEFAULT" o si no hay imagen guardada
        if (profileImageUrl.isNullOrEmpty() || profileImageUrl == "DEFAULT") {
            // Cargar la imagen predeterminada
            profileImageButton.setImageResource(R.drawable.ic_profile)
        } else {
            // Cargar la imagen desde la URL con Glide
            Glide.with(this)
                .load(profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile) // Imagen por defecto mientras carga
                .error(R.drawable.ic_profile) // Imagen si hay error
                .into(profileImageButton)
        }

        // Configurar RecyclerView para los encabezados
        headerCancionesTextView = findViewById(R.id.textViewHeaderCanciones)
        headerCancionesTextView.visibility = View.INVISIBLE

        headerAlbumesTextView = findViewById(R.id.textViewHeaderAlbumes)
        headerAlbumesTextView.visibility = View.INVISIBLE

        headerArtistasTextView = findViewById(R.id.textViewHeaderArtistas)
        headerArtistasTextView.visibility = View.INVISIBLE

        headerPlaylistsTextView = findViewById(R.id.textViewHeaderPlaylists)
        headerPlaylistsTextView.visibility = View.INVISIBLE

        headerPerfilesTextView = findViewById(R.id.textViewHeaderPerfiles)
        headerPerfilesTextView.visibility = View.INVISIBLE

        searchEditText = findViewById(R.id.searchInput)
        recyclerViewCancion = findViewById(R.id.recyclerViewCanciones)
        recyclerViewCancion.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerViewArtista = findViewById(R.id.recyclerViewArtistas)
        recyclerViewArtista.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerViewAlbum = findViewById(R.id.recyclerViewAlbumes)
        recyclerViewAlbum.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerViewPlaylist = findViewById(R.id.recyclerViewPlaylists)
        recyclerViewPlaylist.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerViewPerfil = findViewById(R.id.recyclerViewPerfiles)
        recyclerViewPerfil.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Inicia los adaptadores
        cancionAdapter = CancionAdapter(mutableListOf()) { cancion ->
            val intent = Intent(this, CancionDetail::class.java)
            intent.putExtra("nombre", cancion.nombre)
            intent.putExtra("artista", cancion.nombreArtisticoArtista)
            intent.putExtra("imagen", cancion.fotoPortada)
            intent.putExtra("id", cancion.id)
            startActivity(intent)
        }
        recyclerViewCancion.adapter = cancionAdapter

        artistaAdapter = ArtistaAdapter(mutableListOf())
        recyclerViewArtista.adapter = artistaAdapter

        albumAdapter = AlbumAdapter(mutableListOf())
        recyclerViewAlbum.adapter = albumAdapter

        playlistAdapter = PlaylistAdapter(mutableListOf()) { playlist ->
            val intent = Intent(this, PlaylistDetail::class.java)
            intent.putExtra("nombre", playlist.nombre)
            intent.putExtra("usuario", playlist.nombreUsuarioCreador)
            intent.putExtra("imagen", playlist.fotoPortada)
            intent.putExtra("id", playlist.id)
            Log.d("Playlist", "Buscador -> Playlist")
            startActivity(intent)
        }
        recyclerViewPlaylist.adapter = playlistAdapter

        perfilAdapter = PerfilAdapter(mutableListOf()){perfil ->
            val intent = Intent(this, OtroOyente::class.java)
            intent.putExtra("nombre", perfil.nombreUsuario)
            intent.putExtra("imagen", perfil.fotoPerfil)
            Log.d("Perfil", "Buscador -> Perfil")
            startActivity(intent)
        }
        recyclerViewPerfil.adapter = perfilAdapter

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                val termino = charSequence.toString().trim()
                if (termino.isNotEmpty()) search(termino)
            }
            override fun afterTextChanged(editable: Editable?) {}
        })

        radioOptions = findViewById(R.id.radioGroupFiltros)
        radioTodos = findViewById(R.id.radioTodo)
        radioCanciones = findViewById(R.id.radioCanciones)
        radioArtistas = findViewById(R.id.radioArtistas)
        radioAlbumes = findViewById(R.id.radioAlbumes)
        radioPlaylists = findViewById(R.id.radioPlaylists)
        radioPerfiles = findViewById(R.id.radioPerfiles)

        progressBar = findViewById(R.id.progressBar)
        setupNavigation()
        updateMiniReproductor()

        // Configurar eventos de cambio en el radioGroup para actualizar la vista
        radioOptions.setOnCheckedChangeListener { _, _ ->
            // Llamas a tu función que actualiza la vista
            aplicarFiltros()
        }
    }


    private fun search(termino: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.searchBuscador(authHeader, termino).enqueue(object : Callback<BuscadorResponse> {
            override fun onResponse(call: Call<BuscadorResponse>, response: Response<BuscadorResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            cancionAdapter.updateData(it.canciones)
                            artistaAdapter.updateDataArtista(it.artistas)
                            albumAdapter.updateDataAlbum(it.albumes)
                            playlistAdapter.updateDataPlaylists(it.playlists)
                            perfilAdapter.updateDataPerfiles(it.perfiles)
                            aplicarFiltros()
                        } else {
                            handleErrorCode(it.respuestaHTTP)
                        }
                    } ?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    showToast("Error en la búsqueda: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<BuscadorResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun aplicarFiltros() {
        recyclerViewCancion.visibility = if ((radioTodos.isChecked || radioCanciones.isChecked) && cancionAdapter.itemCount > 0) View.VISIBLE else View.GONE
        recyclerViewArtista.visibility = if ((radioTodos.isChecked || radioArtistas.isChecked) && artistaAdapter.itemCount > 0) View.VISIBLE else View.GONE
        recyclerViewAlbum.visibility = if ((radioTodos.isChecked || radioAlbumes.isChecked) && albumAdapter.itemCount > 0) View.VISIBLE else View.GONE
        recyclerViewPlaylist.visibility = if ((radioTodos.isChecked || radioPlaylists.isChecked) && playlistAdapter.itemCount > 0) View.VISIBLE else View.GONE
        recyclerViewPerfil.visibility = if ((radioTodos.isChecked || radioPerfiles.isChecked) && perfilAdapter.itemCount > 0) View.VISIBLE else View.GONE
        headerCancionesTextView.visibility = if ((radioTodos.isChecked || radioCanciones.isChecked) && cancionAdapter.itemCount > 0) View.VISIBLE else View.GONE
        headerArtistasTextView.visibility = if ((radioTodos.isChecked || radioArtistas.isChecked) && artistaAdapter.itemCount > 0) View.VISIBLE else View.GONE
        headerAlbumesTextView.visibility = if ((radioTodos.isChecked || radioAlbumes.isChecked) && albumAdapter.itemCount > 0) View.VISIBLE else View.GONE
        headerPlaylistsTextView.visibility = if ((radioTodos.isChecked || radioPlaylists.isChecked) && playlistAdapter.itemCount > 0) View.VISIBLE else View.GONE
        headerPerfilesTextView.visibility = if ((radioTodos.isChecked || radioPerfiles.isChecked) && perfilAdapter.itemCount > 0) View.VISIBLE else View.GONE
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
            songImage.setImageResource(R.drawable.ic_default_song)
        } else {
            Glide.with(this)
                .load(songImageUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_default_song)
                .error(R.drawable.ic_default_song)
                .into(songImage)
        }

        songTitle.text = songTitleText
        songArtist.text = songArtistText
        progressBar.progress = songProgress

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