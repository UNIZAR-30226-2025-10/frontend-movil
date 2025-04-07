package com.example.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.Adapters.Seguidos.SeguidosAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.ChangeFollowRequest
import com.example.myapplication.io.response.Seguidos
import com.example.myapplication.io.response.SeguidosResponse
import com.example.myapplication.utils.Preferencias
import com.google.gson.JsonParser
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Seguidos : AppCompatActivity(), SeguidosAdapter.OnUnfollowListener {

    private lateinit var apiService: ApiService
    private lateinit var rvFollowing: RecyclerView
    private lateinit var progressBar: ProgressBar
    private val followingList = mutableListOf<Seguidos>()
    private lateinit var adapter: SeguidosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.seguidos)

        // Initialize views
        rvFollowing = findViewById(R.id.rvFollowing)
        progressBar = findViewById(R.id.progressBar)

        val profileImageButton = findViewById<ImageButton>(R.id.profileImageButton)
        val profileImageUrl = Preferencias.obtenerValorString("fotoPerfil", "")

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

        apiService = ApiService.create()
        setupRecyclerView()
        loadFollowing()
        setupNavigation()
    }

    private fun setupRecyclerView() {
        adapter = SeguidosAdapter(followingList, this) // Pasamos this como listener
        rvFollowing.layoutManager = LinearLayoutManager(this)
        rvFollowing.adapter = adapter
    }

    private fun loadFollowing() {
        progressBar.visibility = View.VISIBLE

        val token = Preferencias.obtenerValorString("token", "") ?: ""
        val authHeader = "Bearer $token"

        apiService.getSeguidos(authHeader).enqueue(object : Callback<SeguidosResponse> {
            override fun onResponse(call: Call<SeguidosResponse>, response: Response<SeguidosResponse>) {
                progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val seguidosResponse = response.body()!!
                    followingList.clear()
                    followingList.addAll(seguidosResponse.seguidos)
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@Seguidos, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SeguidosResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@Seguidos, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onUnfollow(userId: String, position: Int) {
        val token = Preferencias.obtenerValorString("token", "") ?: ""
        val authHeader = "Bearer $token"
        val request = ChangeFollowRequest(false, userId)


        apiService.changeFollow(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                when {
                    response.isSuccessful -> {
                        followingList.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        Toast.makeText(this@Seguidos, "Dejaste de seguir a ", Toast.LENGTH_SHORT).show()
                    }
                    response.code() == 404 -> {
                        Toast.makeText(this@Seguidos, "El usuario no existe", Toast.LENGTH_LONG).show()
                    }
                    response.code() == 409 -> {
                        Toast.makeText(this@Seguidos, "No sigues a este usuario", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        val error = try {

                        } catch (e: Exception) {
                            "Error ${response.code()}"
                        }
                        Toast.makeText(this@Seguidos, "Error: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("API Error", "Error en change-follow", t)
            }
        })
    }

    private fun setupNavigation() {
        val buttonPerfil: ImageButton = findViewById(R.id.profileImageButton)
        val buttonHome: ImageButton = findViewById(R.id.nav_home)
        val buttonSearch: ImageButton = findViewById(R.id.nav_search)
        val buttonCrear: ImageButton = findViewById(R.id.nav_create)

        buttonPerfil.setOnClickListener {
            val esOyente = Preferencias.obtenerValorString("esOyente", "")
            if (esOyente == "oyente") {
                startActivity(Intent(this, Perfil::class.java))
            } else {
                startActivity(Intent(this, PerfilArtista::class.java))
            }
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