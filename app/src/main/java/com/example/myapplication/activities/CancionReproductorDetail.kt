package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.AudioRequest
import com.example.myapplication.io.response.AddReproduccionResponse
import com.example.myapplication.io.response.AudioResponse
import com.example.myapplication.utils.Preferencias
import WebSocketManager
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.view.MotionEvent
import android.widget.ProgressBar
import com.example.myapplication.io.request.ActualizarFavoritoRequest
import com.example.myapplication.io.request.PlayPauseRequest
import com.example.myapplication.io.request.PlayPauseResponse
import com.example.myapplication.io.response.ActualizarFavoritoResponse
import com.example.myapplication.services.MusicPlayerService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CancionReproductorDetail : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var btnFavorito: ImageButton
    private lateinit var btnPlayPause: ImageButton
    private var isFavorito = false
    private lateinit var webSocketManager: WebSocketManager // Declara la variable

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
        setContentView(R.layout.fragment_song)

        apiService = ApiService.create()
        webSocketManager = WebSocketManager.getInstance() // Inicializa WebSocketManager

        /*
        val nombreCancion = intent.getStringExtra("nombre")
        val artistaCancion = intent.getStringExtra("artista")
        val imagenUrl = intent.getStringExtra("imagen")
        val id = intent.getStringExtra("id")
        */

        val id = Preferencias.obtenerValorString("cancionActualId", "")
        val imagenUrl = Preferencias.obtenerValorString("fotoPortadaActual", "")
        val nombreCancion = Preferencias.obtenerValorString("nombreCancionActual", "Nombre de la canción")
        val artistaCancion = Preferencias.obtenerValorString("nombreArtisticoActual", "Artista")
        val songProgress = Preferencias.obtenerValorEntero("progresoCancionActual", 0)

        val textViewNombre = findViewById<TextView>(R.id.textViewNombreCancion)
        val textViewArtista = findViewById<TextView>(R.id.textViewArtista)
        val imageViewCancion = findViewById<ImageView>(R.id.imageViewCancion)
        progressBar = findViewById(R.id.seekBarProgreso)
        btnFavorito = findViewById(R.id.btn_favorito)
        btnPlayPause = findViewById(R.id.btn_PlayPause)

        textViewNombre.text = nombreCancion
        textViewArtista.text = artistaCancion
        Glide.with(this).load(imagenUrl).into(imageViewCancion)

        mirarfav(id)
        /*
        id?.let {
            Log.d("MiApp", "Reproduciendo canción con ID: $id")
            reproducir(it)
        }
        */

        progressBar.progress = songProgress/1749

        btnFavorito.setOnClickListener {
            isFavorito = !isFavorito
            actualizarFavoritoEstado()
            if (id != null) {
                actualizarFavorito(id, isFavorito)
            }
        }

        btnPlayPause.setOnClickListener {
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
                    btnPlayPause.setImageResource(R.drawable.ic_pause)
                    Log.d("MiniReproductor", "Canción pausada en $progreso ms")
                    actualizarEstadoReproduccion(false, progreso) // Enviar estado 'pausado' con el progreso
                } else {
                    Log.d("MiniReproductor", "Intentando reanudar la canción...")
                    service.resume()
                    btnPlayPause.setImageResource(R.drawable.ic_play)
                    Log.d("MiniReproductor", "Canción reanudada")
                    val progreso = service.getProgress()
                    actualizarEstadoReproduccion(true, progreso) // Enviar estado 'pausado' con el progreso
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


    private fun actualizarFavoritoEstado() {
        btnFavorito.setImageResource(if (isFavorito) R.drawable.ic_heart_lleno else R.drawable.ic_heart_vacio)
    }

    private fun actualizarFavorito(id: String, fav: Boolean) {
        val request = ActualizarFavoritoRequest(id, fav)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        // Llamar al servicio API para actualizar el estado del favorito
        apiService.actualizarFavorito(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@CancionReproductorDetail, "Estado de favorito actualizado", Toast.LENGTH_SHORT).show()
                    // Regresar a la pantalla anterior
                    val resultIntent = Intent()
                    resultIntent.putExtra("es_favorito", fav) // Devolver el nuevo estado del favorito
                    setResult(RESULT_OK, resultIntent)
                    finish()
                } else {
                    Toast.makeText(this@CancionReproductorDetail, "Error al actualizar el estado", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@CancionReproductorDetail, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun mirarfav(id: String) {
        val request = AudioRequest(id)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        val sid = WebSocketManager.getInstance().getSid()
        Log.d("WebSocket", "El SID actual es: $sid")

        if (sid == null) {
            Log.e("MiApp", "No se ha generado un sid para el WebSocket")
            return
        }

        // Llamada a la API con el sid en los headers
        apiService.reproducirCancion(authHeader, sid, request).enqueue(object : Callback<AudioResponse> {
            override fun onResponse(call: Call<AudioResponse>, response: Response<AudioResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { audioResponse ->
                        val respuestaTexto = "Audio: ${audioResponse.audio}, Favorito: ${audioResponse.fav}"

                        // Mostrar en Logcat
                        Log.d("API_RESPONSE", "Respuesta exitosa: $respuestaTexto")
                        isFavorito = audioResponse.fav
                        actualizarFavoritoEstado()
                    }
                } else {
                    val errorMensaje = response.errorBody()?.string() ?: "Error desconocido"

                    // Mostrar en Logcat
                    Log.e("API_RESPONSE", "Error en la respuesta: Código ${response.code()} - $errorMensaje")

                }
            }

            override fun onFailure(call: Call<AudioResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)
            }
        })
    }

    private fun actualizarIconoPlayPause() {
        if (serviceBound && musicService != null) {
            val estaReproduciendo = musicService!!.isPlaying()
            val icono = if (estaReproduciendo) R.drawable.ic_play else R.drawable.ic_pause
            val stopButton = findViewById<ImageButton>(R.id.btn_PlayPause)
            stopButton.setImageResource(icono)
        }
    }

    private fun actualizarEstadoReproduccion(reproduciendo: Boolean, progreso: Int) {
        val request = PlayPauseRequest(reproduciendo, progreso)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.playPause(authHeader, request).enqueue(object : Callback<PlayPauseResponse> {
            override fun onResponse(call: Call<PlayPauseResponse>, response: Response<PlayPauseResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@CancionReproductorDetail, "Estado playpause actualizado", Toast.LENGTH_SHORT).show()
                    // Regresar a la pantalla anterior
                    finish()
                } else {
                    Toast.makeText(this@CancionReproductorDetail, "Error al actualizar el estado", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PlayPauseResponse>, t: Throwable) {
                Toast.makeText(this@CancionReproductorDetail, "Error de conexión", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
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

    override fun onDestroy() {
        super.onDestroy()
        webSocketManager.closeWebSocket() // Cerrar la conexión WebSocket
    }

}