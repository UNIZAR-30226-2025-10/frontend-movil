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
import com.example.myapplication.io.request.ValidarArtistaRequest
import com.example.myapplication.io.request.ValidarArtistaResponse
import com.example.myapplication.io.response.HistorialRecientesResponse
import com.example.myapplication.io.response.PendientesResponse
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Admin : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var recyclerSolicitudes: RecyclerView
    private lateinit var solicitudAdapter: SolicitudAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.solicitudes_admin)

        apiService = ApiService.create()
        recyclerSolicitudes = findViewById(R.id.recyclerSolicitudes)
        solicitudAdapter = SolicitudAdapter(
            solicitudes = emptyList(),
            onAceptarClick = { solicitud -> validarSolicitud(solicitud.correo, true) },
            onRechazarClick = { solicitud -> validarSolicitud(solicitud.correo, false) }
        )

        recyclerSolicitudes.layoutManager = LinearLayoutManager(this)
        recyclerSolicitudes.adapter = solicitudAdapter

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            startActivity(Intent(this, Logout::class.java))
        }

        loadSolicitudes()
    }

    private fun loadSolicitudes() {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        apiService.getPendientes(authHeader).enqueue(object : Callback<PendientesResponse> {
            override fun onResponse(call: Call<PendientesResponse>, response: Response<PendientesResponse>) {
                Log.d("ADMIN_RESPONES", "response: ${response.isSuccessful}")
                if (response.isSuccessful) {
                    response.body()?.let {
                            val solicitudesPendientes = it.pendientes
                            solicitudAdapter.updateDataSolicitudes(solicitudesPendientes)
                            recyclerSolicitudes.visibility = if (solicitudesPendientes.isNotEmpty()) View.VISIBLE else View.GONE
                    } ?: showToast("Búsqueda fallida: Datos incorrectos")
                } else {
                    Log.d("ADMIN_RESPONES", "ERROR BUSQUEDA: ${response.body()}")
                    showToast("Error en la búsqueda: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<PendientesResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun validarSolicitud(correo: String, esValida: Boolean) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        val requestBody = ValidarArtistaRequest(correo, esValida)

        apiService.validarArtista(authHeader, requestBody).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    showToast(if (esValida) "Solicitud aceptada" else "Solicitud rechazada")
                    loadSolicitudes()
                } else {
                    showToast("Error al procesar la solicitud: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("ADMIN_RESPONES", "ERROR BUSQUEDA: ${t.message}")
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
