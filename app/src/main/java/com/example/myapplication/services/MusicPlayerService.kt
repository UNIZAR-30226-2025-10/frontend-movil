package com.example.myapplication.services

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.myapplication.utils.Preferencias

class MusicPlayerService : Service() {

    private lateinit var mediaPlayer: MediaPlayer
    private val binder = MusicBinder()

    inner class MusicBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MiniReproductor", "onStartCommand recibido, acción: ${intent?.action}")

        when (intent?.action) {
            "PLAY" -> {
                Log.d("MiniReproductor", "Entra en onStartCommand Play")
                val url = intent.getStringExtra("url")
                val progreso = intent.getIntExtra("progreso", -1)
                if (!url.isNullOrEmpty()) {
                    playSong(url, if (progreso >= 0) progreso else null)
                } else {
                    resume()
                }
            }
            "PLAY1" -> {
                Log.d("MiniReproductor", "Entra en onStartCommand Play")
                val url = intent.getStringExtra("url")
                val progreso = intent.getIntExtra("progreso", -1)
                if (!url.isNullOrEmpty()) {
                    playSong1(url, if (progreso >= 0) progreso else null)
                } else {
                    resume()
                }
            }
            "PAUSE" -> {
                Log.d("MiniReproductor", "Entra en onStartCommand pause")
                pause()
            }
        }
        return START_STICKY
    }



    fun playOrPause() {
        if (::mediaPlayer.isInitialized) {
            if (mediaPlayer.isPlaying) {
                pause()
            } else {
                resume()
            }
        }
    }

    fun seekTo(millis: Int) {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.seekTo(millis)
        }
    }

    fun playSong(songUrl: String, seekToMillis: Int? = null) {
        Log.d("MiniReproductor", "Entra en playsong: $songUrl")
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.reset()
        } else {
            mediaPlayer = MediaPlayer()
        }

        try {
            mediaPlayer.setDataSource(songUrl)  // Aquí podría fallar si la URL es incorrecta
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener {
                Log.d("MiniReproductor", "MediaPlayer preparado")
                Log.d("MiniReproductor", "seekToMillis = $seekToMillis")
                if (seekToMillis != null && seekToMillis > 0) {
                    mediaPlayer.seekTo(seekToMillis)
                } else {
                    val savedPosition = Preferencias.obtenerValorEntero("progresoCancionActual", 0)
                    Log.d("MiniReproductor", "Progreso guardado: $savedPosition")
                    if (savedPosition > 0) {
                        mediaPlayer.seekTo(savedPosition)
                    }
                }
                it.start()  // Iniciar la reproducción
                Log.d("MiniReproductor", "Reproducción iniciada")
            }
        } catch (e: Exception) {
            Log.e("MiniReproductor", "Error al configurar MediaPlayer: ${e.message}")
        }
    }

    fun playSong1(songUrl: String, seekToMillis: Int? = null) {
        Log.d("MiniReproductor", "Entra en playsong: $songUrl")
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.reset()
        } else {
            mediaPlayer = MediaPlayer()
        }

        try {
            mediaPlayer.setDataSource(songUrl)  // Aquí podría fallar si la URL es incorrecta
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener {
                Log.d("MiniReproductor", "MediaPlayer preparado")
                if (seekToMillis != null && seekToMillis > 0) {
                    mediaPlayer.seekTo(seekToMillis)
                } else {
                    val savedPosition = Preferencias.obtenerValorEntero("progresoCancionActual", 0)
                    Log.d("MiniReproductor", "Progreso guardado: $savedPosition")
                    if (savedPosition > 0) {
                        mediaPlayer.seekTo(savedPosition)
                    }
                }
                Log.d("MiniReproductor", "Reproducción iniciada")
            }
        } catch (e: Exception) {
            Log.e("MiniReproductor", "Error al configurar MediaPlayer: ${e.message}")
        }
    }

    fun pause() {
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            val prefs = getSharedPreferences("noizz_prefs", MODE_PRIVATE)
            prefs.edit().putInt("progress", mediaPlayer.currentPosition).apply()
        }
    }

    fun resume() {
        if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    fun getProgress(): Int {
        return if (::mediaPlayer.isInitialized) mediaPlayer.currentPosition else 0
    }

    fun getDuration(): Int {
        return if (::mediaPlayer.isInitialized) mediaPlayer.duration else 0
    }

    fun isPlaying(): Boolean {
        return ::mediaPlayer.isInitialized && mediaPlayer.isPlaying
    }

    override fun onDestroy() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        super.onDestroy()
    }
}
