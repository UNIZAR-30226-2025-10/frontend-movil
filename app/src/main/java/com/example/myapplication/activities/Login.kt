package com.example.myapplication.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.LoginBinding
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.LoginResponse
import com.example.myapplication.io.request.LoginRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Login : AppCompatActivity() {

    private lateinit var binding: LoginBinding
    private lateinit var apiService: ApiService
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialización de ApiService y SharedPreferences
        apiService = ApiService.create()
        sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE)

        // Login Button
        binding.loginButton.setOnClickListener {
            val email = binding.etName.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            // Validación de los campos
            if (email.isNotEmpty() && password.isNotEmpty()) {
                realizarLogin(email,password)
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
        with(sharedPreferences.edit()) {
            putString("token", loginResponse.token)
            putString("correo", loginResponse.correo)
            putString("nombreUsuario", loginResponse.nombreUsuario)
            putString("fotoUsuario", loginResponse.fotoUsuario)
            putBoolean("esOyente", loginResponse.esOyente ?: false)
            putBoolean("esArtista", loginResponse.esArtista ?: false)
            putBoolean("esPendiente", loginResponse.esPendiente ?: false)
            putString("nombreArtistico", loginResponse.nombreArtistico)
            putBoolean("is_logged_in", true)
            apply()
        }
    }

    private fun redirigirUsuario() {
        val intent = Intent(this, Home::class.java)
        startActivity(intent)
        finish()
    }
}
