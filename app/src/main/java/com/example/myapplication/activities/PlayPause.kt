package com.example.myapplication.activities

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.ActualizarFavoritoRequest
import com.example.myapplication.io.request.PlayPauseRequest
import com.example.myapplication.io.request.PlayPauseResponse
import com.example.myapplication.io.response.ActualizarFavoritoResponse
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlayPause : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        apiService = ApiService.create()

        val reproduciendo = intent.getBooleanExtra("ESTADO_REPRODUCCION", false)
        val progresoMMSS = intent.getIntExtra("PROGRESO", 0)

        // Convertir MMSS a milisegundos
        val minutos = progresoMMSS / 100
        val segundos = progresoMMSS % 100
        val progresoEnMilisegundos = (minutos * 60 + segundos) * 1000

        // Posicionar la canción en el tiempo guardado
        mediaPlayer?.seekTo(progresoEnMilisegundos)

        // Si estaba reproduciendo, continuar desde ese punto
        if (reproduciendo) {
            mediaPlayer?.start()
        }

        // Enviar actualización a la API
        actualizarEstadoReproduccion(reproduciendo, progresoMMSS)

    }

    private fun actualizarEstadoReproduccion(reproduciendo: Boolean, progreso: Int) {
        val request = PlayPauseRequest(reproduciendo, progreso)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.playPause(authHeader, request).enqueue(object : Callback<PlayPauseResponse> {
            override fun onResponse(call: Call<PlayPauseResponse>, response: Response<PlayPauseResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@PlayPause, "Estado playpause actualizado", Toast.LENGTH_SHORT).show()
                    // Regresar a la pantalla anterior
                    finish()
                } else {
                    //Toast.makeText(this@PlayPause, "Error al actualizar el estado", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PlayPauseResponse>, t: Throwable) {
                //Toast.makeText(this@PlayPause, "Error de conexión", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
