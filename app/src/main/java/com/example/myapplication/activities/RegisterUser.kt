package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.RegisterResponse
import com.example.myapplication.io.request.RegisterRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterUser : AppCompatActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var apiService: ApiService

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registro_usuario)

        // Inicializar ApiService
        apiService = ApiService.create()

        // Referenciar los EditText desde el layout
        editTextUsername = findViewById(R.id.username)
        editTextEmail = findViewById(R.id.email)
        editTextPassword = findViewById(R.id.password)

        // Referenciar el botón de registro desde el layout
        val buttonRegister: Button = findViewById(R.id.registerButton)

        // Configurar el evento clic del botón de registro
        buttonRegister.setOnClickListener {
            // Obtener los valores de los campos de entrada
            val username = editTextUsername.text.toString()
            val email = editTextEmail.text.toString()
            val password = editTextPassword.text.toString()

            val request = RegisterRequest(username, email, password)

            if (username.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                // Realizar la llamada de registro al servidor
                apiService.postRegister(request).enqueue(object : Callback<RegisterResponse> {
                    override fun onResponse(
                        call: Call<RegisterResponse>,
                        response: Response<RegisterResponse>
                    ) {
                        // Verifica si la respuesta es exitosa
                        if (response.isSuccessful) {
                            val registerResponse = response.body()

                            if (registerResponse != null) {
                                // Suponiendo que el código de error 0 es éxito
                                if (registerResponse.codigoError == 0) {
                                    // Registro exitoso
                                    Toast.makeText(
                                        this@RegisterUser,
                                        "Registro exitoso",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Redirigir a la pantalla de inicio de sesión
                                    val intent = Intent(this@RegisterUser, Login::class.java)
                                    startActivity(intent)
                                    finish() // Finalizar la actividad actual
                                } else {
                                    // Manejar error específico de la respuesta (como un correo ya en uso)
                                    Toast.makeText(
                                        this@RegisterUser,
                                        "Error en el registro: Código ${registerResponse.codigoError}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    this@RegisterUser,
                                    "Respuesta vacía del servidor.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            // Si la respuesta no es exitosa
                            handleErrorCode(response.code())
                        }
                    }

                    override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                        // Error en la solicitud
                        Toast.makeText(
                            this@RegisterUser,
                            "Error en la solicitud: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            } else {
                Toast.makeText(this@RegisterUser, "Faltan campos por rellenar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleErrorCode(statusCode: Int) {
        when (statusCode) {
            400 -> {
                // Error en el registro (correo o usuario ya en uso)
                Toast.makeText(
                    this@RegisterUser,
                    "Error en el registro: Correo o usuario en uso",
                    Toast.LENGTH_SHORT
                ).show()
            }
            500 -> {
                // Error interno en la solicitud
                Toast.makeText(
                    this@RegisterUser,
                    "Error interno en la solicitud",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                // Código de error desconocido
                Toast.makeText(
                    this@RegisterUser,
                    "Error con código de error desconocido: $statusCode",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
