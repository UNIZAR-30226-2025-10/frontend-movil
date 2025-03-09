package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.utils.Preferencias
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.DeleteAccountRequest
import com.example.myapplication.io.response.DeleteAccountResponse
import com.example.myapplication.io.response.LogOutResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DeleteAccount : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var editTextPassword: EditText

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.borrar_cuenta)

        // Inicialización de ApiService
        apiService = ApiService.create()

        // Referenciar los botones
        val buttonDeleteAccount: Button = findViewById(R.id.botonDeleteAccount)
        editTextPassword = findViewById(R.id.contrasenya)

        val contrasenya = editTextPassword.text.toString().trim()

        // Evento clic del botón de delete account
        buttonDeleteAccount.setOnClickListener {
            borrarCuenta(contrasenya)
        }
    }

    // Método para eliminar la cuenta
    private fun borrarCuenta(contrasenya: String) {

        val deleteRequest: DeleteAccountRequest
        deleteRequest = DeleteAccountRequest(contrasenya)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        // Llamada a la API para hacer logout
        apiService.deleteAccount(authHeader,deleteRequest).enqueue(object : Callback<DeleteAccountResponse> {
            override fun onResponse(call: Call<DeleteAccountResponse>, response: Response<DeleteAccountResponse>) {
                if (response.isSuccessful) {
                    val deleteResponse = response.body()
                    if (deleteResponse != null) {
                        Log.d("MiApp", "Respuesta exitosa: ${deleteResponse}")
                        if(deleteResponse.respuestaHTTP == 0){
                            // Borrar los datos guardados en preferencias
                            Preferencias.borrarDatosUsuario() 
                            showToast("Cuenta borrada con existoso")
                            navigateInicio()
                        } else{
                            handleErrorCode(deleteResponse.respuestaHTTP)
                        }
                    } else {
                        showToast("Delete account fallido: datos incorrectos")
                    }
                } else {
                    showToast("Error en el delete account: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<DeleteAccountResponse>, t: Throwable) {
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