package com.example.myapplication.activities

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
import com.example.myapplication.io.request.RegisterArtistRequest
import com.example.myapplication.io.response.RegisterArtistResponse
import com.example.myapplication.io.response.RegisterUserResponse
import com.example.myapplication.utils.Preferencias
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterArtist : AppCompatActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextArtisticname: EditText
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registro_artista)

        // Inicializar ApiService
        apiService = ApiService.create()

        // Referenciar los EditText desde el layout
        editTextUsername = findViewById(R.id.nombreUsuario)
        editTextEmail = findViewById(R.id.email)
        editTextArtisticname = findViewById(R.id.nombreArtisitico)
        editTextPassword = findViewById(R.id.contraseña)


        // Referenciar el botón de registro
        val buttonRegister: Button = findViewById(R.id.enviarSolicitud)
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
            val artisticname = editTextArtisticname.text.toString().trim()

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
                if (password.isEmpty() ) {
                    showToast("Todos los campos son obligatorios.")
                }
                return@setOnClickListener
            }

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && artisticname.isNotEmpty()) {
                registerArtist(email, username, password, artisticname)
            } else {
                showToast("Todos los campos son obligatorios")
            }
        }
    }

    private fun registerArtist(correo: String, nombreUsuario: String, contrasenya: String, nombreArtistico: String) {
        Log.d("EnvioRegistro", "Respuesta exitosa: ${correo}")
        Log.d("EnvioRegistro", "Respuesta exitosa: ${nombreUsuario}")
        Log.d("EnvioRegistro", "Respuesta exitosa: ${contrasenya}")
        Log.d("EnvioRegistro", "Respuesta exitosa: ${nombreArtistico}")

        val request = RegisterArtistRequest(correo, nombreUsuario, contrasenya, nombreArtistico)

        apiService.postRegisterArtista(request).enqueue(object : Callback<RegisterArtistResponse> {
            override fun onResponse(call: Call<RegisterArtistResponse>, response: Response<RegisterArtistResponse>) {
                if (response.isSuccessful) {
                    val registerResponse = response.body()
                    if (registerResponse != null) {
                        Log.d("MiApp", "Respuesta exitosa: ${registerResponse}")
                        if (registerResponse.respuestaHTTP == 0) {
                            showToast("Registro exitoso")
                            guardarDatosOyente(registerResponse)
                            navigateToPendiente()
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
                        Toast.makeText(this@RegisterArtist, errorMessage, Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@RegisterArtist, "Error desconocido.", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<RegisterArtistResponse>, t: Throwable) {
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

    private fun guardarDatosOyente(registerResponse: RegisterArtistResponse) {
        // Agregar logs para cada valor que se guarda
        Log.d("guardarDatosArtista", "Guardando datos del usuario")

        // Guardar los valores utilizando la clase Preferencias

        Preferencias.guardarValorString("tipo", registerResponse.tipo ?: "")
        Log.d("guardarDatosArtista", "Tipo guardado: ${registerResponse.tipo ?: "null"}")

        Preferencias.guardarValorString("token", registerResponse.token ?: "")
        Log.d("guardarDatosArtista", "Token guardado: ${registerResponse.token ?: "null"}")

        Preferencias.guardarValorString("correo", registerResponse.pendiente?.correo ?: "")
        Log.d("guardarDatosArtista", "Correo guardado: ${registerResponse.pendiente?.correo ?: "null"}")

        Preferencias.guardarValorString("nombreUsuario", registerResponse.pendiente?.nombreUsuario ?: "")
        Log.d("guardarDatosArtista", "Nombre de usuario guardado: ${registerResponse.pendiente?.nombreUsuario ?: "null"}")
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
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToPendiente() {
        val intent = Intent(this, Pendiente::class.java)
        startActivity(intent)
        finish()
    }
}
