package com.example.myapplication.activities

import android.app.Activity
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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.Adapters.Noizzys.MisNoizzysAdapter
import com.example.myapplication.Adapters.Noizzys.NoizzyDetailAdapter
import com.example.myapplication.Adapters.Playlist.CancionesBuscadorNoizzyAdapter
import com.example.myapplication.R
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.request.DarLikeNoizzyRequest
import com.example.myapplication.io.request.DeleteNoizzyRequest
import com.example.myapplication.io.request.PostNoizzitoRequest
import com.example.myapplication.io.response.Cancion
import com.example.myapplication.io.response.CancionData
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.InvitacionPlaylist
import com.example.myapplication.io.response.NoizzitoData
import com.example.myapplication.io.response.Noizzy
import com.example.myapplication.io.response.NoizzyDetailResponse
import com.example.myapplication.io.response.Novedad
import com.example.myapplication.io.response.SearchPlaylistResponse
import com.example.myapplication.io.response.Seguidor
import com.example.myapplication.services.WebSocketEventHandler
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NoizzyDetail : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private var noizzyId: String? = null
    private var cancionAnadidaEnNoizzito: Cancion? = null

    // Vistas
    private lateinit var textViewNombreUsuario: TextView
    private lateinit var dot: View
    private lateinit var textViewTexto: TextView
    private lateinit var textViewNumLikes: TextView
    private lateinit var textViewNumComentarios: TextView
    private lateinit var imageViewFotoPerfil: ImageView
    private lateinit var textViewCancionNombre: TextView
    private lateinit var textViewArtistaNombre: TextView
    private lateinit var imageViewFotoPortada: ImageView
    private lateinit var recyclerViewNoizzitos: RecyclerView
    private lateinit var recyclerNoizzys: RecyclerView

    private lateinit var adapter: NoizzyDetailAdapter
    private lateinit var noizzyPrincipal: NoizzyDetailResponse
    private lateinit var btnLikePrincipal: ImageButton
    private lateinit var btnDeletePrincipal: ImageButton

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


    //ESTOS NO SON DE NOTIS

    private val listenerNoizzito: (NoizzitoData, String) -> Unit = { noizzito, idNoizzy ->
        runOnUiThread {
            Log.d("LOGS_NOTIS", "evento en noizzydetail")

            if(idNoizzy == noizzyId) {
                val adapter = recyclerViewNoizzitos.adapter as? NoizzyDetailAdapter
                adapter?.agregarNoizzito(noizzito)
                val numeroActual = textViewNumComentarios.text.toString().toInt()
                textViewNumComentarios.text = (numeroActual + 1).toString()
                recyclerViewNoizzitos.smoothScrollToPosition(0)
            }
        }
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            noizzyId?.let { datosNoizzy(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.noizzy_detail)

        // Inicializar servicio API
        apiService = ApiService.create()

        noizzyId = intent.getStringExtra("id")
        Log.d("NoizzyAdapter", "ID que se llega: ${noizzyId}")
        if (noizzyId == null) {
            Toast.makeText(this, "Error: No se proporcionó ID de Noizzy", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inicializar vistas
        textViewNombreUsuario = findViewById(R.id.noizzyUserName)
        textViewTexto = findViewById(R.id.noizzyContent)
        textViewNumLikes = findViewById(R.id.likeCount)
        textViewNumComentarios = findViewById(R.id.commentCount)
        imageViewFotoPerfil = findViewById(R.id.noizzyProfileImage)
        textViewCancionNombre = findViewById(R.id.recuerdoText1)
        textViewArtistaNombre = findViewById(R.id.recuerdoText2)
        imageViewFotoPortada = findViewById(R.id.recuerdoImage)
        recyclerViewNoizzitos = findViewById(R.id.rvNoizzitos)
        dot = findViewById<View>(R.id.notificationDot)


        btnLikePrincipal = findViewById(R.id.likeButton)
        val btnCommentPrincipal = findViewById<ImageButton>(R.id.commentButton)

        btnLikePrincipal.setOnClickListener {
            darLikePrincipal(noizzyPrincipal) // Ahora sí se pasa el correcto
            Toast.makeText(this, "Like en el principal: ${noizzyId}", Toast.LENGTH_SHORT).show()
        }

        btnCommentPrincipal.setOnClickListener {
            comentarPrincipal(noizzyPrincipal) // Ahora también correcto
            Toast.makeText(this, "Comentario en el principal: ${noizzyId}", Toast.LENGTH_SHORT).show()
        }

        btnDeletePrincipal = findViewById(R.id.deleteButtonNoizzy)
        btnDeletePrincipal.setOnClickListener { borrarPrincipal()}

        recyclerViewNoizzitos.layoutManager = LinearLayoutManager(this)



        // Configurar imagen de perfil
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
        WebSocketEventHandler.registrarListenerNoizzito(listenerNoizzito)

        datosNoizzy(noizzyId!!)
        setupNavigation()
    }

    private fun datosNoizzy(id: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        Log.d("NoizzyAdapter", "entra datos")

        apiService.datosNoizzy(authHeader, id).enqueue(object : Callback<NoizzyDetailResponse> {
            override fun onResponse(call: Call<NoizzyDetailResponse>, response: Response<NoizzyDetailResponse>) {
                if (response.isSuccessful) {
                    val respuesta = response.body()
                    respuesta?.let { noizzy ->
                        Log.d("NoizzyAdapter", "entra datos correctos")
                        Log.d("NoizzyAdapter", "ID ${noizzy.id}")
                        Log.d("NoizzyAdapter", "ID ${noizzy.nombreUsuario}")
                        noizzyPrincipal = noizzy

                        if(noizzyPrincipal.mio) {
                            btnDeletePrincipal.visibility = View.VISIBLE
                        }

                        val likeDrawable = if (noizzy.like) {
                            R.drawable.like_noizzy_selected
                        } else {
                            R.drawable.like_noizzy
                        }
                        btnLikePrincipal.setImageResource(likeDrawable)


                        // Mostrar datos en vistas
                        textViewNombreUsuario.text = noizzy.nombre
                        textViewTexto.text = noizzy.texto
                        textViewNumLikes.text = noizzy.num_likes.toString()
                        textViewNumComentarios.text = noizzy.num_comentarios.toString()

                        // Mostrar foto de perfil si existe
                        if (!noizzy.fotoPerfil.isNullOrEmpty()) {
                            Glide.with(this@NoizzyDetail)
                                .load(noizzy.fotoPerfil)
                                .circleCrop()
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_profile)
                                .into(imageViewFotoPerfil)
                        } else {
                            imageViewFotoPerfil.visibility = View.GONE
                        }

                        // Mostrar canción si existe
                        noizzy.cancion?.let { cancion ->
                            // Verificamos si tanto el nombre de la canción como el nombre artístico están presentes
                            if (!cancion.nombre.isNullOrEmpty() && !cancion.nombreArtisticoArtista.isNullOrEmpty()) {
                                // Si ambos campos son válidos, mostramos los datos de la canción
                                Log.d("NoizzyAdapter", "Canción disponible")
                                textViewCancionNombre.text = cancion.nombre
                                textViewArtistaNombre.text = cancion.nombreArtisticoArtista
                                imageViewFotoPortada.visibility = View.VISIBLE

                                // Cargamos la portada de la canción con Glide
                                Glide.with(this@NoizzyDetail)
                                    .load(cancion.fotoPortada)
                                    .placeholder(R.drawable.no_cancion)  // Placeholder por si no hay imagen
                                    .error(R.drawable.no_cancion)        // Error por si la URL es inválida
                                    .into(imageViewFotoPortada)
                            } else {
                                // Si no hay información relevante, ocultamos todos los elementos relacionados
                                Log.d("NoizzyAdapter", "No hay canción disponible")
                                textViewCancionNombre.visibility = View.GONE
                                textViewArtistaNombre.visibility = View.GONE
                                imageViewFotoPortada.visibility = View.GONE
                            }
                        } ?: run {
                            // Si no hay canción en absoluto (cancion es null), ocultamos los elementos
                            Log.d("NoizzyAdapter", "No hay canción")
                            textViewCancionNombre.visibility = View.GONE
                            textViewArtistaNombre.visibility = View.GONE
                            imageViewFotoPortada.visibility = View.GONE
                        }

                        // Cargar noizzitos en RecyclerView
                        adapter = NoizzyDetailAdapter(
                            noizzy.noizzitos.toMutableList(),
                            onItemClicked = { noizzito ->
                                val intent = Intent(this@NoizzyDetail, NoizzyDetail::class.java)
                                intent.putExtra("id", noizzito.id)
                                launcher.launch(intent)
                            },
                            onLikeClicked = { noizzito ->
                                darLike(noizzito)
                                Toast.makeText(this@NoizzyDetail, "Like en: ${noizzito.id}", Toast.LENGTH_SHORT).show()
                            },
                            onCommentClicked = { noizzito ->
                                comentar(noizzito)
                                Toast.makeText(this@NoizzyDetail, "Comentario en: ${noizzito.id}", Toast.LENGTH_SHORT).show()
                            },
                            onDeleteClicked = { noizzito -> borrar(noizzito)}
                        )
                        recyclerViewNoizzitos.adapter = adapter
                    }
                } else {
                    Log.e("NoizzyDetail", "Respuesta no exitosa: ${response.code()}")
                    Toast.makeText(this@NoizzyDetail, "Error cargando datos del Noizzy", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<NoizzyDetailResponse>, t: Throwable) {
                Log.e("NoizzyDetail", "Error en la solicitud: ${t.message}", t)
                Toast.makeText(this@NoizzyDetail, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun darLike(noizzy: NoizzitoData) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        val request = DarLikeNoizzyRequest(!noizzy.like, noizzy.id.toInt())
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

    private fun comentar(noizzy: NoizzitoData) {
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

                                    Glide.with(this@NoizzyDetail)
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
                request = PostNoizzitoRequest(texto, noizzy.id.toInt(), cancionAnadidaEnNoizzito!!.id)
            } else {
                request = PostNoizzitoRequest(texto, noizzy.id.toInt(),null)
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

    private fun borrar(noizzy: NoizzitoData) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        val request = DeleteNoizzyRequest(noizzy.id.toInt())
        apiService.deleteNoizzy(authHeader, request).enqueue(object :
            Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    adapter.eliminarNoizzitoPorId(noizzy.id)
                    val numeroActual = textViewNumComentarios.text.toString().toInt()
                    textViewNumComentarios.text = (numeroActual -1).toString()
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("Delete Noizzito", "Error en la solicitud: ${t.message}")
            }
        })
    }


    private fun darLikePrincipal(noizzy: NoizzyDetailResponse) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        // Proteger conversión de id
        val idNoizzy = noizzyId?.toInt()
        if (idNoizzy == null) {
            Log.e("darLikePrincipal", "El id del noizzy es nulo o inválido, no se puede dar like")
            return
        }

        val request = DarLikeNoizzyRequest(!noizzy.like, idNoizzy)

        apiService.darLikeNoizzy(authHeader, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    noizzy.like = !noizzy.like
                    if (noizzy.like) {
                        noizzy.num_likes += 1
                    } else {
                        noizzy.num_likes -= 1
                    }
                    // Puedes actualizar la vista si quieres aquí
                    actualizarNoizzyPrincipal(noizzy,"like")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("Dar like", "Error en la solicitud: ${t.message}")
            }
        })
    }


    private fun comentarPrincipal(noizzy: NoizzyDetailResponse) {
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

                                    Glide.with(this@NoizzyDetail)
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
            if (texto.isEmpty()) {
                Toast.makeText(this, "El texto no puede estar vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.d("PostNoizzito", "Error 1")

            val token = Preferencias.obtenerValorString("token", "")
            val authHeader = "Bearer $token"
            val request = if (cancionAnadidaEnNoizzito != null) {
                Log.d("PostNoizzito", "Error 1.1")
                noizzyId?.let { it1 -> PostNoizzitoRequest(texto, it1.toInt(), cancionAnadidaEnNoizzito!!.id) }
            } else {
                Log.d("PostNoizzito", "Error 1.2")
                noizzyId?.let { it1 -> PostNoizzitoRequest(texto, it1.toInt(), null) }
            }

            Log.d("PostNoizzito", "Error 2")

            if (request != null) {
                apiService.postNoizzito(authHeader, request).enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {}
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.d("PostNoizzito", "Error en la solicitud: ${t.message}")
                    }
                })
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun borrarPrincipal() {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"

        // Proteger conversión de id
        val idNoizzy = noizzyId?.toInt()
        if (idNoizzy == null) {
            return
        }

        val request = DeleteNoizzyRequest(idNoizzy)
        apiService.deleteNoizzy(authHeader, request).enqueue(object :
            Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("Delete Noizzy", "Error en la solicitud: ${t.message}")
            }
        })
    }

    fun actualizarNoizzyPrincipal(noizzyActualizado: NoizzyDetailResponse, tipo: String) {
        when (tipo) {
            "like" -> {
                // Actualizar el contador de "likes"
                textViewNumLikes.text = noizzyActualizado.num_likes.toString()

                // Actualizar el botón de like
                val likeDrawable = if (noizzyActualizado.like) {
                    R.drawable.like_noizzy_selected
                } else {
                    R.drawable.like_noizzy
                }
                findViewById<ImageButton>(R.id.likeButton).setImageResource(likeDrawable)
            }

            "comentario" -> {
                // Actualizar el contador de "comentarios"
                textViewNumComentarios.text = noizzyActualizado.num_comentarios.toString()
            }
        }
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

    override fun onDestroy() {
        super.onDestroy()
        WebSocketEventHandler.eliminarListenerNovedad(listenerNovedad)
        WebSocketEventHandler.eliminarListenerSeguidor(listenerSeguidor)
        WebSocketEventHandler.eliminarListenerInvitacion(listenerInvitacion)
        WebSocketEventHandler.eliminarListenerInteraccion(listenerInteraccion)
        WebSocketEventHandler.eliminarListenerNoizzito(listenerNoizzito)
    }
}