package com.example.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.RegisterUserRequest
import com.example.myapplication.io.response.RegisterUserResponse
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterUser : AppCompatActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registro_usuario)

        // Inicializar ApiService
        apiService = ApiService.create()

        // Referenciar los EditText desde el layout
        editTextUsername = findViewById(R.id.username)
        editTextEmail = findViewById(R.id.email)
        editTextPassword = findViewById(R.id.password)

        // Referenciar el botón de registro
        val buttonRegister: Button = findViewById(R.id.registerButton)

        // Evento clic del botón de registro
        buttonRegister.setOnClickListener {
            val username = editTextUsername.text.toString().trim()
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                registerUser(username, email, password)
            } else {
                showToast("Todos los campos son obligatorios")
            }
        }
    }

    private fun registerUser(nombreUsuario: String, correo: String, contrasenya: String) {
        val request = RegisterUserRequest(correo, nombreUsuario, contrasenya, true)

        apiService.postRegisterOyente(request).enqueue(object : Callback<RegisterUserResponse> {
            override fun onResponse(call: Call<RegisterUserResponse>, response: Response<RegisterUserResponse>) {
                if (response.isSuccessful) {
                    val registerResponse = response.body()
                    if (registerResponse != null) {
                        if (registerResponse.respuestaHTTP == 200) {
                            showToast("Registro exitoso")
                            guardarDatosUsuario(registerResponse)
                            navigateToMainScreen()
                        } else {
                            handleErrorCode(registerResponse.respuestaHTTP)
                        }
                    } else {
                        showToast("Error: Respuesta vacía del servidor")
                    }
                } else {
                    showToast("Error en el registro: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<RegisterUserResponse>, t: Throwable) {
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

    private fun guardarDatosUsuario(registerResponse: RegisterUserResponse) {
        // Usando la clase Preferencias para guardar los datos
        Preferencias.guardarValorString("token", registerResponse.token ?: "")
        Preferencias.guardarValorString("correo", registerResponse.correo ?: "")
        Preferencias.guardarValorString("nombreUsuario", registerResponse.nombreUsuario ?: "")
        Preferencias.guardarValorString("fotoUsuario", registerResponse.fotoUsuario ?: "")
        Preferencias.guardarValorBooleano("esOyente", registerResponse.esOyente ?: false)
        Preferencias.guardarValorBooleano("esArtista", registerResponse.esArtista ?: false)
        Preferencias.guardarValorBooleano("esPendiente", registerResponse.esPendiente ?: false)
        Preferencias.guardarValorString("nombreArtistico", registerResponse.nombreArtistico ?: "")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToMainScreen() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
