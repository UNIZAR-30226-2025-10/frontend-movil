package com.example.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.LoginBinding
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.LoginResponse
import com.example.myapplication.io.request.LoginRequest
import com.example.myapplication.io.response.CancionActualResponse
import com.example.myapplication.io.response.HayNotificacionesResponse
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
                    Log.e("MiApp", "Error 503: ${response.body()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
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
        Log.d("guardarDatosOyente", "Volumen: ${loginResponse.usuario?.volumen ?: 0}")

        getMiniReproductor()

        // Conectar el WebSocket después de guardar los datos del usuario
        val token = loginResponse.token ?: ""
        val webSocketManager = WebSocketManager.getInstance()

        webSocketManager.connectWebSocket(token,
            { message -> Log.d("WebSocket", "Mensaje recibido: $message") },
            { error -> Log.e("WebSocket", "Error de conexión: $error") }
        )

        comprobarSiHayNotificaciones {
            navigate(loginResponse)
        }

    }

    private fun getMiniReproductor() {
        val token = Preferencias.obtenerValorString("token", "")
        apiService.getCancionActual("Bearer $token").enqueue(object : Callback<CancionActualResponse> {
            override fun onResponse(call: Call<CancionActualResponse>, response: Response<CancionActualResponse>) {
                Log.d("Reproductor", "entra en on response MiniRepoductor")
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            Log.d("Reproductor", "Peticion valida reproductor")

                            // Guardar los datos en Preferencias de forma segura
                            it.cancion?.let { cancion ->
                                Preferencias.guardarValorString("cancionActualId", cancion.id ?: "")
                                Log.d("MiniReproductor", "ID canción guardado: ${cancion.id ?: "null"}")

                                Preferencias.guardarValorString("audioCancionActual", cancion.audio ?: "")
                                Log.d("MiniReproductor", "Audio canción guardado: ${cancion.audio ?: "null"}")

                                Preferencias.guardarValorString("nombreCancionActual", cancion.nombre ?: "")
                                Log.d("MiniReproductor", "Nombre canción guardado: ${cancion.nombre ?: "null"}")

                                Preferencias.guardarValorString("nombreArtisticoActual", cancion.nombreArtisticoArtista ?: "")
                                Log.d("MiniReproductor", "Nombre artístico guardado: ${cancion.nombreArtisticoArtista ?: "null"}")

                                Preferencias.guardarValorString("nombreUsuarioArtistaActual", cancion.nombreUsuarioArtista ?: "")
                                Log.d("MiniReproductor", "Nombre usuario artista guardado: ${cancion.nombreUsuarioArtista ?: "null"}")

                                Preferencias.guardarValorEntero("progresoCancionActual", cancion.progreso ?: 0)
                                Log.d("MiniReproductor", "Progreso canción guardado: ${cancion.progreso ?: 0}")

                                Preferencias.guardarValorString("featuringsActual", cancion.featuring?.joinToString(",") ?: "")
                                Log.d("MiniReproductor", "Featurings guardados: ${cancion.featuring?.joinToString(",") ?: "null"}")

                                Preferencias.guardarValorBooleano("favoritoActual", cancion.fav ?: false)
                                Log.d("MiniReproductor", "Favorito guardado: ${cancion.fav}")

                                Preferencias.guardarValorString("fotoPortadaActual", cancion.fotoPortada ?: "")
                                Log.d("MiniReproductor", "Foto portada guardada: ${cancion.fotoPortada ?: "null"}")
                            }

                            // Guardar datos de la colección (si hay)
                            it.coleccion?.let { coleccion ->
                                Preferencias.guardarValorString("coleccionActualId", coleccion.id ?: "")
                                Log.d("MiniReproductor", "ID colección guardado: ${coleccion.id ?: "null"}")

                                Preferencias.guardarValorString("ordenColeccionActual", coleccion.orden?.joinToString(",") ?: "")
                                Log.d("MiniReproductor", "Orden colección guardado: ${coleccion.orden?.joinToString(",") ?: "null"}")

                                Preferencias.guardarValorString("ordenNaturalColeccionActual", coleccion.ordenNatural?.joinToString(",") ?: "")
                                Log.d("MiniReproductor", "Orden natural colección guardado: ${coleccion.ordenNatural?.joinToString(",") ?: "null"}")

                                Preferencias.guardarValorEntero("indexColeccionActual", coleccion.index ?: 0)
                                Log.d("MiniReproductor", "Índice colección guardado: ${coleccion.index ?: 0}")

                                Preferencias.guardarValorString("modoColeccionActual", coleccion.modo ?: "")
                                Log.d("MiniReproductor", "Modo colección guardado: ${coleccion.modo ?: "null"}")
                            }
                        } else {
                            handleErrorCode(it.respuestaHTTP)
                        }
                    } ?: Log.d("MiniReproductor", "Busqueda datos coleccion fallida")
                } else {
                    Log.d("MiniReproductor","Error en la búsqueda: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<CancionActualResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
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
        }else if(loginResponse.tipo == "admin"){
            Log.d("guardarDatosOyente", "Es valido: ${loginResponse.tipo ?: "null"}")
            val intent = Intent(this, Admin::class.java)
            startActivity(intent)
            finish()
        }else{
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun comprobarSiHayNotificaciones(onComplete: () -> Unit) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.hayNotificaciones(authHeader).enqueue(object :
            Callback<HayNotificacionesResponse> {
            override fun onResponse(call: Call<HayNotificacionesResponse>, response: Response<HayNotificacionesResponse>) {
                if (response.isSuccessful) {
                    val respuesta = response.body()
                    respuesta?.let {
                        if (respuesta.invitaciones || respuesta.novedadesMusicales || respuesta.interacciones || respuesta.seguidores) {
                            Log.d("MiApp", "dentro if grande")
                            Preferencias.guardarValorBooleano("hay_notificaciones", true)
                            Log.d("MiApp", "${Preferencias.obtenerValorBooleano("hay_notificaciones", false)}")
                            if (respuesta.invitaciones) {
                                Preferencias.guardarValorBooleano("hay_notificaciones_invitaciones", true)
                            }
                            if (respuesta.novedadesMusicales) {
                                Preferencias.guardarValorBooleano("hay_notificaciones_novedades", true)
                            }
                            if (respuesta.interacciones) {
                                Preferencias.guardarValorBooleano("hay_notificaciones_interacciones", true)
                            }
                            if (respuesta.seguidores) {
                                Preferencias.guardarValorBooleano("hay_notificaciones_seguidores", true)
                            }

                        } else {
                            Preferencias.guardarValorBooleano("hay_notificaciones", false)
                            Preferencias.guardarValorBooleano("hay_notificaciones_invitaciones", false)
                            Preferencias.guardarValorBooleano("hay_notificaciones_novedades", false)
                            Preferencias.guardarValorBooleano("hay_notificaciones_interacciones", false)
                            Preferencias.guardarValorBooleano("hay_notificaciones_seguidores", false)
                        }
                        onComplete()
                    }
                }
            }
            override fun onFailure(call: Call<HayNotificacionesResponse>, t: Throwable) {
                Log.d("Hay Notificaciones", "Error en la solicitud: ${t.message}")
                onComplete()
            }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}