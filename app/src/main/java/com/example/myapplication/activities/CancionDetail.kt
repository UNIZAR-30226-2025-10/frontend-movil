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
import android.widget.SeekBar
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CancionDetail : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var seekBar: SeekBar
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var btnFavorito: ImageButton
    private lateinit var btnPlayPause: ImageButton
    private var isFavorito = false
    private var tiempoReproducido = 0
    private var umbralNotificado = false
    private lateinit var webSocketManager: WebSocketManager // Declara la variable

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_song)

        apiService = ApiService.create()
        webSocketManager = WebSocketManager.getInstance() // Inicializa WebSocketManager

        val nombreCancion = intent.getStringExtra("nombre")
        val artistaCancion = intent.getStringExtra("artista")
        val imagenUrl = intent.getStringExtra("imagen")
        val id = intent.getStringExtra("id")

        val textViewNombre = findViewById<TextView>(R.id.textViewNombreCancion)
        val textViewArtista = findViewById<TextView>(R.id.textViewArtista)
        val imageViewCancion = findViewById<ImageView>(R.id.imageViewCancion)
        seekBar = findViewById(R.id.seekBarProgreso)
        btnFavorito = findViewById(R.id.btn_favorito)
        btnPlayPause = findViewById(R.id.btn_PlayPause)

        textViewNombre.text = nombreCancion
        textViewArtista.text = artistaCancion
        Glide.with(this).load(imagenUrl).into(imageViewCancion)

        id?.let {
            Log.d("MiApp", "Reproduciendo canción con ID: $id")
            reproducir(it)
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnFavorito.setOnClickListener {
            isFavorito = !isFavorito
            actualizarFavoritoEstado()

            val intent = Intent(this, ActualizarFavorito::class.java)
            intent.putExtra("cancion_id", id)
            intent.putExtra("es_favorito", isFavorito)

            val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
            startActivity(intent, options.toBundle())
        }

        btnPlayPause.setOnClickListener {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    btnPlayPause.setImageResource(R.drawable.ic_pause)
                } else {
                    it.start()
                }
                val reproduciendo = mediaPlayer?.isPlaying == true
                val progreso = (mediaPlayer?.currentPosition ?: 0) / 1000
                val intent = Intent(this, PlayPause::class.java).apply {
                    putExtra("estado_reproduccion", reproduciendo)
                    putExtra("progreso", progreso)
                }

                val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
                startActivity(intent, options.toBundle())
            }
        }
    }

    private fun actualizarFavoritoEstado() {
        btnFavorito.setImageResource(if (isFavorito) R.drawable.ic_heart_lleno else R.drawable.ic_heart_vacio)
    }

    private fun reproducir(id: String) {
        val request = AudioRequest(id)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        val sid = WebSocketManager.getInstance().getSid() 

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

                        // Mostrar en Toast
                        Toast.makeText(this@CancionDetail, respuestaTexto, Toast.LENGTH_LONG).show()

                        reproducirAudio(audioResponse.audio)
                        isFavorito = audioResponse.fav
                        actualizarFavoritoEstado()
                    }
                } else {
                    val errorMensaje = response.errorBody()?.string() ?: "Error desconocido"

                    // Mostrar en Logcat
                    Log.e("API_RESPONSE", "Error en la respuesta: Código ${response.code()} - $errorMensaje")

                    // Mostrar en Toast
                    Toast.makeText(this@CancionDetail, "Error: $errorMensaje", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<AudioResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)

                // Mostrar en Toast
                Toast.makeText(this@CancionDetail, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun reproducirAudio(audioUrl: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioUrl)
                prepareAsync()
                setOnPreparedListener {
                    start()
                    seekBar.max = duration
                    actualizarSeekBar()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("MiApp", "Error en MediaPlayer: what=$what, extra=$extra")
                    Toast.makeText(this@CancionDetail, "Error al reproducir el audio", Toast.LENGTH_SHORT).show()
                    true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al reproducir el audio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarSeekBar() {
        mediaPlayer?.let { mp ->
            seekBar.progress = mp.currentPosition
            if (mp.isPlaying) {
                tiempoReproducido += 1  // Sumamos un segundo cada vez que se actualiza la barra
                if (tiempoReproducido >= 20 && !umbralNotificado) {
                    notificarReproduccion()
                    umbralNotificado = true
                }
                handler.postDelayed({ actualizarSeekBar() }, 1000)
            }
        }
    }

    private fun notificarReproduccion() {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.addReproduccion(authHeader).enqueue(object : Callback<AddReproduccionResponse> {
            override fun onResponse(call: Call<AddReproduccionResponse>, response: Response<AddReproduccionResponse>) {
                if (response.isSuccessful) {
                    Log.d("MiApp", "Reproducción registrada exitosamente")
                } else {
                    Log.e("MiApp", "Error al registrar la reproducción")
                }
            }

            override fun onFailure(call: Call<AddReproduccionResponse>, t: Throwable) {
                Log.e("MiApp", "Error de conexión al registrar reproducción")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        webSocketManager.closeWebSocket() // Cerrar la conexión WebSocket
    }
}