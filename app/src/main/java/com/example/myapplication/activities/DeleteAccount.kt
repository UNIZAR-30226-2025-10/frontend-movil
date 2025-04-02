package com.example.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.DeleteAccountRequest
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DeleteAccount : AppCompatActivity() {

    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Delete", "Actividad DeleteAccount iniciada sin layout")

        apiService = ApiService.create()

        // Recuperar la contraseña del Intent
        val receivedPassword = intent.getStringExtra("password") ?: ""

        if (receivedPassword.isEmpty()) {
            Log.e("Delete", "Error: No se recibió una contraseña válida desde el perfil")
            showToast("Error: No se recibió una contraseña válida")
            finish() // Cerrar la actividad si no hay contraseña
            return
        }

        Log.d("Delete", "Contraseña recibida del Intent: $receivedPassword")

        // Iniciar el proceso de eliminación de cuenta
        borrarCuenta(receivedPassword)
    }

    private fun borrarCuenta(contrasenya: String) {
        Log.d("Delete", "Iniciando proceso de eliminación de cuenta")

        val deleteRequest = DeleteAccountRequest(contrasenya)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        Log.d("Delete", "Token obtenido: $token")

        apiService.deleteAccount(authHeader, deleteRequest).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.d("Delete", "Respuesta de la API recibida - Código: ${response.code()}")

                if (response.isSuccessful) {
                    Log.d("Delete", "Cuenta eliminada exitosamente")
                    Preferencias.borrarDatosUsuario()
                    showToast("Cuenta borrada con éxito")
                    navigateInicio()
                } else {
                    Log.e("Delete", "Error en la eliminación - Código: ${response.code()}")
                    showToast("Error en la eliminación: Código ${response.code()}")
                    finish()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("Delete", "Error en la solicitud: ${t.message}")
                showToast("Error en la solicitud: ${t.message}")
                finish()
            }
        })
    }

    private fun navigateInicio() {
        Log.d("Delete", "Navegando a la pantalla de inicio")
        val intent = Intent(this, Inicio::class.java)
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
