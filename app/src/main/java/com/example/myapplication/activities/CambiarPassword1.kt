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
import com.example.myapplication.io.response.CambiarPass1Response
import com.example.myapplication.io.request.CambiarPass1Request
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CambiarPassword1 : AppCompatActivity() {

    private lateinit var editTextCode: EditText
    private lateinit var apiService: ApiService
    private var yaRedirigidoAlLogin = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cambiar_contrasegna)

        // Inicializar ApiService
        apiService = ApiService.create()

        // Referenciar los EditText desde el layout
        editTextCode = findViewById(R.id.etEmail)

        // Referenciar el botón de registro
        val buttonSiguiente: Button = findViewById(R.id.nextButton)

        // Evento clic del botón de registro
        buttonSiguiente.setOnClickListener {
            val correo = editTextCode.text.toString().trim()


            if (correo.isNotEmpty() ) {
                enviarcorreo(correo)
            } else {
                showToast("Todos los campos son obligatorios")
            }
        }
    }

    private fun enviarcorreo(correo: String) {
        val request = CambiarPass1Request(correo)
        apiService.CambiarPass1(request).enqueue(object : Callback<Void> {

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.d("MiApp", "Respuesta : ${response.code()}")
                if (response.isSuccessful) {

                    navigateToNext(correo)

                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@CambiarPassword1, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        showToast("Sesión iniciada en otro dispositivo")
                    }
                    val errorBody = response.errorBody()?.string()
                    try {
                        val json = JSONObject(errorBody)
                        val errorMessage = json.getString("error")
                        Toast.makeText(this@CambiarPassword1, errorMessage, Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@CambiarPassword1, "Error desconocido.", Toast.LENGTH_LONG).show()
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
        showToast(message)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToNext(correo: String) {
        val intent = Intent(this, CambiarPassword2::class.java)
        intent.putExtra("correo", correo)
        startActivity(intent)
        finish()
    }

}