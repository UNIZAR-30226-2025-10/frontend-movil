package com.example.myapplication.activities

import android.annotation.SuppressLint
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
import com.example.myapplication.io.response.CambiarPass3Response
import com.example.myapplication.io.request.CambiarPass3Request
import com.example.myapplication.utils.Preferencias
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CambiarPassword3 : AppCompatActivity() {

    private lateinit var editTextCode: EditText
    private lateinit var apiService: ApiService
    private var yaRedirigidoAlLogin = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MiApp2", "antes")
        setContentView(R.layout.cambiar_contrasegna3)
        Log.d("MiApp2", "despues")

        // Inicializar ApiService
        apiService = ApiService.create()
        Log.d("MiApp2", "despues2")

        // Referenciar los EditText desde el layout
        editTextCode = findViewById(R.id.newPass)
        Log.d("MiApp2", "despues3")

        //Recuperar el correo
        val correo = intent.getStringExtra("correo") ?: ""
        Log.d("MiApp2", "despues4")

        // Referenciar el botón de cambiar contraseña
        val buttonNext: Button = findViewById(R.id.nextButton)
        Log.d("MiApp2", "despues5")

        // Evento clic del botón de registro
        buttonNext.setOnClickListener {
            val nuevaPass = editTextCode.text.toString().trim()

            if (!isValidPassword(this, nuevaPass)) {
                if (nuevaPass.isEmpty()) {
                    showToast("Introduce una contraseña")
                }
                return@setOnClickListener
            }
            else {
                enviarNuevaPass(correo, nuevaPass)
            }
        }

        val btnTogglePassword = findViewById<ImageButton>(R.id.btnTogglePassword)
        val font = ResourcesCompat.getFont(this, R.font.poppins_regular)
        val typefaceSpan = TypefaceSpan(font!!)
        var passwordVisible = false

        btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible

            val typeface = editTextCode.typeface
            if (passwordVisible) {
                // Mostrar la contraseña
                editTextCode.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_off) // Ojo abierto
            } else {
                // Ocultar la contraseña
                editTextCode.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_on) // Ojo cerrado
            }
            editTextCode.typeface = typeface
            // Para mantener el cursor al final del texto
            editTextCode.setSelection(editTextCode.text.length)
        }

        // Aplicar la fuente con Spannable
        val text = editTextCode.text.toString()
        val spannable = SpannableString(text)
        spannable.setSpan(typefaceSpan, 0, text.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        editTextCode.setText(spannable)
    }

    private fun enviarNuevaPass(correo: String, nuevaPass: String?) {
        val request = CambiarPass3Request(correo, nuevaPass)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.CambiarPass3(authHeader,request).enqueue(object : Callback<Void> {

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.d("MiApp", "Respuesta : ${response.code()}")
                if (response.isSuccessful) {
                    val registerResponse = response.body()
                    navigateLogin()
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        val errorBody = response.errorBody()?.string()

                        try {
                            val json = JSONObject(errorBody ?: "")
                            val errorMessage = json.getString("error")

                            if (errorMessage == "Token inválido.") {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@CambiarPassword3, Inicio::class.java)
                                startActivity(intent)
                                finish()
                                showToast("Sesión iniciada en otro dispositivo")
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
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
        // showToast(message)
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

    private fun navigateLogin() {
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        finish()
    }
}