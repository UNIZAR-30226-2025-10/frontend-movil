package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.Adapters.OtroArtista.CancionesFavoritasAdapter
import com.example.myapplication.Adapters.OtroArtista.CancionesPopularesAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService

import com.example.myapplication.io.response.CancionPopulares
import com.example.myapplication.io.response.CancionesFavsArtistaResponse
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CancionesFavoritasArtista : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CancionesFavoritasAdapter
    private lateinit var apiService: ApiService
    private lateinit var nombreArtista: String
    private lateinit var nombreUsuario: String
    private lateinit var tvTitle: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.canciones_favoritas_artista)

        apiService = ApiService.create()


        recyclerView = findViewById(R.id.cancionesFavs)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = CancionesFavoritasAdapter(emptyList())  // Solo pasas las canciones
        recyclerView.adapter = adapter

        // Obtener datos del Intent
        nombreUsuario = intent.getStringExtra("nombreUsuario") ?: return
        nombreArtista = intent.getStringExtra("nombreArtista") ?: ""

        tvTitle = findViewById(R.id.tvTitle)
        tvTitle.text = " Canciones que te gustan de ${nombreArtista}"


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


        obtenerCancionesFavoritas()

        setupNavigation()
    }

    private fun obtenerCancionesFavoritas() {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.cancionesFavsArtista(authHeader, nombreUsuario).enqueue(object : Callback<CancionesFavsArtistaResponse> {
            override fun onResponse(call: Call<CancionesFavsArtistaResponse>, response: Response<CancionesFavsArtistaResponse>) {
                val canciones = response.body()?.canciones_favoritas
                if (canciones != null) {
                    Log.d("GoToArtist", "HAY CANCIONES")
                    adapter.submitList(canciones)
                }else{
                    Log.d("GoToArtist", "NO HAY CANCIONES")
                }
            }

            override fun onFailure(call: Call<CancionesFavsArtistaResponse>, t: Throwable) {
                Toast.makeText(this@CancionesFavoritasArtista, "Error de red", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun setupNavigation() {
        val buttonPerfil: ImageButton = findViewById(R.id.profileImageButton)
        val buttonNotis: ImageButton = findViewById(R.id.notificationImageButton)
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

        buttonNotis.setOnClickListener {
            startActivity(Intent(this, Notificaciones::class.java))
        }

        buttonHome.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }

        buttonSearch.setOnClickListener {
            startActivity(Intent(this, Buscador::class.java))
        }

        buttonCrear.setOnClickListener {
            startActivity(Intent(this, CrearPlaylist::class.java))
        }
    }
}
