package com.example.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.LoginBinding
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.LoginResponse
import com.example.myapplication.io.request.LoginRequest
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Login : AppCompatActivity() {

    private lateinit var binding: LoginBinding
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialización de ApiService
        apiService = ApiService.create()

        // Login Button
        binding.loginButton.setOnClickListener {
            val email = binding.etName.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Validación de los campos
            if (email.isNotEmpty() && password.isNotEmpty()) {
                realizarLogin(email, password)
            } else {
                Toast.makeText(this@Login, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun realizarLogin(email: String, password: String) {
        val loginRequest = LoginRequest(email, password)

        apiService.postlogin(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null && loginResponse.token != null) {
                        guardarDatosUsuario(loginResponse)
                        redirigirUsuario()
                    } else {
                        mostrarMensaje("Inicio de sesión fallido: Datos incorrectos")
                    }
                } else {
                    mostrarMensaje("Error en la solicitud: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                mostrarMensaje("Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun guardarDatosUsuario(loginResponse: LoginResponse) {
        // Usando la clase Preferencias para guardar los datos
        Preferencias.guardarValorString("token", loginResponse.token ?: "")
        Preferencias.guardarValorString("correo", loginResponse.correo ?: "")
        Preferencias.guardarValorString("nombreUsuario", loginResponse.nombreUsuario ?: "")
        Preferencias.guardarValorString("fotoUsuario", loginResponse.fotoUsuario ?: "")
        Preferencias.guardarValorBooleano("esOyente", loginResponse.esOyente ?: false)
        Preferencias.guardarValorBooleano("esArtista", loginResponse.esArtista ?: false)
        Preferencias.guardarValorBooleano("esPendiente", loginResponse.esPendiente ?: false)
        Preferencias.guardarValorString("nombreArtistico", loginResponse.nombreArtistico ?: "")
        //Preferencias.guardarValorBooleano("is_logged_in", true)
    }

    private fun redirigirUsuario() {
        val intent = Intent(this, Home::class.java)
        startActivity(intent)
        finish()
    }

    private fun mostrarMensaje(message: String) {
        Toast.makeText(this@Login, message, Toast.LENGTH_SHORT).show()
    }
}
