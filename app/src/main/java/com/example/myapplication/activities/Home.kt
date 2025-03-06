package com.example.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.utils.Preferencias
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.DeleteRequest
import com.example.myapplication.io.response.DeleteResponse
import com.example.myapplication.io.request.LogOutRequest
import com.example.myapplication.io.response.LogOutResponse
import com.example.myapplication.io.response.LoginResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Home : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var editTextPassword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        // Inicialización de ApiService
        apiService = ApiService.create()

        // Referenciar los botones
        val buttonLogout: Button = findViewById(R.id.botonLogout)
        val buttonDeleteAccount: Button = findViewById(R.id.botonDeleteAccount)
        editTextPassword = findViewById(R.id.contrasenya)

        val usuario = Preferencias.obtenerValorString("nombreUsuario","")
        val correo = Preferencias.obtenerValorString("correo","")
        val contrasenya = editTextPassword.text.toString().trim()

        // Evento clic del botón de logout
        buttonLogout.setOnClickListener {
            logout(usuario, correo, contrasenya)
        }

        // Evento clic del botón de delete account
        buttonDeleteAccount.setOnClickListener {
            borrarCuenta(usuario, correo, contrasenya)
        }
    }

    // Método para hacer logout utilizando la API
    private fun logout(usuario: String, correo: String, contrasenya: String) {
        val logoutRequest: LogOutRequest
        logoutRequest = LogOutRequest(correo = correo, nombreUsuario = usuario, contrasenya = contrasenya)


        // Llamada a la API para hacer logout
        apiService.postlogout(logoutRequest).enqueue(object : Callback<LogOutResponse> {
            override fun onResponse(call: Call<LogOutResponse>, response: Response<LogOutResponse>) {
                if (response.isSuccessful) {
                    val logoutResponse = response.body()
                    if (logoutResponse != null) {
                        Log.d("MiApp", "Respuesta exitosa: ${logoutResponse}")
                        if(logoutResponse.respuestaHTTP == 0){
                            showToast("Logout existoso")
                            navigateL()
                        } else{
                            handleErrorCode(logoutResponse.respuestaHTTP)
                        }
                    } else {
                        showToast("Inicio de sesión fallido: Datos incorrectos")
                    }
                } else {
                    showToast("Error en el logout: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<LogOutResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
                Log.e("MiApp", "Error en la solicitud: ${t.message}")
            }
        })
    }

    // Método para eliminar la cuenta
    private fun borrarCuenta(usuario: String, correo: String, contrasenya: String) {

        val deleteRequest: DeleteRequest
        deleteRequest = DeleteRequest(correo = correo, nombreUsuario = usuario, contrasenya = contrasenya)

        // Llamada a la API para hacer logout
        apiService.deleteAccount(deleteRequest).enqueue(object : Callback<DeleteResponse> {
            override fun onResponse(call: Call<DeleteResponse>, response: Response<DeleteResponse>) {
                if (response.isSuccessful) {
                    val deleteResponse = response.body()
                    if (deleteResponse != null) {
                        Log.d("MiApp", "Respuesta exitosa: ${deleteResponse}")
                        if(deleteResponse.respuestaHTTP == 0){
                            showToast("Logout existoso")
                            navigateL()
                        } else{
                            handleErrorCode(deleteResponse.respuestaHTTP)
                        }
                    } else {
                        showToast("Inicio de sesión fallido: Datos incorrectos")
                    }
                } else {
                    showToast("Error en el logout: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<DeleteResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
                Log.e("MiApp", "Error en la solicitud: ${t.message}")
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

    private fun navigateL() {
        val intent = Intent(this, Inicio::class.java)
        startActivity(intent)
        finish()
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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