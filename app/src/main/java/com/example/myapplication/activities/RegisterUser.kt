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

    private fun registerUser(correo: String, nombreUsuario: String, contrasenya: String) {
        val request = RegisterUserRequest(correo, nombreUsuario, contrasenya, true)

        apiService.postRegisterOyente(request).enqueue(object : Callback<RegisterUserResponse> {
            override fun onResponse(call: Call<RegisterUserResponse>, response: Response<RegisterUserResponse>) {
                if (response.isSuccessful) {
                    val registerResponse = response.body()
                    if (registerResponse != null) {
                        Log.d("MiApp", "Código de respuesta: ${registerResponse.respuestaHTTP}")
                    }
                    if (registerResponse != null) {
                        Log.d("MiApp", "Respuesta exitosa: ${registerResponse}")
                        if (registerResponse.respuestaHTTP == 0) {
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
        // Agregar logs para cada valor que se guarda
        Log.d("guardarDatosUsuario", "Guardando datos del usuario")

        // Guardar los valores utilizando la clase Preferencias
        Preferencias.guardarValorString("token", registerResponse.token ?: "")
        Log.d("guardarDatosUsuario", "Token guardado: ${registerResponse.token ?: "null"}")

        Preferencias.guardarValorString("correo", registerResponse.usuario?.correo ?: "")
        Log.d("guardarDatosUsuario", "Correo guardado: ${registerResponse.usuario?.correo ?: "null"}")

        Preferencias.guardarValorString("fotoPerfil", registerResponse.usuario?.fotoPerfil ?: "")
        Log.d("guardarDatosUsuario", "Foto de perfil guardada: ${registerResponse.usuario?.fotoPerfil ?: "null"}")

        Preferencias.guardarValorString("nombreUsuario", registerResponse.usuario?.nombreUsuario ?: "")
        Log.d("guardarDatosUsuario", "Nombre de usuario guardado: ${registerResponse.usuario?.nombreUsuario ?: "null"}")

        Preferencias.guardarValorString("esOyente", registerResponse.usuario?.tipo ?: "")
        Log.d("guardarDatosUsuario", "Es oyente: ${registerResponse.usuario?.tipo ?: ""}")

        Preferencias.guardarValorEntero("volumen", registerResponse.usuario?.volumen ?: 0)
        Log.d("guardarDatosUsuario", "Es artista: ${registerResponse.usuario?.volumen ?: 0}")

    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToMainScreen() {
        val intent = Intent(this, Home::class.java)
        startActivity(intent)
        finish()
    }
}
