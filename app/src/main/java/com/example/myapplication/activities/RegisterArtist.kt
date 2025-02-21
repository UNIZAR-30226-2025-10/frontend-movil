package com.example.myapplication.activities

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.RegisterResponse
import com.example.myapplication.io.request.RegisterRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterArtist : AppCompatActivity(){
   /* private lateinit var editTextUsername: EditText
    private lateinit var editTextArtisticname: EditText
    private lateinit var apiService: ApiService

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registro_artista)

        // Inicializar ApiService
        apiService = ApiService.create()

        // Referenciar los EditText desde el layout
        editTextUsername = findViewById(R.id.nombreUsuario)
        editTextArtisticname = findViewById(R.id.nombreArtisitico)


        // Referenciar el botón de enviar solicitud desde el layout
        val buttonRegister: Button = findViewById(R.id.enviarSolicitud)

        // Configurar el evento clic del botón de registro
        buttonRegister.setOnClickListener {
            // Obtener los valores de los campos de entrada
            val username = editTextUsername.text.toString()
            val artisticname = editTextArtisticname.text.toString()


            val request = RegisterRequest(username, artisticname)

            // Verificar si las contraseñas coinciden
            if (password == confirmPassword) {
                if(username.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()){
                    // Realizar la llamada de registro al servidor
                    apiService.postRegister(request).enqueue(object : Callback<RegisterResponse> {
                        override fun onResponse(
                            call: Call<RegisterResponse>,
                            response: Response<RegisterResponse>
                        ) {
                            val statusCode = response.code()
                            if (response.isSuccessful) {
                                val registerResponse = response.body()

                                if (registerResponse != null) {
                                    if (statusCode == 201) {
                                        // Registro exitoso
                                        Toast.makeText(
                                            this@RegisterActivity,
                                            "Registro exitoso",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        // Redirigir a la pantalla de inicio de sesión
                                        val intent =
                                            Intent(this@RegisterActivity, LoginActivity::class.java)
                                        startActivity(intent)
                                        finish() // Finalizar la actividad actual para evitar volver atrás
                                    }
                                } else {
                                    //debug
                                    Toast.makeText(
                                        this@RegisterActivity,
                                        "Debug: statusCode != 201",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            else { // !response.isSuccesful eso quiere decir que el statusCode no es 2xx
                                when (statusCode) {
                                    400 -> {
                                        // Error en el registro
                                        Toast.makeText(
                                            this@RegisterActivity,
                                            "Error en el registro: Correo o usuario en uso",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                    }
                                    500 -> {
                                        // Error en la solicitud
                                        Toast.makeText(
                                            this@RegisterActivity,
                                            "Error interno en la solicitud",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                    }
                                    else -> {//codigo de error desconocido
                                        Toast.makeText(
                                            this@RegisterActivity,
                                            "Error con codigo de error desconocido",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }

                        }



                        override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                            // Error en la solicitud
                            Toast.makeText(
                                this@RegisterActivity,
                                "Error en la solicitud: ${t.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                }
                else{
                    Toast.makeText(this@RegisterActivity, "Faltan campos por rellenar", Toast.LENGTH_SHORT).show()
                }

            }
            else {
                // Contraseñas no coinciden
                Toast.makeText(this@RegisterActivity, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            }

        }

    }*/
}