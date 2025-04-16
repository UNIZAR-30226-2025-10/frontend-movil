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
import com.example.myapplication.Adapters.Notificaciones.InteraccionesAdapter
import com.example.myapplication.Adapters.Notificaciones.InvitacionesAdapter
import com.example.myapplication.Adapters.Notificaciones.NovedadesAdapter
import com.example.myapplication.Adapters.Notificaciones.SeguidoresAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.AceptarInvitacionRequest
import com.example.myapplication.io.request.DeleteNotiAlbumRequest
import com.example.myapplication.io.request.DeleteNotiCancionRequest
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
import com.example.myapplication.utils.Preferencias
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Notificaciones : AppCompatActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notificaciones)

        apiService = ApiService.create()

        val btnInvitaciones = findViewById<LinearLayout>(R.id.btnInvitaciones)
        val btnInteracciones = findViewById<LinearLayout>(R.id.btnInteracciones)
        val btnNovedades = findViewById<LinearLayout>(R.id.btnNovedades)
        val btnSeguidores = findViewById<LinearLayout>(R.id.btnSeguidores)

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


        //PARA ACTUALIZAR EL PUNTITO DE NOTIFICACIONES EN TIEMPO REAL
        val webSocketManager = WebSocketManager.getInstance()

        webSocketManager.listenToEvent("novedad-musical-ws") { args ->
            val data = args[0] as JSONObject
            val id = data.getString("id")
            val nombre = data.getString("nombre")
            val tipo = data.getString("tipo")
            val fotoPortada = data.getString("fotoPortada")
            val nombreArtisticoArtista = data.getString("nombreArtisticoArtista")
            val featuringArray = data.getJSONArray("featuring")
            val featurings = mutableListOf<String>()

            for (i in 0 until featuringArray.length()) {
                featurings.add(featuringArray.getString(i))
            }

            val novedad = Novedad (
                id = id,
                nombre = nombre,
                tipo = tipo,
                fotoPortada = fotoPortada,
                nombreArtisticoArtista = nombreArtisticoArtista,
                featuring = featurings
            )

            runOnUiThread {
                dotNovedades.visibility = View.VISIBLE
                Preferencias.guardarValorBooleano("hay_notificaciones_novedades", true)
                if (Preferencias.obtenerValorBooleano("hay_notificaciones",false) == false) {
                    dotNotificacion.visibility = View.VISIBLE
                    Preferencias.guardarValorBooleano("hay_notificaciones", true)
                }
                val adapter = recyclerNovedades.adapter as? NovedadesAdapter
                adapter?.agregarNovedad(novedad)
            }
        }

        webSocketManager.listenToEvent("nuevo-seguidor-ws") { args ->
            Log.d("evento", "dentro evento seguidor")
            val data = args[0] as JSONObject
            val nombre = data.getString("nombre")
            val nombreUsuario = data.getString("nombreUsuario")
            val fotoPerfil = data.getString("fotoPerfil")
            val tipo = data.getString("tipo")


            val seguidor = Seguidor (
                nombre= nombre,
                nombreUsuario = nombreUsuario,
                fotoPerfil = fotoPerfil,
                tipo = tipo
            )

            runOnUiThread {
                dotSeguidores.visibility = View.VISIBLE
                Preferencias.guardarValorBooleano("hay_notificaciones_seguidores", true)
                if (Preferencias.obtenerValorBooleano("hay_notificaciones",false) == false) {
                    dotNotificacion.visibility = View.VISIBLE
                    Preferencias.guardarValorBooleano("hay_notificaciones", true)
                }
                val adapter = recyclerSeguidores.adapter as? SeguidoresAdapter
                adapter?.agregarSeguidor(seguidor)
            }
        }

        webSocketManager.listenToEvent("invite-to-playlist-ws") { args ->
            val data = args[0] as JSONObject
            val id = data.getString("id")
            val nombre = data.getString("nombre")
            val nombreUsuario = data.getString("nombreUsuario")
            val fotoPortada = data.getString("fotoPortada")

            val invitacion = InvitacionPlaylist (
                id = id,
                nombre= nombre,
                nombreUsuario = nombreUsuario,
                fotoPortada = fotoPortada
            )

            runOnUiThread {
                dotInvitaciones.visibility = View.VISIBLE
                Preferencias.guardarValorBooleano("hay_notificaciones_invitaciones", true)
                if (Preferencias.obtenerValorBooleano("hay_notificaciones",false) == false) {
                    dotNotificacion.visibility = View.VISIBLE
                    Preferencias.guardarValorBooleano("hay_notificaciones", true)
                }
                val adapter = recyclerInvitaciones.adapter as? InvitacionesAdapter
                adapter?.agregarInvitacion(invitacion)
            }
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
                        dotInvitaciones.visibility = View.GONE
                        Preferencias.guardarValorBooleano("hay_notificaciones_invitaciones", false)
                        Log.d("MiApp", "${Preferencias.obtenerValorBooleano("hay_notificaciones", false)}")
                    }
                    R.id.btnInteracciones -> {
                        dotInteracciones.visibility = View.GONE
                        Preferencias.guardarValorBooleano("hay_notificaciones_interacciones", false)
                    }
                    R.id.btnNovedades -> {
                        dotNovedades.visibility = View.GONE
                        Preferencias.guardarValorBooleano("hay_notificaciones_novedades", false)
                    }
                    R.id.btnSeguidores -> {
                        dotSeguidores.visibility = View.GONE
                        Preferencias.guardarValorBooleano("hay_notificaciones_seguidores", false)
                    }
                }

                if (dotInvitaciones.visibility == View.GONE && dotInteracciones.visibility == View.GONE && dotNovedades.visibility == View.GONE && dotSeguidores.visibility == View.GONE) {
                    dotNotificacion.visibility = View.GONE
                    Preferencias.guardarValorBooleano("hay_notificaciones", false)
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
                        val interaccionesMutable: MutableList<Interaccion> = interacciones.toMutableList()

                        val adapter = InteraccionesAdapter(interaccionesMutable,  onAceptarClick = { interaccion -> verInteraccion(interaccion) })
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
                    adapter?.eliminarInteraccion(interaccion)

                    //IR AL NOIZZY
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("Aceptar Invitacion", "Error en la solicitud: ${t.message}")
            }
        })
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

        buttonNotis.setOnClickListener {
            startActivity(Intent(this, Notificaciones::class.java))
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
