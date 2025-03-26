package com.example.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.Adapters.Home.HeaderAdapter
import com.example.myapplication.Adapters.Home.PlaylistsAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.PlaylistsResponse
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.myapplication.Adapters.Home.RecomendacionesAdapter
import com.example.myapplication.io.response.InfoSeguidoresResponse

class Perfil : AppCompatActivity()  {

    private lateinit var apiService: ApiService
    private lateinit var recyclerViewPlaylists: RecyclerView
    private lateinit var headerPlaylistsRecyclerView: RecyclerView
    private lateinit var playlistsAdapter: PlaylistsAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.perfil)

        // Inicializar API Service
        apiService = ApiService.create()

        val headersPlaylists = listOf("Mis playlists")
        val headerPlaylistsAdapter = HeaderAdapter(headersPlaylists)
        headerPlaylistsRecyclerView = findViewById(R.id.recyclerViewHeadersPlaylistsP)
        headerPlaylistsRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        headerPlaylistsRecyclerView.adapter = headerPlaylistsAdapter
        headerPlaylistsRecyclerView.visibility = View.INVISIBLE

        recyclerViewPlaylists = findViewById(R.id.recyclerViewPlaylistsP)
        recyclerViewPlaylists.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        playlistsAdapter = PlaylistsAdapter(mutableListOf())
        recyclerViewPlaylists.adapter = playlistsAdapter


        // Botones
        val deleteAccountButton = findViewById<Button>(R.id.botonDeleteAccount)
        val logOutButton = findViewById<Button>(R.id.botonLogout)

        // Cargar datos al iniciar
        loadProfileData()

        deleteAccountButton.setOnClickListener {
            //Borrar cuenta
            startActivity(Intent(this, DeleteAccount::class.java))
        }

        logOutButton.setOnClickListener {
            //Cerrar sesion
            startActivity(Intent(this, Logout::class.java))
        }

    }

    private fun loadProfileData() {
        getInfo()
        getMisPlaylists()
    }

    private fun getInfo() {
        val token = Preferencias.obtenerValorString("token", "")
        apiService.getMisDatosOyente("Bearer $token").enqueue(object : Callback<InfoSeguidoresResponse> {
            override fun onResponse(call: Call<InfoSeguidoresResponse>, response: Response<InfoSeguidoresResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            val nombreperfil = it.nombreUsuario
                            val seguidores = it.numSeguidores
                            val seguidos = it.numSeguidos

                            Log.d("Mi app", "ha tomado info: $nombreperfil")
                            Log.d("Mi app", "ha tomado info: $seguidores")
                            Log.d("Mi app", "ha tomado info: $seguidos")

                            val usernameTextView = findViewById<TextView>(R.id.username)
                            val followersTextView = findViewById<TextView>(R.id.followers)
                            val followingTextView = findViewById<TextView>(R.id.following)

                            usernameTextView.text = nombreperfil
                            followersTextView.text = "$seguidores Seguidores"
                            followingTextView.text = "$seguidos Seguidos"

                        } else {
                            handleErrorCode(it.respuestaHTTP)
                        }
                    } ?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    showToast("Error en la búsqueda: Código ${response.code()}")
                }
            }
            override fun onFailure(call: Call<InfoSeguidoresResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun getMisPlaylists() {
        val token = Preferencias.obtenerValorString("token", "")
        apiService.getMisPlaylists("Bearer $token").enqueue(object : Callback<PlaylistsResponse> {
            override fun onResponse(call: Call<PlaylistsResponse>, response: Response<PlaylistsResponse>) {
                Log.d("Mi app", "entra en on response Playlists")
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            val misPlaylists = it.playlists

                            // Actualizar y mostrar las canciones si las hay
                            if (misPlaylists.isNotEmpty()) {
                                playlistsAdapter.updateDataMisPlaylists(misPlaylists)
                                recyclerViewPlaylists.visibility = View.VISIBLE
                                headerPlaylistsRecyclerView.visibility = View.VISIBLE
                            } else {
                                recyclerViewPlaylists.visibility = View.GONE
                                headerPlaylistsRecyclerView.visibility = View.GONE
                                showToast("No hay playlists")
                            }

                        } else {
                            handleErrorCode(it.respuestaHTTP)
                        }
                    } ?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    showToast("Error en la búsqueda: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<PlaylistsResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
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