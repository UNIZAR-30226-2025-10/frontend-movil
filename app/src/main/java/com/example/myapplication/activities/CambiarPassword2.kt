package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.CambiarPass2Response
import com.example.myapplication.io.request.CambiarPass2Request
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CambiarPassword2 : AppCompatActivity() {

    private lateinit var editTextCode: EditText
    private lateinit var apiService: ApiService

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MiApp", "antes")
        setContentView(R.layout.cambiar_contrasegna2)
        Log.d("MiApp", "despues")

        // Inicializar ApiService
        apiService = ApiService.create()
        Log.d("MiApp", "despues2")

        // Referenciar los EditText desde el layout
        editTextCode = findViewById(R.id.codigo)
        Log.d("MiApp", "despues3")

        //Recuperar el correo
        val correo = intent.getStringExtra("correo") ?: ""

        Log.d("MiApp", "despues4")

        // Referenciar el botón de registro
        val buttonRegister: Button = findViewById(R.id.nextButton)

        Log.d("MiApp", "despues5")
        // Evento clic del botón de registro
        buttonRegister.setOnClickListener {
            val codigo = editTextCode.text.toString().trim()


            if (codigo.isNotEmpty() ) {
                Log.d("MiApp", "despues6")
                enviarcodigo(correo, codigo)
                Log.d("MiApp", "despues7")
            } else {
                showToast("Todos los campos son obligatorios")
            }
        }
    }

    private fun enviarcodigo(correo: String, codigo: String?) {
        val request = CambiarPass2Request(correo, codigo)
        apiService.CambiarPass2(request).enqueue(object : Callback<CambiarPass2Response> {

            override fun onResponse(call: Call<CambiarPass2Response>, response: Response<CambiarPass2Response>) {
                Log.d("MiApp", "Respuesta : ${response.code()}")
                if (response.isSuccessful) {
                    val registerResponse = response.body()
                    if (registerResponse != null) {
                        if (registerResponse.respuestaHTTP == 0) {
                            //showToast("Registro exitoso")
                            guardarDatosCambioPass(registerResponse)
                            navigateToNext(correo)
                        } else {
                            handleErrorCode(registerResponse.respuestaHTTP)
                        }
                    } else {
                        //showToast("Error: Respuesta vacía del servidor")
                    }
                } else {
                    //showToast("Error en el verificar codigo: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<CambiarPass2Response>, t: Throwable) {
                //showToast("Error en la solicitud: ${t.message}")
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
        //showToast(message)
    }

    private fun guardarDatosCambioPass(registerResponse: CambiarPass2Response) {
        // Agregar logs para cada valor que se guarda
        Log.d("guardarDatosCambioPass", "Guardando datos cambio contraseña")

        // Guardar los valores utilizando la clase Preferencias
        Preferencias.guardarValorString("token", registerResponse.token_temporal ?: "")
        Log.d("guardarDatosCambioPass", "Token guardado: ${registerResponse.token_temporal ?: "null"}")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToNext(correo: String) {
        val intent = Intent(this, CambiarPassword3::class.java)
        intent.putExtra("correo", correo)  
        startActivity(intent)
        finish()
    }
}