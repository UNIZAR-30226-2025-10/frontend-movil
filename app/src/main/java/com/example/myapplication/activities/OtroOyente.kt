package com.example.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import com.bumptech.glide.Glide
import com.example.myapplication.Adapters.Buscador.PerfilAdapter
import com.example.myapplication.Adapters.OtroOyente.PlaylistOtroOyenteAdapter
import com.example.myapplication.Adapters.Seguidores.SeguidoresAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.ChangeFollowRequest
import com.example.myapplication.io.response.CancionesArtistaResponse
import com.example.myapplication.io.response.GetDatosOyenteResponse
import com.example.myapplication.io.response.GetPlaylistOyenteResponse
import com.example.myapplication.io.response.InfoSeguidoresResponse
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.InvitacionPlaylist
import com.example.myapplication.io.response.Novedad
import com.example.myapplication.io.response.Seguidor
import com.example.myapplication.services.WebSocketEventHandler
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OtroOyente : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var btnFollow: Button
    private lateinit var usernameText: TextView
    private lateinit var profileImage: ImageView
    private lateinit var fotoPerfil: ImageView
    private lateinit var cvLastNoizzy: CardView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlaylistOtroOyenteAdapter
    private lateinit var dot: View

    private var isFollowing = false

    //EVENTOS PARA LAS NOTIFICACIONES
    private val listenerNovedad: (Novedad) -> Unit = {
        runOnUiThread {
            Log.d("LOGS_NOTIS", "evento en home")
            dot.visibility = View.VISIBLE
        }
    }
    private val listenerSeguidor: (Seguidor) -> Unit = {
        runOnUiThread {
            Log.d("LOGS_NOTIS", "evento en home")
            dot.visibility = View.VISIBLE
        }
    }
    private val listenerInvitacion: (InvitacionPlaylist) -> Unit = {
        runOnUiThread {
            Log.d("LOGS_NOTIS", "evento en home")
            dot.visibility = View.VISIBLE
        }
    }
    private val listenerInteraccion: (Interaccion) -> Unit = {
        runOnUiThread {
            Log.d("LOGS_NOTIS", "evento en home")
            dot.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.perfil_otro_oyente)

        apiService = ApiService.create()

        val nombreUser = intent.getStringExtra("nombre")
        val imagenUrl = intent.getStringExtra("imagen")

        // Inicialización de vistas
        btnFollow = findViewById(R.id.btnFollow)
        usernameText = findViewById(R.id.username)
        cvLastNoizzy = findViewById(R.id.lastMessage)
        profileImage = findViewById(R.id.profileImage)
        recyclerView = findViewById(R.id.recyclerViewHeadersPlaylistsP)
        dot = findViewById<View>(R.id.notificationDot)

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

        //PARA EL CIRCULITO ROJO DE NOTIFICACIONES
        if (Preferencias.obtenerValorBooleano("hay_notificaciones",false) == true) {
            dot.visibility = View.VISIBLE
        } else {
            dot.visibility = View.GONE
        }

        //Para actualizar el punto rojo en tiempo real, suscripcion a los eventos
        WebSocketEventHandler.registrarListenerNovedad(listenerNovedad)
        WebSocketEventHandler.registrarListenerSeguidor(listenerSeguidor)
        WebSocketEventHandler.registrarListenerInvitacion(listenerInvitacion)
        WebSocketEventHandler.registrarListenerInteraccion(listenerInteraccion)

        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapter = PlaylistOtroOyenteAdapter(mutableListOf()){ playlist ->
            val intent = Intent(this, PlaylistDetail::class.java)
            intent.putExtra("nombre", playlist.nombre)
            intent.putExtra("imagen", playlist.fotoPortada)
            intent.putExtra("id", playlist.id)
            Log.d("MiAppPlaylist", "id mando ${playlist.id}")
            Log.d("Playlist", "Buscador -> Playlist")
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Evento de clic para el botón de seguir
        btnFollow.setOnClickListener {
            // Cambiar el estado de seguir/no seguir
            isFollowing = !isFollowing
            updateFollowButtonState()
            // Aquí puedes añadir la lógica para realizar una acción, como seguir al oyente en la base de datos
            if (isFollowing) {
                if (nombreUser != null) {
                    onFollowStatusChanged(nombreUser,true)
                }
            } else {
                if (nombreUser != null) {
                    onFollowStatusChanged(nombreUser,false)
                }
            }
        }

        if (nombreUser != null) {
            getDatosOyente(nombreUser)
            getPlaylistOyente(nombreUser)
        }

        setupNavigation()
    }

    private fun getDatosOyente(nombreUser: String) {
        Log.d("OtroOyente", "1")
        val token = Preferencias.obtenerValorString("token", "")
        apiService.getDatosOyente("Bearer $token", nombreUser).enqueue(object : Callback<GetDatosOyenteResponse> {
            override fun onResponse(call: Call<GetDatosOyenteResponse>, response: Response<GetDatosOyenteResponse>) {
                Log.d("OtroOyente", "1")
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            val nombreperfil = it.oyente.nombreUsuario
                            val seguidores = it.oyente.numSeguidores
                            val seguidos = it.oyente.numSeguidos

                            var foto: Any
                            if (it.oyente.fotoPerfil == "DEFAULT" || it.oyente.fotoPerfil.isNullOrEmpty()) {
                                foto = R.drawable.ic_profile
                            } else {
                                foto = it.oyente.fotoPerfil
                            }

                            val siguiendo = it.oyente.siguiendo
                            val lastNoizzy = it.ultimoNoizzy

                            Log.d("OtroOyente", "ha tomado info: $nombreperfil")
                            Log.d("OtroOyente", "ha tomado info: $seguidores")
                            Log.d("OtroOyente", "ha tomado info: $seguidos")

                            val usernameTextView = findViewById<TextView>(R.id.username)
                            val followersTextView = findViewById<TextView>(R.id.followers)
                            val followingTextView = findViewById<TextView>(R.id.following)
                            val fotoImageView = findViewById<ImageView>(R.id.profileImage)

                            isFollowing = siguiendo
                            updateFollowButtonState()

                            usernameTextView.text = nombreperfil
                            followersTextView.text = "$seguidores Seguidores"
                            followingTextView.text = "$seguidos Seguidos"

                            // Cargar la imagen con Glide
                            Glide.with(this@OtroOyente)
                                .load(foto)
                                .placeholder(R.drawable.ic_profile) // Imagen por defecto mientras carga
                                .error(R.drawable.ic_profile) // Imagen por defecto si hay error
                                .circleCrop() // Para que la imagen sea circular
                                .into(fotoImageView)

                        } else {
                            handleErrorCode(it.respuestaHTTP)
                        }
                    } ?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    showToast("Error en la búsqueda: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<GetDatosOyenteResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }
    fun onFollowStatusChanged(userId: String, isFollowing: Boolean) {
        val token = Preferencias.obtenerValorString("token", "") ?: ""
        val authHeader = "Bearer $token"
        val request = ChangeFollowRequest(isFollowing, userId)

        apiService.changeFollow(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    val message = if (isFollowing) "Ahora sigues a este usuario" else "Dejaste de seguir al usuario"
                    Toast.makeText(this@OtroOyente, message, Toast.LENGTH_SHORT).show()
                } else {
                    // Revertir el cambio si falla la API
                    Toast.makeText(this@OtroOyente, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Revertir el cambio si falla la conexión
                Log.e("API Error", "Error en change-follow", t)
                Toast.makeText(this@OtroOyente, "Fallo de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getPlaylistOyente(nombreUsuario: String) {
        Log.d("otroOyente1", "ENTRA PLAYLIST")
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.getPlaylistOyente(authHeader,nombreUsuario).enqueue(object : Callback<GetPlaylistOyenteResponse> {
            override fun onResponse(call: Call<GetPlaylistOyenteResponse>, response: Response<GetPlaylistOyenteResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val num = response.body()?.n_playlists
                    val playlist = response.body()?.playlists
                    val publicPlaylists = findViewById<TextView>(R.id.publicPlaylists)
                    publicPlaylists.text = "$num playlist públicas"
                    Log.d("otroOyente1", "HAY ${num}")
                    if (playlist != null) {
                        Log.d("otroOyente1", "HAY playlist")
                        adapter.submitList(playlist)

                    }else{
                        Log.d("otroOyente1", "NO HAY playlist")
                    }
                }else{
                    Log.d("otroOyente1", "NO success")
                }
            }

            override fun onFailure(call: Call<GetPlaylistOyenteResponse>, t: Throwable) {
                Log.d("otroOyente", "error conexion")
            }
        })
    }


    // Método para actualizar el estado del botón de seguir
    private fun updateFollowButtonState() {
        if (isFollowing) {
            btnFollow.text = "Dejar de seguir"
        } else {
            btnFollow.text = "Seguir"
        }
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

    override fun onDestroy() {
        super.onDestroy()
        WebSocketEventHandler.eliminarListenerNovedad(listenerNovedad)
        WebSocketEventHandler.eliminarListenerSeguidor(listenerSeguidor)
        WebSocketEventHandler.eliminarListenerInvitacion(listenerInvitacion)
        WebSocketEventHandler.eliminarListenerInteraccion(listenerInteraccion)
    }

}