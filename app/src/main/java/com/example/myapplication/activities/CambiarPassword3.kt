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
import com.example.myapplication.io.response.CambiarPass3Response
import com.example.myapplication.io.request.CambiarPass3Request
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CambiarPassword3 : AppCompatActivity() {

    private lateinit var editTextCode: EditText
    private lateinit var apiService: ApiService

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


            if (nuevaPass.isNotEmpty() ) {
                Log.d("MiApp2", "despue6")
                enviarNuevaPass(correo,nuevaPass)
                Log.d("MiApp2", "despues7")
            } else {
                showToast("Todos los campos son obligatorios")
            }
        }
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


                            //showToast("Registro exitoso")
                            navigateLogin()


                } else {
                    //showToast("Error en el verificar codigo: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
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
       // showToast(message)
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