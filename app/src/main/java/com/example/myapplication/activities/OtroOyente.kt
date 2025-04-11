package com.example.myapplication.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.myapplication.Adapters.Buscador.PerfilAdapter
import com.example.myapplication.Adapters.Seguidores.SeguidoresAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.ChangeFollowRequest
import com.example.myapplication.io.response.GetDatosOyenteResponse
import com.example.myapplication.io.response.InfoSeguidoresResponse
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OtroOyente : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var btnFollow: Button
    private lateinit var usernameText: TextView
    private lateinit var lastMessageText: TextView
    private lateinit var profileImage: ImageView

    private var isFollowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.perfil_otro_oyente)

        apiService = ApiService.create()

        val nombreUser = intent.getStringExtra("nombre")
        val imagenUrl = intent.getStringExtra("imagen")

        // Inicialización de vistas
        btnFollow = findViewById(R.id.btnFollow)
        usernameText = findViewById(R.id.username)
        lastMessageText = findViewById(R.id.lastMessage)
        profileImage = findViewById(R.id.profileImage)




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
                // Lógica para seguir al oyente
                lastMessageText.visibility = View.VISIBLE // Mostrar el último mensaje
            } else {
                if (nombreUser != null) {
                    onFollowStatusChanged(nombreUser,false)
                }
                // Lógica para dejar de seguir al oyente
                lastMessageText.visibility = View.GONE // Ocultar el último mensaje
            }
        }

        if (nombreUser != null) {
            getDatosOyente(nombreUser)
        }
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
                            val foto = it.oyente.fotoPerfil
                            val siguiendo = it.oyente.siguiendo

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
                            if (!foto.isNullOrEmpty()) {
                                Glide.with(this@OtroOyente)
                                    .load(foto)
                                    .placeholder(R.drawable.ic_profile) // Imagen por defecto mientras carga
                                    .error(R.drawable.ic_profile) // Imagen por defecto si hay error
                                    .circleCrop() // Para que la imagen sea circular
                                    .into(fotoImageView)
                            }

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

}