package com.example.myapplication.activities

import android.annotation.SuppressLint
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
import com.example.myapplication.Adapters.Seguidores.SeguidoresAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.SeguidoresResponse
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Seguidores : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var rvFollowers: RecyclerView
    private lateinit var progressBar: ProgressBar

    private val followersList = mutableListOf<com.example.myapplication.io.response.Seguidores>()
    private lateinit var adapter: SeguidoresAdapter

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.seguidores)

        // Initialize views
        rvFollowers = findViewById(R.id.rvFollowers)
        progressBar = findViewById(R.id.progressBar)

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

        apiService = ApiService.create()
        setupRecyclerView()
        loadFollowing()
        setupNavigation()
    }

    private fun setupRecyclerView() {
        adapter = SeguidoresAdapter(followersList)
        rvFollowers.layoutManager = LinearLayoutManager(this)
        rvFollowers.adapter = adapter
    }

    private fun loadFollowing() {
        progressBar.visibility = View.VISIBLE

        val token = Preferencias.obtenerValorString("token", "") ?: ""
        val authHeader = "Bearer $token"

        apiService.getSeguidores(authHeader).enqueue(object : Callback<SeguidoresResponse> {
            override fun onResponse(call: Call<SeguidoresResponse>, response: Response<SeguidoresResponse>) {
                progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val seguidoresResponse = response.body()!!
                    followersList.clear()
                    followersList.addAll(seguidoresResponse.seguidores)
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@Seguidores, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SeguidoresResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@Seguidores, "Error de conexión", Toast.LENGTH_SHORT).show()
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
                Log.d("Login", "El usuario es un oyente")
                startActivity(Intent(this, Perfil::class.java))
            } else {
                Log.d("Login", "El usuario NO es un oyente")
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