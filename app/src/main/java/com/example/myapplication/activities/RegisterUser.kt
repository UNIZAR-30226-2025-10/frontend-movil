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

            if (!isValidUsername(username)) {
                showToast("El nombre de usuario no puede contener '@'.")
                return@setOnClickListener
            }

            if (!isValidPassword(password)) {
                showToast("La contraseña debe tener al menos 8 caracteres, una letra y un carácter especial.")
                return@setOnClickListener
            }

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
                        Log.d("MiApp", "Respuesta exitosa: ${registerResponse}")
                        if (registerResponse.respuestaHTTP == 0) {
                            showToast("Registro exitoso")
                            guardarDatosOyente(registerResponse)
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

    private fun guardarDatosOyente(registerResponse: RegisterUserResponse) {
        // Agregar logs para cada valor que se guarda
        Log.d("guardarDatosOyente", "Guardando datos del usuario")

        // Guardar los valores utilizando la clase Preferencias
        Preferencias.guardarValorString("token", registerResponse.token ?: "")
        Log.d("guardarDatosOyente", "Token guardado: ${registerResponse.token ?: "null"}")

        Preferencias.guardarValorString("correo", registerResponse.oyente?.correo ?: "")
        Log.d("guardarDatosOyente", "Correo guardado: ${registerResponse.oyente?.correo ?: "null"}")

        Preferencias.guardarValorString("fotoPerfil", registerResponse.oyente?.fotoPerfil ?: "")
        Log.d("guardarDatosOyente", "Foto de perfil guardada: ${registerResponse.oyente?.fotoPerfil ?: "null"}")

        Preferencias.guardarValorString("nombreUsuario", registerResponse.oyente?.nombreUsuario ?: "")
        Log.d("guardarDatosOyente", "Nombre de usuario guardado: ${registerResponse.oyente?.nombreUsuario ?: "null"}")

        Preferencias.guardarValorString("esOyente", registerResponse.oyente?.tipo ?: "")
        Log.d("guardarDatosOyente", "Es oyente: ${registerResponse.oyente?.tipo ?: ""}")

        Preferencias.guardarValorEntero("volumen", registerResponse.oyente?.volumen ?: 0)
        Log.d("guardarDatosOyente", "Es artista: ${registerResponse.oyente?.volumen ?: 0}")

    }

    // Función para validar el nombre de usuario (no debe contener "@")
    private fun isValidUsername(username: String): Boolean {
        return !username.contains("@") && username.isNotEmpty()
    }

    // Función para validar la contraseña (mínimo 8 caracteres, 1 letra y 1 carácter especial)
    private fun isValidPassword(password: String): Boolean {
        val regex = Regex("^(?=.*[A-Za-z])(?=.*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$")
        return regex.matches(password)
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
