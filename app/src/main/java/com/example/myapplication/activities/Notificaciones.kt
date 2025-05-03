package com.example.myapplication.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.Adapters.Notificaciones.InteraccionesAdapter
import com.example.myapplication.Adapters.Notificaciones.InvitacionesAdapter
import com.example.myapplication.Adapters.Notificaciones.NovedadesAdapter
import com.example.myapplication.Adapters.Notificaciones.SeguidoresAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.AceptarInvitacionRequest
import com.example.myapplication.io.request.AudioColeccionRequest
import com.example.myapplication.io.request.AudioRequest
import com.example.myapplication.io.request.DeleteNotiAlbumRequest
import com.example.myapplication.io.request.DeleteNotiCancionRequest
import com.example.myapplication.io.request.LeerNotiLikeRequest
import com.example.myapplication.io.request.LeerNotiNoizzitoRequest
import com.example.myapplication.io.request.LeerNotiSeguidorRequest
import com.example.myapplication.io.request.VerInteraccionRequest
import com.example.myapplication.io.response.AddReproduccionResponse
import com.example.myapplication.io.response.AudioResponse
import com.example.myapplication.io.response.CancionInfoResponse
import com.example.myapplication.io.response.GetInteraccionesResponse
import com.example.myapplication.io.response.GetInvitacionesResponse
import com.example.myapplication.io.response.GetNovedadesResponse
import com.example.myapplication.io.response.GetNuevosSeguidoresResponse
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.InvitacionPlaylist
import com.example.myapplication.io.response.Novedad
import com.example.myapplication.io.response.Seguidor
import com.example.myapplication.services.MusicPlayerService
import com.example.myapplication.services.WebSocketEventHandler
import com.example.myapplication.utils.Preferencias
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Notificaciones : AppCompatActivity() {

    private val listenerSeguidor: (Seguidor) -> Unit = { seguidor ->
        runOnUiThread {
            Log.d("LOGS_NOTIS", "en evento noti")
            if (botonActivo != "seguidores") {
                dotSeguidores.visibility = View.VISIBLE
                dotNotificacion.visibility = View.VISIBLE
            } else {
                Preferencias.guardarValorBooleano("hay_notificaciones_seguidores", false)
                Preferencias.guardarValorBooleano("hay_notificaciones", false)
            }

            val adapter = recyclerSeguidores.adapter as? SeguidoresAdapter
            adapter?.agregarSeguidor(seguidor)
            numSeguidores++
            noHaySeguidores.visibility = View.GONE
        }
    }

    private val listenerNovedad: (Novedad) -> Unit = { novedad ->
        runOnUiThread {
            if (botonActivo != "novedades") {
                dotNovedades.visibility = View.VISIBLE
                dotNotificacion.visibility = View.VISIBLE
            }

            val adapter = recyclerNovedades.adapter as? NovedadesAdapter
            adapter?.agregarNovedad(novedad)
            numNovedades++
            noHayNovedades.visibility = View.GONE
        }
    }

    private val listenerInteraccion: (Interaccion) -> Unit = { interaccion ->
        runOnUiThread {
            if (botonActivo != "interacciones") {
                dotInteracciones.visibility = View.VISIBLE
                dotNotificacion.visibility = View.VISIBLE
            }

            val adapter = recyclerInteracciones.adapter as? InteraccionesAdapter
            adapter?.agregarInteraccion(interaccion)
            numInteracciones++
            noHayInteracciones.visibility = View.GONE
        }
    }

    private val listenerInvitacion: (InvitacionPlaylist) -> Unit = { invitacion ->
        runOnUiThread {
            if (botonActivo != "invitaciones") {
                dotInvitaciones.visibility = View.VISIBLE
                dotNotificacion.visibility = View.VISIBLE
            }

            val adapter = recyclerInvitaciones.adapter as? InvitacionesAdapter
            adapter?.agregarInvitacion(invitacion)
            numInvitaciones++
            noHayInvitaciones.visibility = View.GONE
        }
    }

    private lateinit var apiService: ApiService
    private lateinit var recyclerInvitaciones: RecyclerView
    private lateinit var recyclerInteracciones: RecyclerView
    private lateinit var recyclerNovedades: RecyclerView
    private lateinit var recyclerSeguidores: RecyclerView
    private lateinit var dotNotificacion: View
    private lateinit var dotInvitaciones: View
    private lateinit var dotNovedades: View
    private lateinit var dotInteracciones: View
    private lateinit var dotSeguidores: View
    private lateinit var noHayInvitaciones: TextView
    private lateinit var noHayNovedades: TextView
    private lateinit var noHayInteracciones: TextView
    private lateinit var noHaySeguidores: TextView

    private var botonActivo: String = "invitaciones"
    private var numInvitaciones: Int = 0
    private var numNovedades: Int = 0
    private var numInteracciones: Int = 0
    private var numSeguidores: Int = 0

    private lateinit var progressBar: ProgressBar
    private var musicService: MusicPlayerService? = null
    private var serviceBound = false
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateProgressBar()
            handler.postDelayed(this, 1000) // cada segundo
        }
    }
    private var indexActual = 0
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.MusicBinder
            musicService = binder.getService()
            serviceBound = true
            handler.post(updateRunnable)
            // El servicio ya está listo, ahora actualiza el mini reproductor
            updateMiniReproductor()
            actualizarIconoPlayPause()
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
            handler.removeCallbacks(updateRunnable)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notificaciones)

        apiService = ApiService.create()

        val btnInvitaciones = findViewById<LinearLayout>(R.id.btnInvitaciones)
        val btnInteracciones = findViewById<LinearLayout>(R.id.btnInteracciones)
        val btnNovedades = findViewById<LinearLayout>(R.id.btnNovedades)
        val btnSeguidores = findViewById<LinearLayout>(R.id.btnSeguidores)

        indexActual = Preferencias.obtenerValorEntero("indexColeccionActual", 0)

        noHayInvitaciones = findViewById(R.id.noHayInvitaciones)
        noHayNovedades = findViewById(R.id.noHayNovedades)
        noHayInteracciones = findViewById(R.id.noHayInteracciones)
        noHaySeguidores = findViewById(R.id.noHaySeguidores)

        //Views de los puntitos de notificación
        dotInvitaciones = findViewById(R.id.dotInvitaciones)
        dotInteracciones = findViewById(R.id.dotInteracciones)
        dotNovedades = findViewById(R.id.dotNovedades)
        dotSeguidores = findViewById(R.id.dotSeguidores)
        dotNotificacion = findViewById(R.id.notificationDot)

        recyclerInvitaciones = findViewById(R.id.recyclerViewInvitaciones)
        recyclerInteracciones = findViewById(R.id.recyclerViewInteracciones)
        recyclerNovedades = findViewById(R.id.recyclerViewNovedades)
        recyclerSeguidores = findViewById(R.id.recyclerViewSeguidores)

        recyclerInvitaciones.layoutManager = LinearLayoutManager(this)
        recyclerNovedades.layoutManager = LinearLayoutManager(this)
        recyclerInteracciones.layoutManager = LinearLayoutManager(this)
        recyclerSeguidores.layoutManager = LinearLayoutManager(this)

        progressBar = findViewById(R.id.progressBar)
        setupNavigation()
        updateMiniReproductor()


        val profileImageButton = findViewById<ImageButton>(R.id.profileImageButton)
        val profileImageUrl = Preferencias.obtenerValorString("fotoPerfil", "")

        if (profileImageUrl.isNullOrEmpty() || profileImageUrl == "DEFAULT") {
            profileImageButton.setImageResource(R.drawable.ic_profile)
        } else {
            Glide.with(this)
                .load(profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(profileImageButton)
        }

        //PARA ACTUALIZAR EL PUNTITO DE NOTIFICACIONES AL ENTRAR EN LA PANTALLA
        if (Preferencias.obtenerValorBooleano("hay_notificaciones",false) == true) {
            dotNotificacion.visibility = View.VISIBLE
        } else {
            dotNotificacion.visibility = View.GONE
        }

        if (Preferencias.obtenerValorBooleano("hay_notificaciones_invitaciones",false) == true) {
            //no poner el punto al entrar porque como se entra en esa pantalla ya estan vistas
            //dotInvitaciones.visibility = View.VISIBLE
        } else {
            dotInvitaciones.visibility = View.GONE
        }

        if (Preferencias.obtenerValorBooleano("hay_notificaciones_novedades",false) == true) {
            dotNovedades.visibility = View.VISIBLE
        } else {
            dotNovedades.visibility = View.GONE
        }

        if (Preferencias.obtenerValorBooleano("hay_notificaciones_interacciones",false) == true) {
            dotInteracciones.visibility = View.VISIBLE
        } else {
            dotInteracciones.visibility = View.GONE
        }

        if (Preferencias.obtenerValorBooleano("hay_notificaciones_seguidores",false) == true) {
            dotSeguidores.visibility = View.VISIBLE
        } else {
            dotSeguidores.visibility = View.GONE
        }



        cogerInvitaciones()
        cogerNovedades()
        cogerInteracciones()
        cogerNuevosSeguidores()

        val botones = listOf(
            btnInvitaciones to recyclerInvitaciones,
            btnInteracciones to recyclerInteracciones,
            btnNovedades to recyclerNovedades,
            btnSeguidores to recyclerSeguidores
        )

        fun seleccionarBoton(botonActivo: LinearLayout, recyclerActivo: RecyclerView) {
            botones.forEach { (boton, recycler) ->
                val textView = boton.getChildAt(0) as TextView
                if (boton == botonActivo) {
                    boton.setBackgroundResource(R.drawable.button_notificaciones_selected)
                    textView.setTextColor(Color.WHITE)
                    recycler.visibility = View.VISIBLE
                } else {
                    boton.setBackgroundResource(R.drawable.button_notificaciones)
                    textView.setTextColor(Color.BLACK)
                    recycler.visibility = View.GONE
                }
            }
        }

        botones.forEach { (boton, recycler) ->
            boton.setOnClickListener {
                seleccionarBoton(boton, recycler)

                when (boton.id) {
                    R.id.btnInvitaciones -> {
                        botonActivo = "invitaciones"
                        dotInvitaciones.visibility = View.GONE
                        Preferencias.guardarValorBooleano("hay_notificaciones_invitaciones", false)
                        noHayInteracciones.visibility = View.GONE
                        noHayNovedades.visibility = View.GONE
                        noHaySeguidores.visibility = View.GONE
                        if (numInvitaciones == 0) {
                            noHayInvitaciones.visibility = View.VISIBLE
                        }
                    }
                    R.id.btnInteracciones -> {
                        botonActivo = "interacciones"
                        dotInteracciones.visibility = View.GONE
                        Preferencias.guardarValorBooleano("hay_notificaciones_interacciones", false)
                        noHayInvitaciones.visibility = View.GONE
                        noHayNovedades.visibility = View.GONE
                        noHaySeguidores.visibility = View.GONE
                        if (numInteracciones == 0) {
                            noHayInteracciones.visibility = View.VISIBLE
                        }
                    }
                    R.id.btnNovedades -> {
                        botonActivo = "novedades"
                        dotNovedades.visibility = View.GONE
                        Preferencias.guardarValorBooleano("hay_notificaciones_novedades", false)
                        noHayInteracciones.visibility = View.GONE
                        noHayInvitaciones.visibility = View.GONE
                        noHaySeguidores.visibility = View.GONE
                        if (numNovedades == 0) {
                            noHayNovedades.visibility = View.VISIBLE
                        }
                    }
                    R.id.btnSeguidores -> {
                        botonActivo = "seguidores"
                        dotSeguidores.visibility = View.GONE
                        Preferencias.guardarValorBooleano("hay_notificaciones_seguidores", false)
                        noHayInteracciones.visibility = View.GONE
                        noHayNovedades.visibility = View.GONE
                        noHayInvitaciones.visibility = View.GONE
                        if (numSeguidores == 0) {
                            noHaySeguidores.visibility = View.VISIBLE
                        }
                    }
                }

                if (dotInvitaciones.visibility == View.GONE && dotInteracciones.visibility == View.GONE && dotNovedades.visibility == View.GONE && dotSeguidores.visibility == View.GONE) {
                    dotNotificacion.visibility = View.GONE
                    Preferencias.guardarValorBooleano("hay_notificaciones", false)
                    Log.d ("notis generales", "${Preferencias.obtenerValorBooleano("hay_notificaciones", false)}")
                }
            }
        }

        seleccionarBoton(btnInvitaciones, recyclerInvitaciones)
    }

    private fun cogerInvitaciones() {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.getInvitaciones(authHeader).enqueue(object :
            Callback<GetInvitacionesResponse> {
            override fun onResponse(call: Call<GetInvitacionesResponse>, response: Response<GetInvitacionesResponse>) {
                if (response.isSuccessful) {
                    val respuesta = response.body()
                    respuesta?.let {
                        val invitaciones = respuesta.invitaciones
                        numInvitaciones = invitaciones.size
                        if (numInvitaciones == 0 && botonActivo == "invitaciones") {
                            noHayInvitaciones.visibility = View.VISIBLE
                        }
                        val invitacionesMutable: MutableList<InvitacionPlaylist> = invitaciones.toMutableList()

                        val adapter = InvitacionesAdapter(
                            invitacionesMutable,
                            onAceptarClick = { invitacion -> aceptarInvitacion(invitacion) },
                            onRechazarClick = { invitacion -> rechazarInvitacion(invitacion) }
                        )

                        recyclerInvitaciones.adapter = adapter
                    }
                }
            }

            override fun onFailure(call: Call<GetInvitacionesResponse>, t: Throwable) {
                Log.d("Get Invitaciones", "Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun cogerNovedades() {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.getNovedades(authHeader).enqueue(object :
            Callback<GetNovedadesResponse> {
            override fun onResponse(call: Call<GetNovedadesResponse>, response: Response<GetNovedadesResponse>) {
                if (response.isSuccessful) {
                    val respuesta = response.body()
                    respuesta?.let {
                        val novedades: List<Novedad> = respuesta.resultado
                        numNovedades = novedades.size
                        if (numNovedades == 0 && botonActivo == "novedades") {
                            noHayNovedades.visibility = View.VISIBLE
                        }
                        val novedadesMutable: MutableList<Novedad> = novedades.toMutableList()

                        val adapter = NovedadesAdapter(novedadesMutable, onAceptarClick = { novedad -> verNovedad(novedad) }, onCerrarClick = { novedad -> cerrarNovedad(novedad) })
                        recyclerNovedades.adapter = adapter
                    }
                }
            }

            override fun onFailure(call: Call<GetNovedadesResponse>, t: Throwable) {
                Log.d("Get Novedades", "Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun cogerInteracciones() {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.getInteracciones(authHeader).enqueue(object :
            Callback<GetInteraccionesResponse> {
            override fun onResponse(call: Call<GetInteraccionesResponse>, response: Response<GetInteraccionesResponse>) {
                if (response.isSuccessful) {
                    val respuesta = response.body()
                    respuesta?.let {
                        val interacciones = respuesta.resultado
                        numInteracciones = interacciones.size
                        if (numInteracciones == 0 && botonActivo == "interacciones") {
                            noHayInteracciones.visibility = View.VISIBLE
                        }
                        val interaccionesMutable: MutableList<Interaccion> = interacciones.toMutableList()

                        val adapter = InteraccionesAdapter(interaccionesMutable,  onAceptarClick = { interaccion -> verInteraccion(interaccion) }, onCerrarClick = { interaccion -> cerrarInteraccion(interaccion) })
                        recyclerInteracciones.adapter = adapter
                    }
                }
            }

            override fun onFailure(call: Call<GetInteraccionesResponse>, t: Throwable) {
                Log.d("Get Interacciones", "Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun cogerNuevosSeguidores() {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.getNuevosSeguidores(authHeader).enqueue(object :
            Callback<GetNuevosSeguidoresResponse> {
            override fun onResponse(call: Call<GetNuevosSeguidoresResponse>, response: Response<GetNuevosSeguidoresResponse>) {
                if (response.isSuccessful) {
                    val respuesta = response.body()
                    respuesta?.let {
                        val seguidores = respuesta.resultado
                        numSeguidores = seguidores.size
                        if (numSeguidores == 0 && botonActivo == "seguidores") {
                            noHaySeguidores.visibility = View.VISIBLE
                        }
                        val seguidoresMutable: MutableList<Seguidor> = seguidores.toMutableList()

                        val adapter = SeguidoresAdapter(seguidoresMutable, onAceptarClick = { seguidor -> verSeguidor(seguidor) }, onCerrarClick = { seguidor -> cerrarSeguidor(seguidor) })
                        recyclerSeguidores.adapter = adapter
                    }
                }
            }

            override fun onFailure(call: Call<GetNuevosSeguidoresResponse>, t: Throwable) {
                Log.d("Get Nuevos Seguidores", "Error en la solicitud: ${t.message}")
            }
        })
    }


    private fun aceptarInvitacion (invitacion: InvitacionPlaylist) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        val request = AceptarInvitacionRequest(invitacion.id)
        apiService.aceptarInvitacionPlaylist(authHeader, request).enqueue(object :
            Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    val adapter = recyclerInvitaciones.adapter as? InvitacionesAdapter
                    adapter?.eliminarInvitacion(invitacion)
                    numInvitaciones = numInvitaciones - 1
                    if (numInvitaciones == 0 && botonActivo == "invitaciones") {
                        noHayInvitaciones.visibility = View.VISIBLE
                    }
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("Aceptar Invitacion", "Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun rechazarInvitacion (invitacion: InvitacionPlaylist) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        val request = AceptarInvitacionRequest(invitacion.id)
        apiService.rechazarInvitacionPlaylist(authHeader, request).enqueue(object :
            Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    val adapter = recyclerInvitaciones.adapter as? InvitacionesAdapter
                    adapter?.eliminarInvitacion(invitacion)
                    numInvitaciones = numInvitaciones - 1
                    if (numInvitaciones == 0 && botonActivo == "invitaciones") {
                        noHayInvitaciones.visibility = View.VISIBLE
                    }
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("Rechazar Invitacion", "Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun verInteraccion (interaccion: Interaccion) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        val request = VerInteraccionRequest(interaccion.noizzy)
        apiService.verInteraccion(authHeader, request).enqueue(object :
            Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    val adapter = recyclerInteracciones.adapter as? InteraccionesAdapter
                    adapter?.eliminarVariasInteraccion(interaccion)
                    numInteracciones = numInteracciones - 1
                    if (numInteracciones == 0 && botonActivo == "interacciones") {
                        noHayInteracciones.visibility = View.VISIBLE
                    }
                    //IR AL NOIZZY
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("Ver interaccion", "Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun cerrarInteraccion(interaccion: Interaccion) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        if (interaccion.tipo == "like") {
            val request = LeerNotiLikeRequest(interaccion.noizzy, interaccion.nombreUsuario)
            apiService.leerNotificacionLike(authHeader, request).enqueue(object :
                Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        val adapter = recyclerInteracciones.adapter as? InteraccionesAdapter
                        adapter?.eliminarInteraccion(interaccion)
                        numInteracciones = numInteracciones - 1
                        if (numInteracciones == 0 && botonActivo == "interacciones") {
                            noHayInteracciones.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.d("Cerrar Noti Like", "Error en la solicitud: ${t.message}")
                }
            })
        } else{
            val request = LeerNotiNoizzitoRequest(interaccion.noizzito!!)
            apiService.leerNotificacionNoizzito(authHeader, request).enqueue(object :
                Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        val adapter = recyclerInteracciones.adapter as? InteraccionesAdapter
                        adapter?.eliminarInteraccion(interaccion)
                        numInteracciones = numInteracciones - 1
                        if (numInteracciones == 0 && botonActivo == "interacciones") {
                            noHayInteracciones.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.d("Cerrar Noti Noizzito", "Error en la solicitud: ${t.message}")
                }
            })
        }

    }

    private fun verNovedad (novedad: Novedad) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        if (novedad.tipo == "cancion") {
            val request = DeleteNotiCancionRequest(novedad.id)
            apiService.deleteNotificacionCancion(authHeader, request).enqueue(object :
                Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        val adapter = recyclerNovedades.adapter as? NovedadesAdapter
                        adapter?.eliminarNovedad(novedad)
                        numNovedades = numNovedades - 1
                        if (numNovedades == 0 && botonActivo == "novedades") {
                            noHayNovedades.visibility = View.VISIBLE
                        }

                        val intent = Intent(this@Notificaciones, AlbumDetail::class.java).apply {
                            putExtra("id", novedad.id)
                        }
                        startActivity(intent)
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.d("Delete Notificacion Cancion", "Error en la solicitud: ${t.message}")
                }
            })
        } else {
            val request = DeleteNotiAlbumRequest(novedad.id)
            apiService.deleteNotificacionAlbum(authHeader, request).enqueue(object :
                Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        val adapter = recyclerNovedades.adapter as? NovedadesAdapter
                        adapter?.eliminarNovedad(novedad)
                        numNovedades = numNovedades - 1
                        if (numNovedades == 0 && botonActivo == "novedades") {
                            noHayNovedades.visibility = View.VISIBLE
                        }

                        val intent = Intent(this@Notificaciones, AlbumDetail::class.java).apply {
                            putExtra("id", novedad.id)
                        }
                        startActivity(intent)
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.d("Delete Notificacion Album", "Error en la solicitud: ${t.message}")
                }
            })
        }
    }

    private fun cerrarNovedad (novedad: Novedad) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        if (novedad.tipo == "cancion") {
            val request = DeleteNotiCancionRequest(novedad.id)
            apiService.deleteNotificacionCancion(authHeader, request).enqueue(object :
                Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        val adapter = recyclerNovedades.adapter as? NovedadesAdapter
                        adapter?.eliminarNovedad(novedad)
                        numNovedades = numNovedades - 1
                        if (numNovedades == 0 && botonActivo == "novedades") {
                            noHayNovedades.visibility = View.VISIBLE
                        }
                    }
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.d("Cerrar: Delete Notificacion Cancion", "Error en la solicitud: ${t.message}")
                }
            })
        } else {
            val request = DeleteNotiAlbumRequest(novedad.id)
            apiService.deleteNotificacionAlbum(authHeader, request).enqueue(object :
                Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        val adapter = recyclerNovedades.adapter as? NovedadesAdapter
                        adapter?.eliminarNovedad(novedad)
                        numNovedades = numNovedades - 1
                        if (numNovedades == 0 && botonActivo == "novedades") {
                            noHayNovedades.visibility = View.VISIBLE
                        }
                    }
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.d("Cerrar: Delete Notificacion Album", "Error en la solicitud: ${t.message}")
                }
            })
        }
    }

    private fun verSeguidor (seguidor: Seguidor) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        val request = LeerNotiSeguidorRequest(seguidor.nombreUsuario)

        apiService.leerNotificacionSeguidor(authHeader, request).enqueue(object :
            Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    val adapter = recyclerSeguidores.adapter as? SeguidoresAdapter
                    adapter?.eliminarSeguidor(seguidor)
                    numSeguidores = numSeguidores - 1
                    if (numSeguidores == 0 && botonActivo == "seguidores") {
                        noHaySeguidores.visibility = View.VISIBLE
                    }

                    if (seguidor.tipo == "oyente") {
                        val intent = Intent(this@Notificaciones, OtroOyente::class.java).apply {
                            putExtra("nombre", seguidor.nombreUsuario)
                        }
                        startActivity(intent)
                    } else {
                        val intent = Intent(this@Notificaciones, OtroArtista::class.java).apply {
                            putExtra("nombre", seguidor.nombreUsuario)
                        }
                        startActivity(intent)
                    }

                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("Leer Notificacion Seguidor", "Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun cerrarSeguidor (seguidor: Seguidor) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        val request = LeerNotiSeguidorRequest(seguidor.nombreUsuario)

        apiService.leerNotificacionSeguidor(authHeader, request).enqueue(object :
            Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    val adapter = recyclerSeguidores.adapter as? SeguidoresAdapter
                    adapter?.eliminarSeguidor(seguidor)
                    numSeguidores = numSeguidores - 1
                    if (numSeguidores == 0 && botonActivo == "seguidores") {
                        noHaySeguidores.visibility = View.VISIBLE
                    }
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("Cerrar Notificacion Seguidor", "Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun updateMiniReproductor() {
        val songImage = findViewById<ImageView>(R.id.songImage)
        val songTitle = findViewById<TextView>(R.id.songTitle)
        val songArtist = findViewById<TextView>(R.id.songArtist)
        val stopButton = findViewById<ImageButton>(R.id.stopButton)
        val btnAvanzar = findViewById<ImageButton>(R.id.btnAvanzar)
        val btnRetroceder = findViewById<ImageButton>(R.id.btnRetroceder)

        val songImageUrl = Preferencias.obtenerValorString("fotoPortadaActual", "")
        val songTitleText = Preferencias.obtenerValorString("nombreCancionActual", "")
        val songArtistText = Preferencias.obtenerValorString("nombreArtisticoActual", "")
        val songProgress = Preferencias.obtenerValorEntero("progresoCancionActual", 0)

        // Imagen
        if (songImageUrl.isNullOrEmpty()) {
            songImage.setImageResource(R.drawable.no_cancion)
        } else {
            Glide.with(this)
                .load(songImageUrl)
                .centerCrop()
                .placeholder(R.drawable.no_cancion)
                .error(R.drawable.no_cancion)
                .into(songImage)
        }

        songTitle.text = songTitleText
        songArtist.text = songArtistText
        progressBar.progress = songProgress/1749

        songImage.setOnClickListener {
            startActivity(Intent(this, CancionReproductorDetail::class.java))
        }

        // Configurar botón de play/pause
        btnRetroceder.setOnClickListener {
            val hayColeccion = Preferencias.obtenerValorString("coleccionActualId", "")
            if(hayColeccion == ""){
                val cancionActual = Preferencias.obtenerValorString("cancionActualId", "")
                reproducir(cancionActual)
            }
            else{
                indexActual--
                val ordenColeccion = Preferencias.obtenerValorString("ordenColeccionActual", "")
                    .split(",")
                    .filter { id -> id.isNotEmpty() }
                if (indexActual < 0){
                    indexActual = ordenColeccion.size
                }
                Preferencias.guardarValorEntero("indexColeccionActual", indexActual)
                reproducirColeccion()
            }
        }
        // Configurar botón de play/pause
        btnAvanzar.setOnClickListener {
            val hayColeccion = Preferencias.obtenerValorString("coleccionActualId", "")
            if(hayColeccion == ""){
                val cancionActual = Preferencias.obtenerValorString("cancionActualId", "")
                reproducir(cancionActual)
            }
            else{
                indexActual++
                val ordenColeccion = Preferencias.obtenerValorString("ordenColeccionActual", "")
                    .split(",")
                    .filter { id -> id.isNotEmpty() }
                if (indexActual > ordenColeccion.size){
                    indexActual=0
                }
                Preferencias.guardarValorEntero("indexColeccionActual", indexActual)
                reproducirColeccion()
            }
        }
        // Configurar botón de play/pause
        stopButton.setOnClickListener {
            Log.d("MiniReproductor", "Botón presionado")
            if (musicService == null) {
                Log.w("MiniReproductor", "musicService es null")
                return@setOnClickListener
            }

            musicService?.let { service ->
                Log.d("MiniReproductor", "isPlaying: ${service.isPlaying()}")
                if (service.isPlaying()) {
                    val progreso = service.getProgress()
                    Preferencias.guardarValorEntero("progresoCancionActual", progreso)
                    service.pause()
                    stopButton.setImageResource(R.drawable.ic_pause)
                    Log.d("MiniReproductor", "Canción pausada en $progreso ms")
                } else {
                    Log.d("MiniReproductor", "Intentando reanudar la canción...")
                    service.resume()
                    stopButton.setImageResource(R.drawable.ic_play)
                    Log.d("MiniReproductor", "Canción reanudada")
                }
            }
        }

        // Añadir un OnTouchListener al ProgressBar para actualizar el progreso
        // Añadir el performClick dentro del OnTouchListener
        progressBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    updateProgressFromTouch(event.x, progressBar)
                    progressBar.performClick()  // Agregar esta línea
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    updateProgressFromTouch(event.x, progressBar)
                    progressBar.performClick()  // Agregar esta línea
                    true
                }
                MotionEvent.ACTION_UP -> {
                    updateProgressFromTouch(event.x, progressBar)
                    progressBar.performClick()  // Agregar esta línea
                    true
                }
                else -> false
            }
        }

    }

    private fun updateProgressFromTouch(x: Float, progressBar: ProgressBar) {
        // Obtener el ancho del ProgressBar
        val width = progressBar.width - progressBar.paddingLeft - progressBar.paddingRight
        // Calcular el progreso basado en la posición del toque (x)
        val progress = ((x / width) * 100).toInt()

        // Actualizar el ProgressBar
        progressBar.progress = progress

        // Actualizar el progreso en el servicio de música
        musicService?.let { service ->
            val duration = service.getDuration()
            val newProgress = (progress * duration) / 100
            service.seekTo(newProgress)  // Mover la canción al nuevo progreso
            Preferencias.guardarValorEntero("progresoCancionActual", newProgress)
            Log.d("MiniReproductor", "Nuevo progreso: $newProgress ms")
        }
    }

    private fun updateProgressBar() {
        musicService?.let { service ->
            if (service.isPlaying()) {
                val current = service.getProgress()
                val duration = service.getDuration()

                if (duration > 0) {
                    val progress = (current * 100) / duration
                    progressBar.progress = progress
                }
            }
        }
    }

    private fun actualizarIconoPlayPause() {
        if (serviceBound && musicService != null) {
            val estaReproduciendo = musicService!!.isPlaying()
            val icono = if (estaReproduciendo) R.drawable.ic_play else R.drawable.ic_pause
            val stopButton = findViewById<ImageButton>(R.id.stopButton)
            stopButton.setImageResource(icono)
        }
    }

    private fun reproducir(id: String) {
        val request = AudioRequest(id)
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        val sid = WebSocketManager.getInstance().getSid()
        Log.d("WebSocket", "El SID actual es: $sid")

        if (sid == null) {
            Log.e("MiApp", "No se ha generado un sid para el WebSocket")
            return
        }

        // Llamada a la API con el sid en los headers
        apiService.reproducirCancion(authHeader, sid, request).enqueue(object : Callback<AudioResponse> {
            override fun onResponse(call: Call<AudioResponse>, response: Response<AudioResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { audioResponse ->
                        val respuestaTexto = "Audio: ${audioResponse.audio}, Favorito: ${audioResponse.fav}"

                        Preferencias.guardarValorString("coleccionActualId", "")
                        // Mostrar en Logcat
                        Log.d("API_RESPONSE", "Respuesta exitosa: $respuestaTexto")

                        // Mostrar en Toast
                        //Toast.makeText(this@Home, respuestaTexto, Toast.LENGTH_LONG).show()

                        reproducirAudio(audioResponse.audio)
                        notificarReproduccion()

                        Preferencias.guardarValorString("audioCancionActual", audioResponse.audio)
                        guardarDatoscCancion(id)
                        actualizarIconoPlayPause()
                    }
                } else {
                    val errorMensaje = response.errorBody()?.string() ?: "Error desconocido"

                    // Mostrar en Logcat
                    Log.e("API_RESPONSE", "Error en la respuesta: Código ${response.code()} - $errorMensaje")

                }
            }

            override fun onFailure(call: Call<AudioResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)

            }
        })
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
                        actualizarIconoPlayPause()
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

                        updateMiniReproductor()
                        actualizarIconoPlayPause()
                    }
                } else {
                    val errorMensaje = response.errorBody()?.string() ?: "Error desconocido"

                    // Mostrar en Logcat
                    Log.e("API_RESPONSE", "Error en la respuesta: Código ${response.code()} - $errorMensaje")

                    // Mostrar en Toast
                    Toast.makeText(this@Notificaciones, "Error: $errorMensaje", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<CancionInfoResponse>, t: Throwable) {
                // Mostrar en Logcat
                Log.e("API_RESPONSE", "Error de conexión: ${t.message}", t)

                // Mostrar en Toast
                Toast.makeText(this@Notificaciones, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        WebSocketEventHandler.registrarListenerSeguidor(listenerSeguidor)
        WebSocketEventHandler.registrarListenerNovedad(listenerNovedad)
        WebSocketEventHandler.registrarListenerInteraccion(listenerInteraccion)
        WebSocketEventHandler.registrarListenerInvitacion(listenerInvitacion)
        val intent = Intent(this, MusicPlayerService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        WebSocketEventHandler.eliminarListenerSeguidor(listenerSeguidor)
        WebSocketEventHandler.eliminarListenerNovedad(listenerNovedad)
        WebSocketEventHandler.eliminarListenerInteraccion(listenerInteraccion)
        WebSocketEventHandler.eliminarListenerInvitacion(listenerInvitacion)
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    private fun setupNavigation() {
        val buttonPerfil: ImageButton = findViewById(R.id.profileImageButton)
        val buttonHome: ImageButton = findViewById(R.id.nav_home)
        val buttonSearch: ImageButton = findViewById(R.id.nav_search)
        val buttonCrear: ImageButton = findViewById(R.id.nav_create)

        buttonPerfil.setOnClickListener {
            val esOyente = Preferencias.obtenerValorString("esOyente", "")
            if (esOyente == "oyente") {
                Log.d("Login", "El usuario es un oyente")
                startActivity(Intent(this, Perfil::class.java))
            } else {
                Log.d("Login", "El usuario NO es un oyente")
                startActivity(Intent(this, PerfilArtista::class.java))
            }
        }

        buttonHome.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }

        buttonSearch.setOnClickListener {
            startActivity(Intent(this, Buscador::class.java))
        }

        buttonCrear.setOnClickListener {
            startActivity(Intent(this, CrearPlaylist::class.java))
        }
    }

}
