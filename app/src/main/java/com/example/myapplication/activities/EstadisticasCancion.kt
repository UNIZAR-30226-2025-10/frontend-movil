package com.example.myapplication.activities

import PublicasAdapter
import QuienLikeAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
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
import com.example.myapplication.io.request.AudioColeccionRequest
import com.example.myapplication.io.response.AddReproduccionResponse
import com.example.myapplication.io.response.AudioResponse
import com.example.myapplication.io.response.CancionInfoResponse
import com.example.myapplication.io.response.GetEstadisticasFavsResponse
import com.example.myapplication.io.response.GetEstadisticasPlaylistResponse
import com.example.myapplication.io.response.GetSignatureResponse
import com.example.myapplication.io.response.PersonasLike
import com.example.myapplication.io.response.Publicas
import com.example.myapplication.services.MusicPlayerService
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

    private var musicService: MusicPlayerService? = null
    private var serviceBound = false

    private var indexActual = 0
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.MusicBinder
            musicService = binder.getService()
            serviceBound = true
            handler.post(updateRunnable)
            //actualizarIconoPlayPause()
            MusicPlayerService.setOnCompletionListener {
                runOnUiThread {
                    val idcoleccion = Preferencias.obtenerValorString("coleccionActualId", "")
                    if(idcoleccion == ""){
                        Preferencias.guardarValorEntero("progresoCancionActual", 0)
                        musicService?.resume()
                    }
                    else {
                        Log.d("Reproducción", "Canción finalizada, pasando a la siguiente")
                        indexActual++
                        Preferencias.guardarValorEntero("indexColeccionActual", indexActual)
                        Preferencias.guardarValorEntero("progresoCancionActual", 0)
                        reproducirColeccion()
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
        }
    }
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            //updateProgressBar()
            handler.postDelayed(this, 1000) // cada segundo
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.estadisticas_cancion)

        apiService = ApiService.create()
        recycler = findViewById<RecyclerView>(R.id.recyclerHorizontal)
        recycler.layoutManager = LinearLayoutManager(this@EstadisticasCancion, LinearLayoutManager.HORIZONTAL, false)

        indexActual = Preferencias.obtenerValorEntero("indexColeccionActual", 0)

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

    private fun reproducirColeccion() {
        val ordenColeccion = Preferencias.obtenerValorString("ordenColeccionActual", "")
            .split(",")
            .filter { id -> id.isNotEmpty() }

        val modoColeccion =  Preferencias.obtenerValorString("modoColeccionActual", "")

        val indice = Preferencias.obtenerValorEntero("indexColeccionActual", 0)

        if (indice >= ordenColeccion.size) {
            Log.d("Reproducción", "Fin de la playlist")
            return
        }

        val idcoleccion = Preferencias.obtenerValorString("coleccionActualId", "")

        val listaNatural = Preferencias.obtenerValorString("ordenNaturalColeccionActual", "")
            .split(",")
            .filter { id -> id.isNotEmpty() }

        Log.d("ReproducirPlaylist", "Lista natural ids: ${listaNatural.joinToString(",")}")
        Log.d("ReproducirPlaylist", "Lista ids reproduccion: ${ordenColeccion.joinToString(",")}")
        Log.d("ReproducirPlaylist", "Indice: $indice")
        Log.d("ReproducirPlaylist", "Modo: $modoColeccion")
        Log.d("ReproducirPlaylist", "Id coleccion: $idcoleccion")

        val request = AudioColeccionRequest(idcoleccion, modoColeccion, ordenColeccion, indice)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        val sid = WebSocketManager.getInstance().getSid() ?: run {
            Log.e("WebSocket", "SID no disponible")
            return
        }

        apiService.reproducirColeccion(authHeader, sid, request).enqueue(object : Callback<AudioResponse> {
            override fun onResponse(call: Call<AudioResponse>, response: Response<AudioResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { audioResponse ->
                        reproducirAudioColeccion(audioResponse.audio) // No enviar progreso
                        notificarReproduccion()
                        guardarDatoscCancion(ordenColeccion[indice])
                    }
                } else {
                    Log.e("API", "Error: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<AudioResponse>, t: Throwable) {
                Log.e("API", "Fallo: ${t.message}")
            }
        })
    }

    private fun reproducirAudio(audioUrl: String, progreso: Int = 0) {
        try {
            Preferencias.guardarValorEntero("progresoCancionActual", progreso)
            val startIntent = Intent(this, MusicPlayerService::class.java).apply {
                action = "PLAY"
                putExtra("url", audioUrl)
                putExtra("progreso", progreso)
            }
            startService(startIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al reproducir el audio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reproducirAudioColeccion(audioUrl: String, progreso: Int = 0) {
        try {
            Preferencias.guardarValorEntero("progresoCancionActual", progreso)
            val intent = Intent(this, MusicPlayerService::class.java).apply {
                action = "PLAY"
                putExtra("url", audioUrl)
                putExtra("progreso", progreso)
            }
            startService(intent)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al reproducir el audio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun notificarReproduccion() {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.addReproduccion(authHeader).enqueue(object : Callback<AddReproduccionResponse> {
            override fun onResponse(call: Call<AddReproduccionResponse>, response: Response<AddReproduccionResponse>) {
                if (response.isSuccessful) {
                    Log.d("MiApp", "Reproducción registrada exitosamente")
                } else {
                    Log.e("MiApp", "Error al registrar la reproducción")
                }
            }

            override fun onFailure(call: Call<AddReproduccionResponse>, t: Throwable) {
                Log.e("MiApp", "Error de conexión al registrar reproducción")
            }
        })
    }

    private fun guardarDatoscCancion(id: String) {
        Preferencias.guardarValorString("cancionActualId", id)

        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        // Llamada a la API con el sid en los headers
        apiService.getInfoCancion(authHeader, id).enqueue(object : Callback<CancionInfoResponse> {
            override fun onResponse(call: Call<CancionInfoResponse>, response: Response<CancionInfoResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { cancionResponse ->
                        val foto = cancionResponse.fotoPortada
                        val nombre = cancionResponse.nombre
                        val artista = cancionResponse.nombreArtisticoArtista

                        // Mostrar en Logcat
                        Log.d("CancionInfo", "Respuesta exitosa Canción")
                        Log.d("CancionInfo", "Canción: $nombre")
                        Log.d("CancionInfo", "Artista: $artista")
                        Log.d("CancionInfo", "Foto: $foto")

                        Preferencias.guardarValorString("nombreCancionActual", nombre)
                        Preferencias.guardarValorString("nombreArtisticoActual", artista)
                        Preferencias.guardarValorString("fotoPortadaActual", foto)

                    }
                } else {
                    val errorMensaje = response.errorBody()?.string() ?: "Error desconocido"

                    // Mostrar en Logcat
                    Log.e("API_RESPONSE", "Error en la respuesta: Código ${response.code()} - $errorMensaje")

                    // Mostrar en Toast
                    Toast.makeText(this@EstadisticasCancion, "Error: $errorMensaje", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<CancionInfoResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)

                // Mostrar en Toast
                Toast.makeText(this@EstadisticasCancion, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, MusicPlayerService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }
}
