package com.example.myapplication.activities

import android.annotation.SuppressLint
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
import com.example.myapplication.io.response.AudioResponse
import com.example.myapplication.utils.Preferencias
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

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_song)

        apiService = ApiService.create()

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
            startActivity(intent)
        }

        btnPlayPause.setOnClickListener {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    btnPlayPause.setImageResource(R.drawable.ic_play)
                } else {
                    it.start()
                    btnPlayPause.setImageResource(R.drawable.ic_pause)
                }
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

        apiService.reproducirCancion(authHeader, request).enqueue(object : Callback<AudioResponse> {
            override fun onResponse(call: Call<AudioResponse>, response: Response<AudioResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { audioResponse ->
                        reproducirAudio(audioResponse.audio)
                        isFavorito = audioResponse.fav
                        actualizarFavoritoEstado()
                    }
                } else {
                    Toast.makeText(this@CancionDetail, "Error al obtener el audio", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AudioResponse>, t: Throwable) {
                Toast.makeText(this@CancionDetail, "Error de conexión", Toast.LENGTH_SHORT).show()
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
                handler.postDelayed({ actualizarSeekBar() }, 1000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
