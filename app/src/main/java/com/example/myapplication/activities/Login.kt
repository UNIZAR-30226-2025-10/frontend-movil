package com.example.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.LoginBinding
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.LoginResponse
import com.example.myapplication.io.request.LoginRequest
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Login : AppCompatActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        // Inicialización de ApiService
        apiService = ApiService.create()

        //Referenciar los EditText desde el layout
        editTextUsername = findViewById(R.id.etName)
        editTextPassword = findViewById(R.id.etPassword)

        //Referenciar el botones
        val buttonLogin: Button = findViewById(R.id.loginButton)
        val buttonRegister: Button = findViewById(R.id.registerLink)
        val buttonForgotPass: Button = findViewById(R.id.forgotPass)

        //Evento clic del he olvidado contraseña
        buttonForgotPass.setOnClickListener{
            startActivity(Intent(this, CambiarPassword1::class.java))
        }

        //Evento clic de ir a registro
        buttonRegister.setOnClickListener{
             startActivity(Intent(this, ElegirRegistro::class.java))
        }

        // Evento clic del botón de login
        buttonLogin.setOnClickListener {
            val username = editTextUsername.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            // Validación de los campos
            if (username.isNotEmpty() && password.isNotEmpty()) {
                realizarLogin(username, password)
            } else {
                Toast.makeText(this@Login, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun realizarLogin(nombreUsuario: String, contrasenya: String) {
        val loginRequest: LoginRequest
        if (nombreUsuario.contains("@")) {
            loginRequest = LoginRequest(correo = nombreUsuario, nombreUsuario = null, contrasenya = contrasenya)
        } else {
            loginRequest = LoginRequest(correo = null, nombreUsuario = nombreUsuario, contrasenya = contrasenya)
        }

        apiService.postlogin(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null) {
                        Log.d("MiApp", "Respuesta exitosa: ${loginResponse}")
                        Log.d("MiApp", "Respuesta exitosa tipo: ${loginResponse.tipo}")
                        Log.d("MiApp", "Respuesta exitosa correo: ${loginResponse.usuario?.correo}")
                        Log.d("MiApp", "Respuesta exitosa foto: ${loginResponse.usuario?.fotoPerfil}")
                        Log.d("MiApp", "Respuesta exitosa user: ${loginResponse.usuario?.nombreUsuario}")
                        Log.d("MiApp", "Respuesta exitosa volumen: ${loginResponse.usuario?.volumen}")
                        if(loginResponse.respuestaHTTP == 0){
                            showToast("Login existoso")
                            guardarDatosUsuario(loginResponse)
                            navigate(loginResponse)
                        } else{
                            handleErrorCode(loginResponse.respuestaHTTP)
                        }
                    } else {
                        showToast("Inicio de sesión fallido: Datos incorrectos")
                    }
                } else {
                    if (response.code() == 401) {
                        val errorMessage = response.errorBody()?.string() ?: "Error desconocido"
                        if (errorMessage.contains("Nombre de usuario o correo no válido")) {
                            // Error de usuario no válido
                            Log.e("MiApp", "Error 401: Usuario no válido.")
                            showToast("Usuario no válido")
                        } else if (errorMessage.contains("Contraseña incorrecta")) {
                            // Error de contraseña incorrecta
                            Log.e("MiApp", "Error 401: Contraseña incorrecta.")
                            showToast("Contraseña incorrecta")
                        } else {
                            // Otro error de autenticación
                            Log.e("MiApp", "Error 401: $errorMessage")
                            showToast("Error en el login: $errorMessage")
                        }
                    }
                    showToast("Error en el login: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
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

    private fun guardarDatosUsuario(loginResponse: LoginResponse) {
        // Agregar logs para cada valor que se guarda
        Log.d("guardarDatosOyente", "Guardando datos del usuario")

        // Guardar los valores utilizando la clase Preferencias
        Preferencias.guardarValorString("token", loginResponse.token ?: "")
        Log.d("guardarDatosOyente", "Token guardado: ${loginResponse.token ?: "null"}")

        Preferencias.guardarValorString("correo", loginResponse.usuario?.correo ?: "")
        Log.d("guardarDatosOyente", "Correo guardado: ${loginResponse.usuario?.correo ?: "null"}")

        Preferencias.guardarValorString("fotoPerfil", loginResponse.usuario?.fotoPerfil ?: "")
        Log.d("guardarDatosOyente", "Foto de perfil guardada: ${loginResponse.usuario?.fotoPerfil ?: "null"}")

        Preferencias.guardarValorString("nombreUsuario", loginResponse.usuario?.nombreUsuario ?: "")
        Log.d("guardarDatosOyente", "Nombre de usuario guardado: ${loginResponse.usuario?.nombreUsuario ?: "null"}")

        Preferencias.guardarValorString("esOyente", loginResponse.tipo ?: "")
        Log.d("guardarDatosOyente", "Es oyente: ${loginResponse.tipo ?: ""}")

        Preferencias.guardarValorEntero("volumen", loginResponse.usuario?.volumen ?: 0)
        Log.d("guardarDatosOyente", "Es artista: ${loginResponse.usuario?.volumen ?: 0}")

        // Conectar el WebSocket después de guardar los datos del usuario
        val token = loginResponse.token ?: ""
        val webSocketManager = WebSocketManager.getInstance()
        webSocketManager.connectWebSocket(token) { message ->
            // Maneja los mensajes recibidos del servidor
            Log.d("WebSocket", "Mensaje recibido: $message")
        }
    }

    private fun navigate(loginResponse: LoginResponse) {
        if(loginResponse.tipo == "pendiente"){
            val intent = Intent(this, Pendiente::class.java)
            startActivity(intent)
            finish()
        }else if(loginResponse.tipo == "valido"){
            Log.d("guardarDatosOyente", "Es valido: ${loginResponse.tipo ?: "null"}")
            val intent = Intent(this, CodigoArtista::class.java)
            startActivity(intent)
            finish()
        }else{
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}