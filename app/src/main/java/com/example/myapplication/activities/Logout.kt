package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.utils.Preferencias
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.ProgresoRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Logout : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private var yaRedirigidoAlLogin = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicialización de ApiService
        apiService = ApiService.create()

        val progreso = Preferencias.obtenerValorEntero("progresoCancionActual", -1)
        val cancionid = Preferencias.obtenerValorString("cancionActualId", "")
        val audio = Preferencias.obtenerValorString("audioCancionActual", "")
        if(progreso == -1 || cancionid == "" || audio == ""){
            logout()
        }
        else{
            guardarprogreso()
        }
    }

    // Método para hacer logout utilizando la API
    private fun logout() {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        Log.d("LOGOUT", "Iniciando logout...")
        Log.d("LOGOUT", "Token obtenido: $token")

        if (token.isEmpty()) {
            Log.e("LOGOUT", "Error: No se encontró un token guardado.")
            showToast("Error: No hay sesión iniciada.")
            return
        }

        // Cerrar la conexión WebSocket si está abierta
        Log.d("LOGOUT", "Cerrando WebSocket...")
        WebSocketManager.getInstance().closeWebSocket()

        // Llamada a la API para hacer logout
        apiService.logout(authHeader).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.d("LOGOUT", "Respuesta recibida del servidor. Código: ${response.code()}")

                if (response.isSuccessful) {
                    Log.d("LOGOUT", "Logout exitoso. Borrando datos de usuario...")
                    Preferencias.borrarDatosUsuario()
                    showToast("Logout exitoso")
                    Preferencias.guardarValorBooleano("primerinicio", false)
                    navigateInicio()
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@Logout, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@Logout, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("LOGOUT", "Error en el logout. Código HTTP: ${response.code()}")
                    val errorBody = response.errorBody()?.string()
                    Log.e("LOGOUT", "Cuerpo del error: $errorBody")
                    //showToast("Error en el logout: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("LOGOUT", "Fallo en la solicitud: ${t.message}")
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun guardarprogreso(){
        val progreso = Preferencias.obtenerValorEntero("progresoCancionActual", 0)/1000
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        val request = ProgresoRequest(progreso)
        Log.d("LOGOUT", "Iniciando guardar progreso...")
        Log.d("LOGOUT", "Token obtenido: $token")

        // Llamada a la API para hacer logout
        apiService.change_progreso(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.d("LOGOUT", "Respuesta recibida del servidor. Código: ${response.code()}")

                if (response.isSuccessful) {
                    Log.d("LOGOUT", "Entra en repsonse change progreso...")
                    Log.d("LOGOUT", "Progreso que se guarda: $progreso")
                    logout()
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        yaRedirigidoAlLogin = true
                        val intent = Intent(this@Logout, Inicio::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(this@Logout, "Sesión iniciada en otro dispositivo", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("LOGOUT", "Error en el logout. Código HTTP: ${response.code()}")
                    val errorBody = response.errorBody()?.string()
                    Log.e("LOGOUT", "Cuerpo del error: $errorBody")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("LOGOUT", "Fallo en la solicitud: ${t.message}")
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }


    private fun navigateInicio() {
        Log.d("LOGOUT", "Redirigiendo al inicio...")
        val intent = Intent(this, Inicio::class.java)
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
