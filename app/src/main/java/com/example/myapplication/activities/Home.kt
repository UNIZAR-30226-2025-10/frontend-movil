package com.example.myapplication.activities

import Buscador
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.utils.Preferencias
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.DeleteAccountRequest
import com.example.myapplication.io.response.DeleteAccountResponse
import com.example.myapplication.io.response.LogOutResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.myapplication.io.response.HistorialCancionesResponse
import com.example.myapplication.io.response.HistorialEscuchasResponse
import com.example.myapplication.io.response.PlaylistsResponse
import com.example.myapplication.io.response.RecomendacionesResponse

class Home : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var token: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_bueno)

        // Obtener el token del usuario (desde SharedPreferences o Preferencias)
        token = Preferencias.obtenerValorString("token", "")

        // Inicializar API Service
        apiService = ApiService.create()

        // Cargar datos al iniciar
        loadHomeData()

        // Configurar botones de navegación
        setupNavigation()
    }

    private fun loadHomeData() {
        getHistorialCanciones()
        getHistorialEscuchas()
        getMisPlaylists()
        getRecomendaciones()
    }

    private fun getHistorialCanciones() {
        apiService.getHistorialCanciones("Bearer $token").enqueue(object : Callback<HistorialCancionesResponse> {
            override fun onResponse(call: Call<HistorialCancionesResponse>, response: Response<HistorialCancionesResponse>) {
                if (response.isSuccessful) {
                    val historial = response.body()?.historial_canciones ?: emptyList()
                    Log.d("Home", "Historial de Canciones: $historial")
                    // Aquí podrías actualizar el RecyclerView o UI
                }
            }
            override fun onFailure(call: Call<HistorialCancionesResponse>, t: Throwable) {
                Log.e("Home", "Error cargando historial de canciones", t)
            }
        })
    }

    private fun getHistorialEscuchas() {
        apiService.getHistorialEscuchas("Bearer $token").enqueue(object : Callback<HistorialEscuchasResponse> {
            override fun onResponse(call: Call<HistorialEscuchasResponse>, response: Response<HistorialEscuchasResponse>) {
                if (response.isSuccessful) {
                    val historial = response.body()?.historial_Escuchas ?: emptyList()
                    Log.d("Home", "Historial de Escuchas: $historial")
                }
            }
            override fun onFailure(call: Call<HistorialEscuchasResponse>, t: Throwable) {
                Log.e("Home", "Error cargando historial de Escuchas", t)
            }
        })
    }



    private fun getMisPlaylists() {
        apiService.getMisPlaylists("Bearer $token").enqueue(object : Callback<PlaylistsResponse> {
            override fun onResponse(call: Call<PlaylistsResponse>, response: Response<PlaylistsResponse>) {
                if (response.isSuccessful) {
                    val playlists = response.body()?.playlists ?: emptyList()
                    Log.d("Home", "Mis Playlists: $playlists")
                }
            }
            override fun onFailure(call: Call<PlaylistsResponse>, t: Throwable) {
                Log.e("Home", "Error cargando playlists", t)
            }
        })
    }

    private fun getRecomendaciones() {
        apiService.getRecomendaciones("Bearer $token").enqueue(object : Callback<RecomendacionesResponse> {
            override fun onResponse(call: Call<RecomendacionesResponse>, response: Response<RecomendacionesResponse>) {
                if (response.isSuccessful) {
                    val recomendaciones = response.body()?.canciones_recomendadas ?: emptyList()
                    Log.d("Home", "Recomendaciones: $recomendaciones")
                }
            }
            override fun onFailure(call: Call<RecomendacionesResponse>, t: Throwable) {
                Log.e("Home", "Error cargando recomendaciones", t)
            }
        })
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
}
