package com.example.myapplication.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TypefaceSpan
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.RegisterUserRequest
import com.example.myapplication.io.response.RegisterUserResponse
import com.example.myapplication.utils.Preferencias
import org.json.JSONObject
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

        val btnTogglePassword = findViewById<ImageButton>(R.id.btnTogglePassword)
        val font = ResourcesCompat.getFont(this, R.font.poppins_regular)
        val typefaceSpan = TypefaceSpan(font!!)
        var passwordVisible = false
        val buttonInicio: Button = findViewById(R.id.loginLink)

        buttonInicio.setOnClickListener{
            startActivity(Intent(this, Login::class.java))
        }

        btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible

            val typeface = editTextPassword.typeface

            if (passwordVisible) {
                // Mostrar la contraseña
                editTextPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_off) // Ojo abierto
            } else {
                // Ocultar la contraseña
                editTextPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_on) // Ojo cerrado
            }

            editTextPassword.typeface = typeface
            // Para mantener el cursor al final del texto
            editTextPassword.setSelection(editTextPassword.text.length)
        }

        // Evento clic del botón de registro
        buttonRegister.setOnClickListener {
            val username = editTextUsername.text.toString().trim()
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()


            // Validaciones
            if (!isValidUsername(username)) {
                if (username.isEmpty() ) {
                    showToast("Todos los campos son obligatorios.")
                } else {
                    showToast("El nombre de usuario no puede contener '@', espacios ni comas.")
                }
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                if (email.isEmpty()) {
                    showToast("Todos los campos son obligatorios.")
                } else {
                    showToast("El correo electrónico debe contener un '@'.")
                }
                return@setOnClickListener
            }

            if (!isValidPassword(this, password)) {
                if (password.isEmpty()) {
                    showToast("Todos los campos son obligatorios.")
                }
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
                    val errorBody = response.errorBody()?.string()
                    try {
                        val json = JSONObject(errorBody)
                        val errorMessage = json.getString("error")
                        Toast.makeText(this@RegisterUser, errorMessage, Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@RegisterUser, "Error desconocido.", Toast.LENGTH_LONG).show()
                    }
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
        Log.d("guardarDatosOyente", "Guardando datos del usuario")

        Preferencias.guardarValorString("token", registerResponse.token ?: "")
        Log.d("guardarDatosOyente", "Token guardado: ${registerResponse.token ?: "null"}")

        Preferencias.guardarValorString("correo", registerResponse.oyente?.correo ?: "")
        Log.d("guardarDatosOyente", "Correo guardado: ${registerResponse.oyente?.correo ?: "null"}")

        Preferencias.guardarValorString("fotoPerfil", registerResponse.oyente?.fotoPerfil ?: "")
        Log.d("guardarDatosOyente", "Foto de perfil guardada: ${registerResponse.oyente?.fotoPerfil ?: "null"}")

        Preferencias.guardarValorString("nombreUsuario", registerResponse.oyente?.nombreUsuario ?: "")
        Log.d("guardarDatosOyente", "Nombre de usuario guardado: ${registerResponse.oyente?.nombreUsuario ?: "null"}")

        Preferencias.guardarValorString("esOyente", registerResponse.tipo ?: "")
        Log.d("guardarDatosOyente", "Es oyente: ${registerResponse.tipo ?: ""}")

        Preferencias.guardarValorEntero("volumen", registerResponse.oyente?.volumen ?: 0)
        Log.d("guardarDatosOyente", "Es artista: ${registerResponse.oyente?.volumen ?: 0}")
    }

    // Función para validar el nombre de usuario (no debe contener "@")
    private fun isValidUsername(username: String): Boolean {
        return username.isNotEmpty() &&
                !username.contains("@") &&
                !username.contains(" ") &&
                !username.contains(",")
    }

    // Función para validar el correo (debe contener "@")
    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.isNotEmpty()
    }

    // Función para validar la contraseña (mínimo 10 caracteres, 1 letra y 1 carácter especial)
    private fun isValidPassword(context: Context, password: String): Boolean {
        return when {
            password.isEmpty() -> {
                false
            }
            password.length < 10 -> {
                Toast.makeText(context, "La contraseña debe tener al menos 10 caracteres.", Toast.LENGTH_LONG).show()
                false
            }
            !password.any { it.isLetter() } -> {
                Toast.makeText(context, "La contraseña debe contener al menos una letra.", Toast.LENGTH_LONG).show()
                false
            }
            !password.any { it.isDigit() || "!@#\$%^&*()_+-=[]{};':\"\\|,.<>/?".contains(it) } -> {
                Toast.makeText(context, "La contraseña debe contener al menos un número o carácter especial.", Toast.LENGTH_LONG).show()
                false
            }
            else -> true
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToMainScreen() {
        val intent = Intent(this, Home::class.java)
        startActivity(intent)
        finish()
    }
}
