package com.example.myapplication.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.Adapters.Home.EscuchasAdapter
import com.example.myapplication.Adapters.Home.HeaderAdapter
import com.example.myapplication.Adapters.Home.PlaylistsAdapter
import com.example.myapplication.Adapters.Home.RecientesAdapter
import com.example.myapplication.Adapters.Home.RecomendacionesAdapter
import com.example.myapplication.Adapters.SolicitudesAdmin.SolicitudAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.HistorialRecientesResponse
import com.example.myapplication.io.response.PendientesResponse
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Admin : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var recyclerSolicitudes: RecyclerView
    private lateinit var SolicitudAdapter: SolicitudAdapter



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MiApp", "admin 1")
        setContentView(R.layout.solicitudes_admin)

        apiService = ApiService.create()

        findViewById<Button>(R.id.btnLogout).setOnClickListener { startActivity(Intent(this, Logout::class.java)) }
        Log.d("MiApp", "admin 2")
        loadSolicitudes()

    }


    private fun loadSolicitudes() {
        val token = Preferencias.obtenerValorString("token", "")
        apiService.getPendientes("Bearer $token").enqueue(object :
            Callback<PendientesResponse> {
            override fun onResponse(call: Call<PendientesResponse>, response: Response<PendientesResponse>) {
                Log.d("MiApp", "entra en on response pendientes")
                if (response.isSuccessful) {
                    Log.d("MiApp", "entra en on response succesful pendientes")
                    response.body()?.let {
                        if (it.respuestaHTTP == 0) {
                            Log.d("MiApp", "entra en respuesta http pendientes")
                            val solicitudesPendientes = it.pendientes
                            Log.d("MiApp", "pendientes = $solicitudesPendientes")

                            // Actualizar y mostrar las canciones si las hay
                            if (solicitudesPendientes.isNotEmpty()) {
                                Log.d("MiApp", "no esta vacía")
                                SolicitudAdapter.updateDataSolicitudes(solicitudesPendientes)
                                recyclerSolicitudes.visibility = View.VISIBLE
                            } else {
                                Log.d("MiApp", "no hay pendientes")
                                recyclerSolicitudes.visibility = View.GONE
                                showToast("No hay pendientes")
                            }

                        } else {
                            handleErrorCode(it.respuestaHTTP)
                        }
                    } ?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    showToast("Error en la búsqueda: Código ${response.code()}")
                }
                Log.d("MiApp", "sale de recientes")
            }

            override fun onFailure(call: Call<PendientesResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
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
}
