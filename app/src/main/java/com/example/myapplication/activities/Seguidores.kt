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
import com.example.myapplication.io.request.ChangeFollowRequest
import com.example.myapplication.io.response.Seguidores
import com.example.myapplication.io.response.SeguidoresResponse
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Seguidores : AppCompatActivity(), SeguidoresAdapter.OnFollowListener {

    private lateinit var apiService: ApiService
    private lateinit var rvFollowers: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: SeguidoresAdapter
    private val followersList = mutableListOf<Seguidores>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.seguidores)

        rvFollowers = findViewById(R.id.rvFollowers)
        progressBar = findViewById(R.id.progressBar)
        val profileImageButton = findViewById<ImageButton>(R.id.profileImageButton)

        val profileImageUrl = Preferencias.obtenerValorString("fotoPerfil", "")
        Log.d("ProfileImage", "URL de la imagen de perfil: $profileImageUrl")

        if (profileImageUrl.isNullOrEmpty() || profileImageUrl == "DEFAULT") {
            profileImageButton.setImageResource(R.drawable.ic_profile)
        } else {
            Glide.with(this)
                .load(profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(profileImageButton)
        }

        apiService = ApiService.create()
        setupRecyclerView()
        loadFollowers()
        setupNavigation()
    }

    private fun setupRecyclerView() {
        adapter = SeguidoresAdapter(followersList, this)
        rvFollowers.layoutManager = LinearLayoutManager(this)
        rvFollowers.adapter = adapter
    }

    private fun loadFollowers() {
        progressBar.visibility = View.VISIBLE
        val token = Preferencias.obtenerValorString("token", "") ?: ""
        val authHeader = "Bearer $token"

        apiService.getSeguidores(authHeader).enqueue(object : Callback<SeguidoresResponse> {
            override fun onResponse(call: Call<SeguidoresResponse>, response: Response<SeguidoresResponse>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    followersList.clear()
                    followersList.addAll(response.body()!!.seguidores)
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

    override fun onFollowStatusChanged(userId: String, isFollowing: Boolean, position: Int) {
        val token = Preferencias.obtenerValorString("token", "") ?: ""
        val authHeader = "Bearer $token"
        val request = ChangeFollowRequest(isFollowing, userId)

        apiService.changeFollow(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    val message = if (isFollowing) "Ahora sigues a este usuario" else "Dejaste de seguir al usuario"
                    Toast.makeText(this@Seguidores, message, Toast.LENGTH_SHORT).show()
                } else {
                    // Revertir el cambio si falla la API
                    adapter.updateItem(position, !isFollowing)
                    Toast.makeText(this@Seguidores, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Revertir el cambio si falla la conexión
                adapter.updateItem(position, !isFollowing)
                Log.e("API Error", "Error en change-follow", t)
                Toast.makeText(this@Seguidores, "Fallo de conexión", Toast.LENGTH_SHORT).show()
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
            val intent = if (esOyente == "oyente") {
                Intent(this, Perfil::class.java)
            } else {
                Intent(this, PerfilArtista::class.java)
            }
            startActivity(intent)
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