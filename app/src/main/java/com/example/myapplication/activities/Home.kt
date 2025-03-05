package com.example.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import com.example.myapplication.R
import retrofit2.Callback
import retrofit2.Response
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.LoginResponse

class Home : AppCompatActivity() {

    private lateinit var btnLogout: Button
    private lateinit var btnDeleteAccount: Button
    private lateinit var apiService: ApiService
    private var token: String? = null  // El token será almacenado después del login

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        btnLogout = findViewById(R.id.botonLogout)
        btnDeleteAccount = findViewById(R.id.botonDeleteAccount)

        // Obtener el token del login (suponiendo que lo guardas al hacer login)
        token = getTokenFromSharedPrefs()

        apiService = ApiService.create()

        btnLogout.setOnClickListener {
            token?.let { it -> logoutUser(it) }
        }

        btnDeleteAccount.setOnClickListener {
            token?.let { it -> deleteUserAccount(it) }
        }
    }

    private fun logoutUser(token: String) {
        apiService.logout("Bearer $token").enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@Home, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                } else {
                    Toast.makeText(this@Home, "Error al cerrar sesión", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@Home, "Fallo de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteUserAccount(token: String) {
        apiService.deleteAccount("Bearer $token").enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@Home, "Cuenta eliminada", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                } else {
                    Toast.makeText(this@Home, "Error al eliminar cuenta", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@Home, "Fallo de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToLogin() {
        val intent = Intent(this, Inicio::class.java)
        startActivity(intent)
        finish()
    }

    private fun getTokenFromSharedPrefs(): String? {
        // Recupera el token desde SharedPreferences (o el método que uses)
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return sharedPrefs.getString("token", null)
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