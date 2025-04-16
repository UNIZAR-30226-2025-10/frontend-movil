package com.example.myapplication.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.example.myapplication.io.request.DeleteNotiAlbumRequest
import com.example.myapplication.io.request.DeleteNotiCancionRequest
import com.example.myapplication.io.request.LeerNotiLikeRequest
import com.example.myapplication.io.request.LeerNotiNoizzitoRequest
import com.example.myapplication.io.request.LeerNotiSeguidorRequest
import com.example.myapplication.io.request.VerInteraccionRequest
import com.example.myapplication.io.response.GetInteraccionesResponse
import com.example.myapplication.io.response.GetInvitacionesResponse
import com.example.myapplication.io.response.GetNovedadesResponse
import com.example.myapplication.io.response.GetNuevosSeguidoresResponse
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.InvitacionPlaylist
import com.example.myapplication.io.response.Novedad
import com.example.myapplication.io.response.Seguidor
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notificaciones)

        //PARA ACTUALIZAR EL PUNTITO ROJO EN TIEMPO REAL
        //WebSocketEventHandler.registrarListenerSeguidor(listenerSeguidor)
        //WebSocketEventHandler.registrarListenerNovedad(listenerNovedad)
        //WebSocketEventHandler.registrarListenerInteraccion(listenerInteraccion)
        //WebSocketEventHandler.registrarListenerInvitacion(listenerInvitacion)

        apiService = ApiService.create()

        val btnInvitaciones = findViewById<LinearLayout>(R.id.btnInvitaciones)
        val btnInteracciones = findViewById<LinearLayout>(R.id.btnInteracciones)
        val btnNovedades = findViewById<LinearLayout>(R.id.btnNovedades)
        val btnSeguidores = findViewById<LinearLayout>(R.id.btnSeguidores)

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

        setupNavigation()

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

                        /*val intent = Intent(this@Notificaciones, AlbumDetail::class.java).apply {
                            //MANDAR ID DEL ALBUM DE LA CANCION
                            //putExtra("id", novedad.id)
                        }
                        startActivity(intent)*/
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

                        /*val intent = Intent(this@Notificaciones, AlbumDetail::class.java).apply {
                            //putExtra("id", novedad.id)
                        }
                        startActivity(intent)*/
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

                    /*if (seguidor.tipo == "oyente") {
                        val intent = Intent(this@Notificaciones, PerfilOtro::class.java).apply {
                            putExtra("nombreUsuario", seguidor.nombreUsuario)
                        }
                    } else {
                        val intent = Intent(this@Notificaciones, PerfilArtistaOtro::class.java).apply {
                            putExtra("nombreUsuario", seguidor.nombreUsuario)
                        }
                    }
                    startActivity(intent)*/
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

    private fun setupNavigation() {
        val buttonPerfil: ImageButton = findViewById(R.id.profileImageButton)
        val buttonNotis: ImageButton = findViewById(R.id.notificationImageButton)
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

        /*buttonNotis.setOnClickListener {
            startActivity(Intent(this, Notificaciones::class.java))
        }*/

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

    override fun onStart() {
        super.onStart()
        WebSocketEventHandler.registrarListenerSeguidor(listenerSeguidor)
        WebSocketEventHandler.registrarListenerNovedad(listenerNovedad)
        WebSocketEventHandler.registrarListenerInteraccion(listenerInteraccion)
        WebSocketEventHandler.registrarListenerInvitacion(listenerInvitacion)
    }

    override fun onStop() {
        super.onStop()
        WebSocketEventHandler.eliminarListenerSeguidor(listenerSeguidor)
        WebSocketEventHandler.eliminarListenerNovedad(listenerNovedad)
        WebSocketEventHandler.eliminarListenerInteraccion(listenerInteraccion)
        WebSocketEventHandler.eliminarListenerInvitacion(listenerInvitacion)
    }
}
