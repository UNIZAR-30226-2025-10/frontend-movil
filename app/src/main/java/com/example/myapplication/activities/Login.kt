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
        sharedPreferences = getSharedPreferences("my_preference", Context.MODE_PRIVATE)

        // Login Button
        binding.loginButton.setOnClickListener {
            val email = binding.etName.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Validación de los campos
            if (email.isNotEmpty() && password.isNotEmpty()) {
                val loginRequest = LoginRequest(email, password)

                // Llamada a la API para login
                apiService.postlogin(loginRequest).enqueue(object : Callback<LoginResponse> {
                    override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                        if (response.isSuccessful) {
                            val loginResponse = response.body()
                            if (loginResponse != null && loginResponse.token != null) {
                                // Guardamos el token en SharedPreferences
                                Preferencias.guardarValorString("token", loginResponse.token)

                                // Marcar al usuario como logueado
                                Preferencias.guardarValorBooleano("is_logged_in", true)

                                // Opcional: Guardar más datos si los hay (nombre, avatar, etc.)
                                // Preferencias.guardarValorString("user_name", loginResponse.user?.name)

                                // Redirigir a la pantalla principal
                                val intent = Intent(this@Login, Home::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this@Login, "Inicio de sesión fallido", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@Login, "Error en la solicitud", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        Toast.makeText(this@Login, "Error en la solicitud: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this@Login, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

    }
}
