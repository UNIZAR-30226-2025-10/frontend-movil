package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.utils.Preferencias
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.LogOutResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Logout : AppCompatActivity() {

    private lateinit var apiService: ApiService

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicialización de ApiService
        apiService = ApiService.create()

        logout()
    }

    // Método para hacer logout utilizando la API
    private fun logout() {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        // Cerrar la conexión WebSocket si está abierta
        WebSocketManager.getInstance().closeWebSocket()

        // Llamada a la API para hacer logout
        apiService.logout(authHeader).enqueue(object : Callback<LogOutResponse> {
            override fun onResponse(call: Call<LogOutResponse>, response: Response<LogOutResponse>) {
                if (response.isSuccessful) {
                    val logoutResponse = response.body()
                    if (logoutResponse != null) {
                        Log.d("MiApp", "Respuesta exitosa: ${logoutResponse}")
                        if (logoutResponse.respuestaHTTP == 0) {
                            Preferencias.borrarDatosUsuario() // Limpiar preferencias
                            showToast("Logout exitoso")
                            navigateInicio() // Redirigir al inicio
                        } else {
                            handleErrorCode(logoutResponse.respuestaHTTP)
                        }
                    } else {
                        showToast("Logout fallido: Datos incorrectos")
                    }
                } else {
                    showToast("Error en el logout: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<LogOutResponse>, t: Throwable) {
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

    private fun navigateInicio() {
        val intent = Intent(this, Inicio::class.java)
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}