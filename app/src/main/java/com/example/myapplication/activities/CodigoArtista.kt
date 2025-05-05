package com.example.myapplication.activities

import android.annotation.SuppressLint
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
import com.example.myapplication.io.response.VerifyArtistResponse
import com.example.myapplication.io.request.VerifyArtistRequest
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CodigoArtista : AppCompatActivity() {

    private lateinit var editTextCode: EditText
    private lateinit var apiService: ApiService

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.codigo_registro_artista)

        // Inicializar ApiService
        apiService = ApiService.create()

        // Referenciar los EditText desde el layout
        editTextCode = findViewById(R.id.codigo)

        // Referenciar el botón de registro
        val buttonRegister: Button = findViewById(R.id.nextButton)

        // Evento clic del botón de registro
        buttonRegister.setOnClickListener {
            val codigo = editTextCode.text.toString().trim()


            if (codigo.isNotEmpty() ) {
                enviarcodigo(codigo)
            } else {
                showToast("Todos los campos son obligatorios")
            }
        }

        val btnTogglePassword = findViewById<ImageButton>(R.id.btnTogglePassword)
        val font = ResourcesCompat.getFont(this, R.font.poppins_regular)
        val typefaceSpan = TypefaceSpan(font!!)
        var passwordVisible = false

        btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible

            if (passwordVisible) {
                // Mostrar la contraseña
                editTextCode.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_on) // Ojo abierto
            } else {
                // Ocultar la contraseña
                editTextCode.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_off) // Ojo cerrado
            }

            // Para mantener el cursor al final del texto
            editTextCode.setSelection(editTextCode.text.length)

        }

        // Aplicar la fuente con Spannable
        val text = editTextCode.text.toString()
        val spannable = SpannableString(text)
        spannable.setSpan(typefaceSpan, 0, text.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        editTextCode.setText(spannable)
    }

    private fun enviarcodigo(codigo: String?) {
        val request = VerifyArtistRequest(codigo)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.verifyArtista(authHeader,request).enqueue(object : Callback<VerifyArtistResponse> {

            override fun onResponse(call: Call<VerifyArtistResponse>, response: Response<VerifyArtistResponse>) {
                Log.d("MiApp", "Respuesta : ${response.code()}")
                if (response.isSuccessful) {
                    val registerResponse = response.body()
                    if (registerResponse != null) {
                        Log.d("MiApp", "Respuesta exitosa: ${registerResponse}")
                        Log.d("MiApp", "Respuesta exitosa: ${registerResponse.tipo}")
                        Log.d("MiApp", "Respuesta exitosa: ${registerResponse.artista_valido?.biografia}")
                        Log.d("MiApp", "Respuesta exitosa: ${registerResponse.artista_valido?.nombreArtistico}")
                        if (registerResponse.respuestaHTTP == 0) {
                            showToast("Registro exitoso")
                            guardarDatosArtista(registerResponse)
                            navigateToMainScreen()
                        } else {
                            handleErrorCode(registerResponse.respuestaHTTP)
                        }
                    } else {
                        showToast("Error: Respuesta vacía del servidor")
                    }
                } else {
                    showToast("Error en el verificar codigo: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<VerifyArtistResponse>, t: Throwable) {
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

    private fun guardarDatosArtista(registerResponse: VerifyArtistResponse) {
        // Agregar logs para cada valor que se guarda
        Log.d("guardarDatosArtista", "Guardando datos del usuario")

        // Guardar los valores utilizando la clase Preferencias
        Preferencias.guardarValorString("token", registerResponse.token ?: "")
        Log.d("guardarDatosArtista", "Token guardado: ${registerResponse.token ?: "null"}")

        Preferencias.guardarValorString("correo", registerResponse.artista_valido?.correo ?: "")
        Log.d("guardarDatosArtista", "Correo guardado: ${registerResponse.artista_valido?.correo ?: "null"}")

        Preferencias.guardarValorString("nombreArtistico", registerResponse.artista_valido?.nombreArtistico ?: "")
        Log.d("guardarDatosArtista", "nombreArtistico guardado: ${registerResponse.artista_valido?.nombreArtistico ?: "null"}")

        Preferencias.guardarValorString("biografia", registerResponse.artista_valido?.biografia ?: "")
        Log.d("guardarDatosArtista", "Biografia guardado: ${registerResponse.artista_valido?.biografia ?: "null"}")

        Preferencias.guardarValorString("nombreUsuario", registerResponse.artista_valido?.nombreUsuario ?: "")
        Log.d("guardarDatosArtista", "Nombre de usuario guardado: ${registerResponse.artista_valido?.nombreUsuario ?: "null"}")

        Preferencias.guardarValorString("fotoPerfil", registerResponse.artista_valido?.fotoPerfil ?: "")
        Log.d("guardarDatosArtista", "Foto de perfil guardada: ${registerResponse.artista_valido?.fotoPerfil ?: "null"}")

        Preferencias.guardarValorEntero("volumen", registerResponse.artista_valido?.volumen ?: 0)
        Log.d("guardarDatosArtista", "volumen: ${registerResponse.artista_valido?.volumen ?: 0}")

        Preferencias.guardarValorString("tipo", registerResponse.tipo ?: "")
        Log.d("guardarDatosArtista", "volumen: ${registerResponse.tipo ?: ""}")

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