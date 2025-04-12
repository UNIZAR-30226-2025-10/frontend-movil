package com.example.myapplication.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.PlayPauseRequest
import com.example.myapplication.io.request.PlayPauseResponse
import com.example.myapplication.services.MusicPlayerService
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlayPause : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private var musicService: MusicPlayerService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val musicBinder = binder as MusicPlayerService.MusicBinder
            musicService = musicBinder.getService()
            isBound = true

            controlarReproduccion()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            musicService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        apiService = ApiService.create()

        val intent = Intent(this, MusicPlayerService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun controlarReproduccion() {
        val reproduciendo = intent.getBooleanExtra("ESTADO_REPRODUCCION", false)
        val progresoMMSS = intent.getIntExtra("PROGRESO", 0)
        val audioUrl = intent.getStringExtra("AUDIO_URL") ?: ""

        // MMSS a milisegundos
        val minutos = progresoMMSS / 100
        val segundos = progresoMMSS % 100
        val progresoEnMilisegundos = (minutos * 60 + segundos) * 1000

        musicService?.playSong(audioUrl) // siempre recarga por si cambió
        musicService?.seekTo(progresoEnMilisegundos)

        if (!reproduciendo) musicService?.pause()

        actualizarEstadoReproduccion(reproduciendo, progresoMMSS)
    }

    private fun actualizarEstadoReproduccion(reproduciendo: Boolean, progreso: Int) {
        val request = PlayPauseRequest(reproduciendo, progreso)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.playPause(authHeader, request).enqueue(object : Callback<PlayPauseResponse> {
            override fun onResponse(call: Call<PlayPauseResponse>, response: Response<PlayPauseResponse>) {
                Toast.makeText(this@PlayPause,
                    if (response.isSuccessful) "Estado play/pause actualizado"
                    else "Error al actualizar el estado",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }

            override fun onFailure(call: Call<PlayPauseResponse>, t: Throwable) {
                Toast.makeText(this@PlayPause, "Error de conexión", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}
