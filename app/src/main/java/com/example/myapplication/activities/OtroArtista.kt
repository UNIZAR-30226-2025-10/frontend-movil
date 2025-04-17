package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.Adapters.OtroArtista.CancionesArtistaAdapter
import com.example.myapplication.Adapters.OtroArtista.CancionesPopularesAdapter
import com.example.myapplication.Adapters.OtroArtista.DiscografiaDiscosAdapter
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.ActualizarFavoritoRequest
import com.example.myapplication.io.response.CancionesArtistaResponse
import com.example.myapplication.io.response.DatosArtistaResponse
import com.example.myapplication.io.response.DiscografiaAlbumArtistaResponse
import com.example.myapplication.io.response.NumFavoritasArtistaResponse
import com.example.myapplication.io.response.PopularesArtistaResponse
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OtroArtista : AppCompatActivity() {

    private lateinit var nombreArtistico: TextView
    private lateinit var biografia: TextView
    private lateinit var seguidores: TextView
    private lateinit var fotoPerfil: ImageView
    private lateinit var fotoPerfilFavoritos: ImageButton
    private lateinit var numCanciones: TextView
    private lateinit var artistaLike: TextView
    private lateinit var apiService: ApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAlbumes: RecyclerView
    private lateinit var recyclerViewCanciones: RecyclerView
    private lateinit var cancionesAdapter: CancionesPopularesAdapter
    private lateinit var albumesAdapter: DiscografiaDiscosAdapter
    private lateinit var cancionesArtistaAdapter: CancionesArtistaAdapter

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.otro_artista)

        apiService = ApiService.create()

        // Inicializar las vistas
        nombreArtistico = findViewById(R.id.artisticname)
        biografia = findViewById(R.id.biografia)
        seguidores = findViewById(R.id.followers)
        fotoPerfil = findViewById(R.id.profileImage)
        fotoPerfilFavoritos =  findViewById(R.id.profileImage2)
        recyclerView = findViewById(R.id.recyclerViewPopulares)
        recyclerViewCanciones = findViewById(R.id.recyclerViewCanciones)
        recyclerViewAlbumes = findViewById(R.id.recyclerViewDiscografia)
        numCanciones = findViewById(R.id.numCanciones)
        artistaLike = findViewById(R.id.artistaLike)

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

        // Obtener el nombre de usuario del intent
        val nombreUsuario = intent.getStringExtra("nombreUsuario") ?: ""

        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        cancionesAdapter = CancionesPopularesAdapter { cancion, isFavorite, position  ->
            actualizarFavorito(cancion.id, isFavorite, position, nombreUsuario)
        }
        recyclerView.adapter = cancionesAdapter

        recyclerViewCanciones.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewAlbumes.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        cancionesArtistaAdapter = CancionesArtistaAdapter()
        albumesAdapter = DiscografiaDiscosAdapter()

        recyclerViewCanciones.adapter = cancionesArtistaAdapter
        recyclerViewAlbumes.adapter = albumesAdapter




        val radioGroupDiscografia = findViewById<RadioGroup>(R.id.radioGroupDiscografia)
        radioGroupDiscografia.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.cancionesArtista -> {
                    // Mostrar las canciones y ocultar los álbumes
                    recyclerViewCanciones.visibility = View.VISIBLE
                    recyclerViewAlbumes.visibility = View.GONE
                    obtenerCanciones(nombreUsuario)  // Llamar para obtener canciones
                }
                R.id.discosEPs -> {
                    // Mostrar los álbumes y ocultar las canciones
                    recyclerViewCanciones.visibility = View.GONE
                    recyclerViewAlbumes.visibility = View.VISIBLE
                    obtenerAlbumesDelArtista(nombreUsuario)  // Llamar para obtener álbumes
                }
            }
        }

        // Llamar a la API para obtener los datos del artista
        getDatosArtista(nombreUsuario)
        getCancionesPopulares(nombreUsuario)
        getNumFavoritas(nombreUsuario)
        findViewById<RadioGroup>(R.id.radioGroupDiscografia).check(R.id.cancionesArtista)
        recyclerViewCanciones.visibility = View.VISIBLE
        recyclerViewAlbumes.visibility = View.GONE
        obtenerCanciones(nombreUsuario)

        setupNavigation()
    }

    private fun getDatosArtista(nombreUsuario: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.getDatosArtista(authHeader,nombreUsuario).enqueue(object : Callback<DatosArtistaResponse> {
            override fun onResponse(
                call: Call<DatosArtistaResponse>,
                response: Response<DatosArtistaResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val artista = response.body()?.artista

                    // Mostrar la información en los TextViews
                    nombreArtistico.text = artista?.nombreArtistico
                    artistaLike.text = " De ${artista?.nombreArtistico}"
                    biografia.text = artista?.biografia
                    seguidores.text = "Seguidores: ${artista?.numSeguidores}"

                    // Cargar la imagen usando Glide
                    val foto = artista?.fotoPerfil
                    if (!foto.isNullOrEmpty()) {
                        Glide.with(this@OtroArtista)
                            .load(foto)
                            .placeholder(R.drawable.ic_profile) // Imagen por defecto mientras carga
                            .error(R.drawable.ic_profile) // Imagen por defecto si hay error
                            .circleCrop() // Para que la imagen sea circular
                            .into(fotoPerfil)
                    }
                    if (!foto.isNullOrEmpty()) {
                        Glide.with(this@OtroArtista)
                            .load(foto)
                            .placeholder(R.drawable.ic_profile) // Imagen por defecto mientras carga
                            .error(R.drawable.ic_profile) // Imagen por defecto si hay error
                            .circleCrop() // Para que la imagen sea circular
                            .into(fotoPerfilFavoritos)
                    }
                    artista?.nombreArtistico?.let { nombreArtista ->
                        // Actualizar el nombre artístico en el adapter
                        albumesAdapter.actualizarNombreArtista(nombreArtista)
                        cancionesArtistaAdapter.actualizarNombreArtista(nombreArtista)
                        cancionesAdapter.actualizarNombreArtista(nombreArtista)
                    }
                } else {
                    Toast.makeText(this@OtroArtista, "Error al obtener los datos del artista", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<DatosArtistaResponse>, t: Throwable) {
                Toast.makeText(this@OtroArtista, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun getCancionesPopulares(nombreUsuario: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.getCancionesPopulares(authHeader,nombreUsuario).enqueue(object : Callback<PopularesArtistaResponse> {
            override fun onResponse(call: Call<PopularesArtistaResponse>, response: Response<PopularesArtistaResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val cancionesResponse = response.body()!!
                    val canciones = cancionesResponse.canciones_populares

                    // Pasar las canciones al adaptador
                    cancionesAdapter.submitList(canciones)
                } else {
                    Toast.makeText(this@OtroArtista, "Error al obtener las canciones populares", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PopularesArtistaResponse>, t: Throwable) {
                Toast.makeText(this@OtroArtista, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getNumFavoritas(nombreUsuario: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.getNumeroCancionesFavoritas(authHeader,nombreUsuario).enqueue(object : Callback<NumFavoritasArtistaResponse> {
            override fun onResponse(call: Call<NumFavoritasArtistaResponse>, response: Response<NumFavoritasArtistaResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val totalFavoritas = response.body()!!.total_favoritas
                    if(totalFavoritas == 1){
                        numCanciones.text = "Te gusta ${totalFavoritas} canción"
                    }else{
                        numCanciones.text = "Te gustan ${totalFavoritas} canciones"
                    }
                } else {
                    Toast.makeText(this@OtroArtista, "Error al obtener las num favoritas", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<NumFavoritasArtistaResponse>, t: Throwable) {
                Toast.makeText(this@OtroArtista, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun obtenerAlbumesDelArtista(nombreUsuario: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.albumesArtista(authHeader,nombreUsuario).enqueue(object : Callback<DiscografiaAlbumArtistaResponse> {
            override fun onResponse(call: Call<DiscografiaAlbumArtistaResponse>, response: Response<DiscografiaAlbumArtistaResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    // Obtener los álbumes de la respuesta
                    val albumes = response.body()?.albumes

                    // Verificar que los álbumes no sean nulos
                    if (!albumes.isNullOrEmpty()) {
                        // Pasar los álbumes al adaptador
                        albumesAdapter.submitList(albumes)
                    } else {
                        Toast.makeText(this@OtroArtista, "No se encontraron álbumes para este artista.", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(this@OtroArtista, "Error al obtener albumes", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<DiscografiaAlbumArtistaResponse>, t: Throwable) {
                Toast.makeText(this@OtroArtista, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun obtenerCanciones(nombreUsuario: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.cancionesArtista(authHeader,nombreUsuario).enqueue(object : Callback<CancionesArtistaResponse> {
            override fun onResponse(call: Call<CancionesArtistaResponse>, response: Response<CancionesArtistaResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val canciones = response.body()?.canciones
                    if (canciones != null) {
                        Log.d("GoToArtist", "HAY CANCIONES")
                        cancionesArtistaAdapter.submitList(canciones)
                    }else{
                        Log.d("GoToArtist", "NO HAY CANCIONES")
                    }
                }
            }

            override fun onFailure(call: Call<CancionesArtistaResponse>, t: Throwable) {
                Toast.makeText(this@OtroArtista, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun actualizarFavorito(id: String, fav: Boolean, position: Int, nombreUsuario: String) {
        val request = ActualizarFavoritoRequest(id, fav)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.actualizarFavorito(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (!response.isSuccessful) {
                    cancionesAdapter.updateFavoriteState(position, !fav)
                }
                // Actualizar contador
                getNumFavoritas(nombreUsuario)
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Revertir si hay error
                cancionesAdapter.updateFavoriteState(position, !fav)
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
