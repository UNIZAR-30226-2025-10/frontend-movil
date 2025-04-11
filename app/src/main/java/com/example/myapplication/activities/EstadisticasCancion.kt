package com.example.myapplication.activities

import PublicasAdapter
import QuienLikeAdapter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.GetEstadisticasFavsResponse
import com.example.myapplication.io.response.GetEstadisticasPlaylistResponse
import com.example.myapplication.io.response.GetSignatureResponse
import com.example.myapplication.io.response.PersonasLike
import com.example.myapplication.io.response.Publicas
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class EstadisticasCancion : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var recycler: RecyclerView
    private lateinit var recycler2: RecyclerView
    private lateinit var nombreCancion: TextView
    private lateinit var nombreAlbum: TextView
    private lateinit var duracion: TextView
    private lateinit var reproducciones: TextView
    private lateinit var meGustas: TextView
    private lateinit var fecha: TextView
    private lateinit var fotoPortada: ImageView
    private lateinit var nPlaylists: TextView
    private lateinit var verMeGustas: TextView
    private lateinit var verPlaylists: TextView
    private lateinit var privadas: TextView
    private lateinit var btnEliminar: Button
    private var idCancion :String? = null
    private var quienLike: List<PersonasLike>? = null
    private var playlistsPublicas: List<Publicas>? = null
    private var verMegustasOpen: Boolean = false
    private var verPlaylistsOpen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.estadisticas_cancion)

        apiService = ApiService.create()
        recycler = findViewById<RecyclerView>(R.id.recyclerHorizontal)
        recycler.layoutManager = LinearLayoutManager(this@EstadisticasCancion, LinearLayoutManager.HORIZONTAL, false)

        recycler2 = findViewById<RecyclerView>(R.id.recyclerHorizontal2)
        recycler2.layoutManager = LinearLayoutManager(this@EstadisticasCancion, LinearLayoutManager.HORIZONTAL, false)

        // Vincular vistas
        nombreCancion = findViewById(R.id.nombreCancion)
        nombreAlbum = findViewById(R.id.nombreAlbum)
        duracion = findViewById(R.id.duracion)
        reproducciones = findViewById(R.id.repros)
        meGustas = findViewById(R.id.me_gustas)
        fecha = findViewById(R.id.fecha)
        fotoPortada = findViewById(R.id.centerImage)
        nPlaylists = findViewById(R.id.playlists)
        verMeGustas = findViewById(R.id.ver_me_gustas)
        verPlaylists = findViewById(R.id.ver_playlists)
        btnEliminar = findViewById(R.id.firstButton)
        privadas = findViewById(R.id.playlists_privadas)

        // Obtener datos del intent
        idCancion = intent.getStringExtra("id")
        val nombre = intent.getStringExtra("nombre")
        val album = intent.getStringExtra("album")
        val duracionSegundos = intent.getIntExtra("duracion", 0)
        val reproduccionesCount = intent.getIntExtra("reproducciones", 0)
        val meGustasCount = intent.getIntExtra("meGustas", 0)
        val fechaPublicacion = intent.getStringExtra("fecha")
        val fotoUrl = intent.getStringExtra("foto")
        val playlistsCount = intent.getIntExtra("nPlaylists", 0)

        nombreCancion.text = nombre
        nombreAlbum.text = "De $album"
        duracion.text = formatearDuracion(duracionSegundos)
        meGustas.text = "$meGustasCount Me Gustas"
        nPlaylists.text = "$playlistsCount Playlists"

        val fechaFormateada = formatearFecha(fechaPublicacion!!)
        fecha.text = fechaFormateada

        val format = NumberFormat.getInstance(Locale("es", "ES"))
        val formattedRepros = format.format(reproduccionesCount)
        reproducciones.text = "$formattedRepros Reproducciones"


        Glide.with(this)
            .load(fotoUrl)
            .into(fotoPortada)

        if (playlistsCount != 0) {
            verPlaylists.visibility = View.VISIBLE
        }

        if (meGustasCount != 0) {
            verMeGustas.visibility = View.VISIBLE
        }

        verPlaylists.setOnClickListener {
            if (verPlaylistsOpen) {
                verPlaylistsOpen = false
                recycler2.visibility = View.GONE
                privadas.visibility = View.GONE
            } else {
                obtenerPlaylists()
                verPlaylistsOpen = true
                recycler2.visibility = View.VISIBLE
                privadas.visibility = View.VISIBLE
            }
        }

        verMeGustas.setOnClickListener {
            if (verMegustasOpen) {
                verMegustasOpen = false
                recycler.visibility = View.GONE
            } else {
                obtenerQuienLeGusta()
                verMegustasOpen = true
                recycler.visibility = View.VISIBLE
            }
        }

        btnEliminar.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("¿Está seguro de que desea eliminar esta canción?")
            builder.setMessage("Al hacerlo, desaparecerá completamente del sistema. Se perderán todas sus reproducciones, 'Me gusta' y cualquier playlist en la que haya sido añadida. Esta acción es irreversible.")

            builder.setPositiveButton("Eliminar") { dialog, _ ->
                val token = Preferencias.obtenerValorString("token", "")
                val authHeader = "Bearer $token"

                apiService.deleteCancion(authHeader, idCancion!!).enqueue(object :
                    Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@EstadisticasCancion, "Canción eliminada con éxito", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@EstadisticasCancion, "Error al eliminar la canción", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.d("Eliminar canción", "Error en la solicitud: ${t.message}")
                        Toast.makeText(this@EstadisticasCancion, "Error de red al eliminar", Toast.LENGTH_SHORT).show()
                    }
                })

                dialog.dismiss()
            }

            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun formatearDuracion(segundos: Int): String {
        val minutos = segundos / 60
        val segundosRestantes = segundos % 60
        return "${minutos} minutos ${segundosRestantes} segundos"
    }

    fun formatearFecha(fechaIso: String): String {
        val fecha = LocalDate.parse(fechaIso)
        val formatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        return fecha.format(formatter)
    }

    private fun obtenerQuienLeGusta() {
        if (quienLike == null) {
            val token = Preferencias.obtenerValorString("token", "")
            val authHeader = "Bearer $token"

            apiService.getEstadisticasFavs(authHeader, idCancion!!).enqueue(object :
                Callback<GetEstadisticasFavsResponse> {
                override fun onResponse(call: Call<GetEstadisticasFavsResponse>, response: Response<GetEstadisticasFavsResponse>) {
                    if (response.isSuccessful) {
                        val respuesta = response.body()
                        respuesta?.let {
                            if (respuesta?.oyentes_favs != null) {
                                quienLike = respuesta.oyentes_favs


                                val adapter = QuienLikeAdapter(quienLike!!)
                                recycler.adapter = adapter
                            } else {
                                Log.e("API", "La lista quienLike es null")
                            }
                        }
                    }
                }
                override fun onFailure(call: Call<GetEstadisticasFavsResponse>, t: Throwable) {
                    Log.d("Estadisticas Favs", "Error en la solicitud: ${t.message}")
                }
            })
        }
    }

    private fun obtenerPlaylists() {
        if (quienLike == null) {
            val token = Preferencias.obtenerValorString("token", "")
            val authHeader = "Bearer $token"

            apiService.getEstadisticasPlaylists(authHeader, idCancion!!).enqueue(object :
                Callback<GetEstadisticasPlaylistResponse> {
                override fun onResponse(call: Call<GetEstadisticasPlaylistResponse>, response: Response<GetEstadisticasPlaylistResponse>) {
                    if (response.isSuccessful) {
                        val respuesta = response.body()
                        respuesta?.let {
                            if (respuesta?.playlists_publicas != null) {
                                playlistsPublicas = respuesta.playlists_publicas
                                privadas.text = "+ " + respuesta.n_privadas.toString() + " playlists privadas"

                                val adapter = PublicasAdapter(playlistsPublicas!!)
                                recycler2.adapter = adapter
                            } else {
                                Log.e("API", "La lista playlistsPublicas es null")
                            }
                        }
                    }
                }
                override fun onFailure(call: Call<GetEstadisticasPlaylistResponse>, t: Throwable) {
                    Log.d("Estadisticas playlists", "Error en la solicitud: ${t.message}")
                }
            })
        }
    }
}
