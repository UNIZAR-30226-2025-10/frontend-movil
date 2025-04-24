package com.example.myapplication.activities

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.Adapters.Noizzys.MisNoizzysAdapter
import com.example.myapplication.Adapters.Notificaciones.InvitacionesAdapter
import com.example.myapplication.Adapters.Playlist.CancionesBuscadorNoizzyAdapter
import com.example.myapplication.Adapters.Playlist.SongPlaylistSearchAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.DarLikeNoizzyRequest
import com.example.myapplication.io.request.PostNoizzitoRequest
import com.example.myapplication.io.request.PostNoizzyRequest
import com.example.myapplication.io.response.Cancion
import com.example.myapplication.io.response.GetInvitacionesResponse
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.InvitacionPlaylist
import com.example.myapplication.io.response.MisNoizzysResponse
import com.example.myapplication.io.response.Noizzy
import com.example.myapplication.io.response.Novedad
import com.example.myapplication.io.response.SearchPlaylistResponse
import com.example.myapplication.io.response.Seguidor
import com.example.myapplication.services.WebSocketEventHandler
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MisNoizzys: AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var dot: View
    private lateinit var adapter: MisNoizzysAdapter
    private lateinit var recyclerNoizzys: RecyclerView
    private lateinit var botonPublicar: Button
    private var cancionAnadidaEnNoizzy: Cancion? = null
    private var cancionAnadidaEnNoizzito: Cancion? = null

    //EVENTOS PARA LAS NOTIFICACIONES
    private val listenerNovedad: (Novedad) -> Unit = {
        runOnUiThread {
            dot.visibility = View.VISIBLE
        }
    }
    private val listenerSeguidor: (Seguidor) -> Unit = {
        runOnUiThread {
            dot.visibility = View.VISIBLE
        }
    }
    private val listenerInvitacion: (InvitacionPlaylist) -> Unit = {
        runOnUiThread {
            dot.visibility = View.VISIBLE
        }
    }
    private val listenerInteraccion: (Interaccion) -> Unit = {
        runOnUiThread {
            dot.visibility = View.VISIBLE
        }
    }

    private val listenerNoizzy: (Noizzy, Boolean) -> Unit = { noizzy, mio ->
        runOnUiThread {
            Log.d("LOGS_NOTIS", "evento en mis noizzys")
            if (mio) {
                val adapter = recyclerNoizzys.adapter as? MisNoizzysAdapter
                adapter?.agregarNoizzy(noizzy)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mis_noizzys)

        apiService = ApiService.create()
        dot = findViewById<View>(R.id.notificationDot)
        recyclerNoizzys = findViewById(R.id.noizzysRecyclerView)
        recyclerNoizzys.layoutManager = LinearLayoutManager(this)
        botonPublicar = findViewById(R.id.publicarNoizzyButton)


        val profileImageButton = findViewById<ImageButton>(R.id.profileImageButton)
        val profileImageUrl = Preferencias.obtenerValorString("fotoPerfil", "")

        if (profileImageUrl.isNullOrEmpty() || profileImageUrl == "DEFAULT") {
            profileImageButton.setImageResource(R.drawable.ic_profile)
        } else {
            Glide.with(this)
                .load(profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile) // Imagen por defecto mientras carga
                .error(R.drawable.ic_profile) // Imagen si hay error
                .into(profileImageButton)
        }

        //PARA EL CIRCULITO ROJO DE NOTIFICACIONES
        if (Preferencias.obtenerValorBooleano("hay_notificaciones",false) == true) {
            dot.visibility = View.VISIBLE
        } else {
            dot.visibility = View.GONE
        }

        //Para actualizar el punto rojo en tiempo real, suscripcion a los eventos
        WebSocketEventHandler.registrarListenerNovedad(listenerNovedad)
        WebSocketEventHandler.registrarListenerSeguidor(listenerSeguidor)
        WebSocketEventHandler.registrarListenerInvitacion(listenerInvitacion)
        WebSocketEventHandler.registrarListenerInteraccion(listenerInteraccion)

        setupNavigation()
        cargarMisNoizzys()

        botonPublicar.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.popup_publicar_noizzy, null)

            val dialog = android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            dialog.window?.setDimAmount(0.7f)

            val imageView = dialogView.findViewById<ImageView>(R.id.popupProfileImage)
            val url = Preferencias.obtenerValorString("fotoPerfil", "")

            if (url.isNullOrEmpty() || url == "DEFAULT") {
                imageView.setImageResource(R.drawable.ic_profile)
            } else {
                Glide.with(this)
                    .load(url)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(imageView)
            }

            val editText = dialogView.findViewById<EditText>(R.id.popupEditText)
            val addSongText= dialogView.findViewById<TextView>(R.id.popupAddSongButton)
            addSongText.paintFlags = addSongText.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            val publishButton = dialogView.findViewById<Button>(R.id.popupPublishButton)
            val cerrarVentana = dialogView.findViewById<ImageButton>(R.id.closeButton)

            val buscador = dialogView.findViewById<LinearLayout>(R.id.buscador)
            val cancionAnadida = dialogView.findViewById<LinearLayout>(R.id.cancionNoizzy)
            val searchEditText = dialogView.findViewById<EditText>(R.id.searchSongEditText)
            val songResults = dialogView.findViewById<RecyclerView>(R.id.songResultsRecyclerView)
            songResults.layoutManager = LinearLayoutManager(this)

            val dividerItemDecoration = DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
            ContextCompat.getDrawable(this, R.drawable.recycler_divider)?.let {
                dividerItemDecoration.setDrawable(it)
            }
            songResults.addItemDecoration(dividerItemDecoration)

            addSongText.setOnClickListener {
                addSongText.visibility = View.GONE
                buscador.visibility = View.VISIBLE
            }

            searchEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val query = s.toString().trim()
                    if (query.isNotEmpty()) {

                        val token = Preferencias.obtenerValorString("token", "")
                        val authHeader = "Bearer $token"

                        apiService.searchForSongsNoizzy(authHeader, query).enqueue(object :
                            Callback<SearchPlaylistResponse> {
                            override fun onResponse(call: Call<SearchPlaylistResponse>, response: Response<SearchPlaylistResponse>) {
                                if (response.isSuccessful) {
                                    val canciones = response.body()?.canciones ?: emptyList()
                                    songResults.adapter = CancionesBuscadorNoizzyAdapter(canciones) { cancionSeleccionada ->
                                        buscador.visibility = View.GONE
                                        cancionAnadidaEnNoizzy = cancionSeleccionada
                                        val imagenCancion = dialogView.findViewById<ImageView>(R.id.recuerdoImage)
                                        val nombreCancion = dialogView.findViewById<TextView>(R.id.recuerdoText1)
                                        val nombreArtista = dialogView.findViewById<TextView>(R.id.recuerdoText2)
                                        val botonQuitar = dialogView.findViewById<ImageButton>(R.id.quitarCancion)

                                        Glide.with(this@MisNoizzys)
                                            .load(cancionSeleccionada.fotoPortada)
                                            .placeholder(R.drawable.no_cancion)
                                            .error(R.drawable.no_cancion)
                                            .into(imagenCancion)

                                        nombreCancion.text = cancionSeleccionada.nombre
                                        nombreArtista.text = cancionSeleccionada.nombreArtisticoArtista

                                        botonQuitar.setOnClickListener {
                                            cancionAnadidaEnNoizzy = null
                                            addSongText.visibility = View.VISIBLE
                                            cancionAnadida.visibility = View.GONE
                                        }

                                        cancionAnadida.visibility = View.VISIBLE
                                    }
                                }
                            }
                            override fun onFailure(call: Call<SearchPlaylistResponse>, t: Throwable) {
                                Log.d("Buscar Canciones para Noizzy", "Error en la solicitud: ${t.message}")
                            }
                        })
                    }
                }
            })


            cerrarVentana.setOnClickListener {
                cancionAnadidaEnNoizzy = null
                dialog.dismiss()
            }

            publishButton.setOnClickListener {
                val texto = editText.text.toString()

                val token = Preferencias.obtenerValorString("token", "")
                val authHeader = "Bearer $token"
                val request: PostNoizzyRequest
                if (cancionAnadidaEnNoizzy != null) {
                    request = PostNoizzyRequest(texto, cancionAnadidaEnNoizzy!!.id)
                } else {
                    request = PostNoizzyRequest(texto, null)
                }

                apiService.postNoizzy(authHeader, request).enqueue(object :
                    Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            //cargarMisNoizzys()
                        }
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.d("Post Noizzy", "Error en la solicitud: ${t.message}")
                    }
                })
                dialog.dismiss()
            }
            dialog.show()
        }
    }

    private fun cargarMisNoizzys() {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        apiService.getMisNoizzys(authHeader).enqueue(object :
            Callback<MisNoizzysResponse> {
            override fun onResponse(call: Call<MisNoizzysResponse>, response: Response<MisNoizzysResponse>) {
                if (response.isSuccessful) {
                    val respuesta = response.body()
                    respuesta?.let {
                        val noizzys = response.body()?.noizzys ?: emptyList()
                        val noizzysMutable: MutableList<Noizzy> = noizzys.toMutableList()

                        adapter = MisNoizzysAdapter(
                            noizzysMutable,
                            onLikeClicked = { noizzy -> darLike(noizzy)},
                            onCommentClicked = { noizzy -> comentar(noizzy) }
                        )

                        recyclerNoizzys.adapter = adapter
                    }
                }
            }
            override fun onFailure(call: Call<MisNoizzysResponse>, t: Throwable) {
                Log.d("Mis Noizzys", "Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun darLike(noizzy: Noizzy) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        val request = DarLikeNoizzyRequest(!noizzy.like, noizzy.id)
        apiService.darLikeNoizzy(authHeader, request).enqueue(object :
            Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    noizzy.like = !noizzy.like
                    if (noizzy.like) {
                        noizzy.num_likes += 1
                    } else {
                        noizzy.num_likes -= 1
                    }
                    adapter.actualizarNoizzy(noizzy)
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("Dar like", "Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun comentar(noizzy: Noizzy) {
        val dialogView = layoutInflater.inflate(R.layout.popup_publicar_noizzito, null)

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setDimAmount(0.7f)

        val imageView = dialogView.findViewById<ImageView>(R.id.popupProfileImage)
        val url = Preferencias.obtenerValorString("fotoPerfil", "")

        if (url.isNullOrEmpty() || url == "DEFAULT") {
            imageView.setImageResource(R.drawable.ic_profile)
        } else {
            Glide.with(this)
                .load(url)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(imageView)
        }

        val editText = dialogView.findViewById<EditText>(R.id.popupEditText)
        val addSongText= dialogView.findViewById<TextView>(R.id.popupAddSongButton)
        addSongText.paintFlags = addSongText.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        val publishButton = dialogView.findViewById<Button>(R.id.popupPublishButton)
        val cerrarVentana = dialogView.findViewById<ImageButton>(R.id.closeButton)

        val buscador = dialogView.findViewById<LinearLayout>(R.id.buscador)
        val cancionAnadida = dialogView.findViewById<LinearLayout>(R.id.cancionNoizzy)
        val searchEditText = dialogView.findViewById<EditText>(R.id.searchSongEditText)
        val songResults = dialogView.findViewById<RecyclerView>(R.id.songResultsRecyclerView)
        songResults.layoutManager = LinearLayoutManager(this)

        val dividerItemDecoration = DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        ContextCompat.getDrawable(this, R.drawable.recycler_divider)?.let {
            dividerItemDecoration.setDrawable(it)
        }
        songResults.addItemDecoration(dividerItemDecoration)

        addSongText.setOnClickListener {
            addSongText.visibility = View.GONE
            buscador.visibility = View.VISIBLE
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {

                    val token = Preferencias.obtenerValorString("token", "")
                    val authHeader = "Bearer $token"

                    apiService.searchForSongsNoizzy(authHeader, query).enqueue(object :
                        Callback<SearchPlaylistResponse> {
                        override fun onResponse(call: Call<SearchPlaylistResponse>, response: Response<SearchPlaylistResponse>) {
                            if (response.isSuccessful) {
                                val canciones = response.body()?.canciones ?: emptyList()
                                songResults.adapter = CancionesBuscadorNoizzyAdapter(canciones) { cancionSeleccionada ->
                                    buscador.visibility = View.GONE
                                    cancionAnadidaEnNoizzito = cancionSeleccionada
                                    val imagenCancion = dialogView.findViewById<ImageView>(R.id.recuerdoImage)
                                    val nombreCancion = dialogView.findViewById<TextView>(R.id.recuerdoText1)
                                    val nombreArtista = dialogView.findViewById<TextView>(R.id.recuerdoText2)
                                    val botonQuitar = dialogView.findViewById<ImageButton>(R.id.quitarCancion)

                                    Glide.with(this@MisNoizzys)
                                        .load(cancionSeleccionada.fotoPortada)
                                        .placeholder(R.drawable.no_cancion)
                                        .error(R.drawable.no_cancion)
                                        .into(imagenCancion)

                                    nombreCancion.text = cancionSeleccionada.nombre
                                    nombreArtista.text = cancionSeleccionada.nombreArtisticoArtista

                                    botonQuitar.setOnClickListener {
                                        cancionAnadidaEnNoizzito = null
                                        addSongText.visibility = View.VISIBLE
                                        cancionAnadida.visibility = View.GONE
                                    }

                                    cancionAnadida.visibility = View.VISIBLE
                                }
                            }
                        }
                        override fun onFailure(call: Call<SearchPlaylistResponse>, t: Throwable) {
                            Log.d("Buscar Canciones para Noizzito", "Error en la solicitud: ${t.message}")
                        }
                    })
                }
            }
        })


        cerrarVentana.setOnClickListener {
            cancionAnadidaEnNoizzito = null
            dialog.dismiss()
        }

        publishButton.setOnClickListener {
            val texto = editText.text.toString()

            val token = Preferencias.obtenerValorString("token", "")
            val authHeader = "Bearer $token"
            val request: PostNoizzitoRequest
            if (cancionAnadidaEnNoizzito != null) {
                request = PostNoizzitoRequest(texto, noizzy.id, cancionAnadidaEnNoizzito!!.id)
            } else {
                request = PostNoizzitoRequest(texto, noizzy.id,null)
            }

            apiService.postNoizzito(authHeader, request).enqueue(object :
                Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        noizzy.num_comentarios += 1
                        adapter.actualizarNoizzy(noizzy)
                    }
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.d("Post Noizzito", "Error en la solicitud: ${t.message}")
                }
            })
            dialog.dismiss()
        }
        dialog.show()
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

    override fun onStart() {
        super.onStart()
        WebSocketEventHandler.registrarListenerNoizzy(listenerNoizzy)
    }

    override fun onStop() {
        super.onStop()
        WebSocketEventHandler.eliminarListenerNoizzy(listenerNoizzy)
    }

    override fun onDestroy() {
        super.onDestroy()
        WebSocketEventHandler.eliminarListenerNovedad(listenerNovedad)
        WebSocketEventHandler.eliminarListenerSeguidor(listenerSeguidor)
        WebSocketEventHandler.eliminarListenerInvitacion(listenerInvitacion)
        WebSocketEventHandler.eliminarListenerInteraccion(listenerInteraccion)
    }
}