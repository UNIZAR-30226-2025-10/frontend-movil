package com.example.myapplication.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.Adapters.EstadisticasAlbum.CancionEstAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.CancionEst
import com.example.myapplication.io.response.EstadisticasAlbumResponse
import com.example.myapplication.io.response.MiAlbum
import com.example.myapplication.utils.Preferencias
import retrofit2.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.text.NumberFormat



class EstadisticasAlbum : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var text1: TextView
    private lateinit var text2: TextView
    private lateinit var nombreAlbum: TextView
    private lateinit var nCanciones: TextView
    private lateinit var nMegustas: TextView
    private lateinit var nRepros: TextView
    private lateinit var apiService: ApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CancionEstAdapter
    private var idAlbum: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.estadisticas_album)

        idAlbum = intent.getStringExtra("id")
        apiService = ApiService.create()

        imageView = findViewById(R.id.centerImage)
        text1 = findViewById(R.id.text1)
        text2 = findViewById(R.id.text2)
        nombreAlbum = findViewById(R.id.nombreAlbum)
        nCanciones = findViewById(R.id.num_canciones)
        nMegustas = findViewById(R.id.me_gustas)
        nRepros = findViewById(R.id.reproducciones)

        recyclerView = findViewById(R.id.recyclerCanciones)
        recyclerView.layoutManager = LinearLayoutManager(this)

        datosAlbum()
    }

    fun formatearDuracion(segundos: Int): String {
        val minutos = segundos / 60
        val segundosRestantes = segundos % 60
        return "${minutos} minutos ${segundosRestantes} segundos"
    }

    fun formatearFecha(fechaIso: String): String {
        val fecha = LocalDate.parse(fechaIso)
        val formatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        return fecha.format(formatter)
    }

    private fun datosAlbum() {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        if (idAlbum != null) {
            val id = idAlbum as String
            apiService.getEstadisticasAlbum(authHeader, id)
                .enqueue(object : Callback<EstadisticasAlbumResponse> {
                    override fun onResponse(
                        call: Call<EstadisticasAlbumResponse>,
                        response: Response<EstadisticasAlbumResponse>
                    ) {
                        if (response.isSuccessful) {
                            val stats = response.body()
                            stats?.let {
                                val fechaFormateada = formatearFecha(stats.fechaPublicacion).replaceFirstChar { it.uppercase() }
                                text1.text = formatearDuracion(stats.duracion)
                                text2.text = fechaFormateada
                                nombreAlbum.text = stats.nombre

                                val num = "${stats.canciones.size} canciones"
                                nCanciones.text = num

                                val meGustas = "${stats.favs} Me Gustas"
                                nMegustas.text = meGustas

                                val format = NumberFormat.getInstance(Locale("es", "ES"))
                                val formattedRepros = format.format(stats.reproducciones)
                                val repros = "$formattedRepros Reproducciones"
                                nRepros.text = repros


                                Glide.with(this@EstadisticasAlbum)
                                    .load(it.fotoPortada)
                                    .into(imageView)

                                adapter = CancionEstAdapter(stats.canciones, stats.nombreArtisticoArtista,  object : CancionEstAdapter.OnEstadisticasClickListener{
                                    override fun onVerEstadisticasClick(cancion: CancionEst) {
                                        Toast.makeText(this@EstadisticasAlbum, "Ver estadísticas", Toast.LENGTH_SHORT).show()
                                    }
                                })
                                recyclerView.adapter = adapter
                            }
                        }
                    }

                    override fun onFailure(call: Call<EstadisticasAlbumResponse>, t: Throwable) {
                        Log.d("Pedir estadisticas álbum", "Error en la solicitud: ${t.message}")
                    }
                })
        }
    }
}
