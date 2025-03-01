package com.example.myapplication.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.RegisterArtistRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
class RegisterArtist : AppCompatActivity(){
   /* private lateinit var editTextEmail: EditText
    private lateinit var buttonSubmit: Button
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registro_artista)

        // Inicializar ApiService
        apiService = ApiService.create()

        // Referenciar el campo de correo y el botón de enviar
        editTextEmail = findViewById(R.id.email)
        buttonSubmit = findViewById(R.id.submitButton)

        // Evento clic del botón para solicitar el código
        buttonSubmit.setOnClickListener {
            val email = editTextEmail.text.toString().trim()

            if (email.isNotEmpty()) {
                sendResetRequest(email)
            } else {
                showToast("Por favor, ingrese un correo electrónico")
            }
        }
    }

    private fun sendResetRequest(email: String) {
        val request = ForgotPasswordRequest(email)

        apiService.forgotPassword(request).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    showToast("Correo de recuperación enviado. Revisa tu bandeja de entrada.")
                    navigateToVerificationActivity(email)
                } else {
                    showToast("Error al enviar el correo")
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToVerificationActivity(email: String) {
        val intent = Intent(this, VerifyCodeActivity::class.java)
        intent.putExtra("email", email)
        startActivity(intent)
    }*/
}