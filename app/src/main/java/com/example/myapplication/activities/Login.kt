package com.example.myapplication.activities

import android.app.Dialog
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TypefaceSpan
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.example.myapplication.R
import com.example.myapplication.databinding.LoginBinding
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.LoginResponse
import com.example.myapplication.io.request.LoginRequest
import com.example.myapplication.io.response.CancionActualResponse
import com.example.myapplication.io.response.HayNotificacionesResponse
import com.example.myapplication.utils.Preferencias
import org.json.JSONObject
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
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnTogglePassword = findViewById<ImageButton>(R.id.btnTogglePassword)
        val font = ResourcesCompat.getFont(this, R.font.poppins_regular)
        val typefaceSpan = TypefaceSpan(font!!)
        var passwordVisible = false

        btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible

            val typeface = etPassword.typeface
            if (passwordVisible) {
                // Mostrar la contraseña
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_off) // Ojo abierto
            } else {
                // Ocultar la contraseña
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_on) // Ojo cerrado
            }

            etPassword.typeface = typeface
            // Para mantener el cursor al final del texto
            etPassword.setSelection(etPassword.text.length)
        }

        // Aplicar la fuente con Spannable
        val text = etPassword.text.toString()
        val spannable = SpannableString(text)
        spannable.setSpan(typefaceSpan, 0, text.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        etPassword.setText(spannable)

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
                            guardarDatosUsuario(loginResponse)
                        } else{
                            handleErrorCode(loginResponse.respuestaHTTP)
                        }
                    } else {
                        showToast("Inicio de sesión fallido: Datos incorrectos")
                    }
                } else {
                    if (response.code() == 403) {
                        cambiar_sesion(loginRequest)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        try {
                            val json = JSONObject(errorBody)
                            val errorMessage = json.getString("error")
                            Toast.makeText(this@Login, errorMessage, Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@Login, "Error desconocido.", Toast.LENGTH_LONG).show()
                        }
                    }
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

        Preferencias.guardarValorBooleano("primerinicio", false)

        if(loginResponse.tipo != "admin") {
            getMiniReproductor()
        }

        // Conectar el WebSocket después de guardar los datos del usuario
        val token = loginResponse.token ?: ""
        Log.d("WebSocket", "Token: ${token}")
        val webSocketManager = WebSocketManager.getInstance()

        webSocketManager.connectWebSocket(token,
            { message -> Log.d("WebSocket", "Mensaje recibido: $message") },
            { error -> Log.e("WebSocket", "Error de conexión: $error") }
        )

        if(loginResponse.tipo == "admin" || loginResponse.tipo == "valido" || loginResponse.tipo == "pendiente") {
            navigate(loginResponse)
        }
        else{
            comprobarSiHayNotificaciones {
                navigate(loginResponse)
            }
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

                                //val progresoEnApp = cancion.progreso?.times(1000).toInt()
                                val progresoEnApp = ((cancion.progreso ?: 0.0) * 1000).toInt()
                                Preferencias.guardarValorEntero("progresoCancionActual", progresoEnApp ?: 0)
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
                Log.d("MiniReproductor","Error al pedir cancion: ${t.message}")
                showToast("Error al pedir cancion: ${t.message}")
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

    private fun cambiar_sesion(loginRequest: LoginRequest){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_switch_session)

        val window: Window? = dialog.window
        window?.setLayout((Resources.getSystem().displayMetrics.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.setCancelable(false)

        val btnAceptar = dialog.findViewById<Button>(R.id.btnAceptar)
        val btnRechazar = dialog.findViewById<Button>(R.id.btnRechazar)


        btnAceptar.setOnClickListener {
            // Llamada a la API para cambiar de sesión
            apiService.switch_session(loginRequest).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse != null && loginResponse.respuestaHTTP == 0) {
                            showToast("Sesión cambiada con éxito")
                            guardarDatosUsuario(loginResponse)
                        } else {
                            showToast("No se pudo cambiar la sesión: ${loginResponse?.respuestaHTTP}")
                        }
                    } else {
                        showToast("Error al cambiar sesión: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    showToast("Fallo al cambiar sesión: ${t.message}")
                }
            })

            dialog.dismiss()
        }

        btnRechazar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}