package com.example.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.ActualizarFavoritoRequest
import com.example.myapplication.io.response.ActualizarFavoritoResponse
import com.example.myapplication.utils.Preferencias
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ActualizarFavorito : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private var esFavorito: Boolean = false
    private var cancionId: String? = null
    private var yaRedirigidoAlLogin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        apiService = ApiService.create()

        // Obtener datos del Intent (id de la canción y el estado del favorito)
        cancionId = intent.getStringExtra("cancion_id")
        esFavorito = intent.getBooleanExtra("es_favorito", false)

        // Actualizar estado del favorito en el backend
        cancionId?.let {
            actualizarFavorito(it, esFavorito)
        }
    }

    private fun actualizarFavorito(id: String, fav: Boolean) {
        val request = ActualizarFavoritoRequest(id, fav)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        // Llamar al servicio API para actualizar el estado del favorito
        apiService.actualizarFavorito(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ActualizarFavorito, "Estado de favorito actualizado", Toast.LENGTH_SHORT).show()
                    // Regresar a la pantalla anterior
                    val resultIntent = Intent()
                    resultIntent.putExtra("es_favorito", fav) // Devolver el nuevo estado del favorito
                    setResult(RESULT_OK, resultIntent)
                    finish()
                } else {
                    if (response.code() == 401 && !yaRedirigidoAlLogin) {
                        val errorBody = response.errorBody()?.string()

                        try {
                            val json = JSONObject(errorBody ?: "")
                            val errorMessage = json.getString("error")

                            if (errorMessage == "Token inválido.") {
                                yaRedirigidoAlLogin = true
                                val intent = Intent(this@ActualizarFavorito, Inicio::class.java)
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
                Toast.makeText(this@ActualizarFavorito, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
