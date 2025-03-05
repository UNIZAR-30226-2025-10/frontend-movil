package com.example.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.utils.Preferencias
import com.example.myapplication.io.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Home : AppCompatActivity() {

    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        // Inicialización de ApiService
        apiService = ApiService.create()

        // Referenciar los botones
        val buttonLogout: Button = findViewById(R.id.botonLogout)
        val buttonDeleteAccount: Button = findViewById(R.id.botonDeleteAccount)

        // Evento clic del botón de logout
        buttonLogout.setOnClickListener {
            logout()
        }

        // Evento clic del botón de delete account
        buttonDeleteAccount.setOnClickListener {
            deleteAccount()
        }
    }

    // Método para hacer logout utilizando la API
    private fun logout() {
        val token = Preferencias.obtenerValorString("token", "")

        if (token.isNotEmpty()) {
            // Llamada a la API para hacer logout
            apiService.logout("Bearer $token").enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        // Borrar los datos guardados
                        Preferencias.guardarValorString("token", "")
                        Preferencias.guardarValorString("correo", "")
                        Preferencias.guardarValorString("fotoPerfil", "")
                        Preferencias.guardarValorString("nombreUsuario", "")
                        Preferencias.guardarValorString("esOyente", "")
                        Preferencias.guardarValorEntero("volumen", 0)

                        // Mostrar mensaje y redirigir a login
                        Toast.makeText(this@Home, "Has cerrado sesión correctamente", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@Home, Login::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@Home, "Error al cerrar sesión", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(this@Home, "Error al hacer logout: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("MiApp", "Error al hacer logout: ${t.message}")
                }
            })
        } else {
            Toast.makeText(this, "No se ha encontrado un token válido", Toast.LENGTH_SHORT).show()
        }
    }

    // Método para eliminar la cuenta
    private fun deleteAccount() {
        val token = Preferencias.obtenerValorString("token", "")

        if (token.isNotEmpty()) {
            apiService.deleteAccount("Bearer $token").enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        // Borrar los datos del usuario después de eliminar la cuenta
                        Preferencias.guardarValorString("token", "")
                        Preferencias.guardarValorString("correo", "")
                        Preferencias.guardarValorString("fotoPerfil", "")
                        Preferencias.guardarValorString("nombreUsuario", "")
                        Preferencias.guardarValorString("esOyente", "")
                        Preferencias.guardarValorEntero("volumen", 0)

                        Toast.makeText(this@Home, "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show()

                        // Redirigir a la pantalla de login
                        val intent = Intent(this@Home, Login::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@Home, "Error al eliminar la cuenta", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(this@Home, "Error en la solicitud: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("MiApp", "Error al eliminar cuenta: ${t.message}")
                }
            })
        } else {
            Toast.makeText(this, "No se ha encontrado un token válido", Toast.LENGTH_SHORT).show()
        }
    }
}


/*private lateinit var recentlyListenedAdapter: SongAdapter
   private lateinit var playlistsAdapter: PlaylistAdapter
   private lateinit var latestSongsAdapter: SongAdapter
   private lateinit var recommendationsAdapter: SongAdapter

   private lateinit var recyclerViewRecentlyListened: RecyclerView
   private lateinit var recyclerViewPlaylists: RecyclerView
   private lateinit var recyclerViewLatestSongs: RecyclerView
   private lateinit var recyclerViewRecommendations: RecyclerView

   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       setContentView(R.layout.activity_home)

       // Inicializamos los RecyclerViews
       recyclerViewRecentlyListened = findViewById(R.id.recentlyListened)
       recyclerViewPlaylists = findViewById(R.id.playlistsRecyclerView)
       recyclerViewLatestSongs = findViewById(R.id.latestSongsRecyclerView)
       recyclerViewRecommendations = findViewById(R.id.recommendationsRecyclerView)

       // Configuramos los RecyclerViews
       recyclerViewRecentlyListened.layoutManager = LinearLayoutManager(this)
       recyclerViewPlaylists.layoutManager = LinearLayoutManager(this)
       recyclerViewLatestSongs.layoutManager = LinearLayoutManager(this)
       recyclerViewRecommendations.layoutManager = LinearLayoutManager(this)

       // Cargar datos simulados
       loadSimulatedData()
   }

   private fun loadSimulatedData() {
       // Simulamos los datos de cada sección
       val recentSongs = listOf(
           Song("Song 1", "Artist A", "3:15"),
           Song("Song 2", "Artist B", "4:00")
       )

       val playlists = listOf(
           Playlist("My Playlist", 10),
           Playlist("Top Hits", 20)
       )

       val latestSongs = listOf(
           Song("Song 3", "Artist C", "2:45"),
           Song("Song 4", "Artist D", "3:30")
       )

       val recommendations = listOf(
           Song("Song 5", "Artist E", "3:10"),
           Song("Song 6", "Artist F", "4:20")
       )

       // Asignamos los datos a los adaptadores
       recentlyListenedAdapter = SongAdapter(recentSongs)
       playlistsAdapter = PlaylistAdapter(playlists)
       latestSongsAdapter = SongAdapter(latestSongs)
       recommendationsAdapter = SongAdapter(recommendations)

       // Establecemos los adaptadores a los RecyclerViews
       recyclerViewRecentlyListened.adapter = recentlyListenedAdapter
       recyclerViewPlaylists.adapter = playlistsAdapter
       recyclerViewLatestSongs.adapter = latestSongsAdapter
       recyclerViewRecommendations.adapter = recommendationsAdapter
   }*/