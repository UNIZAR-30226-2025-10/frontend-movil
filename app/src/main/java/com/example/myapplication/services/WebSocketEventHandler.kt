package com.example.myapplication.services

import android.util.Log
import com.example.myapplication.io.response.Cancion
import com.example.myapplication.io.response.CancionData
import com.example.myapplication.io.response.CancionNoizzy
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.InvitacionPlaylist
import com.example.myapplication.io.response.NoizzitoData
import com.example.myapplication.io.response.Noizzy
import com.example.myapplication.io.response.Novedad
import com.example.myapplication.io.response.Seguidor
import com.example.myapplication.utils.Preferencias
import org.json.JSONObject

object WebSocketEventHandler {
    private val seguidoresListeners = mutableListOf<(Seguidor) -> Unit>()
    private val novedadesListeners = mutableListOf<(Novedad) -> Unit>()
    private val interaccionesListeners = mutableListOf<(Interaccion) -> Unit>()
    private val invitacionesListeners = mutableListOf<(InvitacionPlaylist) -> Unit>()
    private val nuevoNoizzyListeners = mutableListOf<(Noizzy, Boolean) -> Unit>()
    private val nuevoNoizzitoListeners = mutableListOf<(NoizzitoData, String) -> Unit>()
    private val webSocketManager = WebSocketManager.getInstance()

    fun init() {
        Log.d("LOGS_NOTIS", "WebSocketEventHandler init llamado")

        webSocketManager.listenToEvent("actualizar-noizzy-ws") { args ->
            Log.d("LOGS_NOTIS", "nuevo noizzy")
            val data = args[0] as JSONObject
            val tipo = data.getString("tipo")
            val nombre = data.getString("nombre")
            val nombreUsuario = data.getString("nombreUsuario")
            val mio = data.getBoolean("mio")
            val fotoPerfil = data.getString("fotoPerfil")
            val id = data.getInt("id")
            val texto = data.getString("texto")
            val fecha = data.getString("fecha")
            val cancionJson = data.optJSONObject("cancion")

            val cancion: CancionNoizzy? = cancionJson?.let {
                CancionNoizzy(
                    id = it.getInt("id"),
                    nombre = it.getString("nombre"),
                    fotoPortada = it.getString("fotoPortada"),
                    nombreArtisticoArtista = it.getString("nombreArtisticoArtista")
                )
            }

            var noizzy = Noizzy(
                tipo = tipo,
                nombre = nombre,
                nombreUsuario = nombreUsuario,
                fotoPerfil = fotoPerfil,
                fecha = fecha,
                id = id,
                texto = texto,
                like = false,
                cancion = cancion,
                num_likes = 0,
                num_comentarios = 0
            )

            // Notificás a todos los que estén escuchando
            nuevoNoizzyListeners.forEach { it(noizzy, mio) }
        }

        webSocketManager.listenToEvent("actualizar-noizzito-ws") { args ->
            Log.d("LOGS_NOTIS", "nuevo noizzito")
            val data = args[0] as JSONObject
            val tipo = data.getString("tipo")
            val nombre = data.getString("nombre")
            val mio = data.getBoolean("mio")
            val nombreUsuario = data.getString("nombreUsuario")
            val fotoPerfil = data.getString("fotoPerfil")
            val idNoizzy = data.getString("noizzy")
            val id = data.getString("id")
            val texto = data.getString("texto")
            val fecha = data.getString("fecha")
            val cancionJson = data.optJSONObject("cancion")

            val cancion: CancionData? = cancionJson?.let {
                CancionData(
                    id = it.getString("id"),
                    nombre = it.getString("nombre"),
                    fotoPortada = it.getString("fotoPortada"),
                    nombreArtisticoArtista = it.getString("nombreArtisticoArtista")
                )
            }

            var noizzito = NoizzitoData(
                id = id,
                tipo = tipo,
                nombre = nombre,
                nombreUsuario = nombreUsuario,
                mio = mio,
                fotoPerfil = fotoPerfil,
                fecha = fecha,
                texto = texto,
                like = false,
                cancion = cancion,
                num_likes = 0,
                num_comentarios = 0
            )

            // Notificás a todos los que estén escuchando
            nuevoNoizzitoListeners.forEach { it(noizzito, idNoizzy) }
        }


        webSocketManager.listenToEvent("nuevo-seguidor-ws") { args ->
            Log.d("LOGS_NOTIS", "llegó nuevo-seguidor-ws")

            val data = args[0] as JSONObject
            val seguidor = Seguidor(
                nombre = data.getString("nombre"),
                nombreUsuario = data.getString("nombreUsuario"),
                fotoPerfil = data.getString("fotoPerfil"),
                tipo = data.getString("tipo")
            )

            // Notificás a todos los que estén escuchando
            seguidoresListeners.forEach { it(seguidor) }

            // Esto siempre se hace, estés donde estés
            Preferencias.guardarValorBooleano("hay_notificaciones_seguidores", true)
            Preferencias.guardarValorBooleano("hay_notificaciones", true)
        }

        webSocketManager.listenToEvent("novedad-musical-ws") { args ->
            Log.d("LOGS_NOTIS", "llegó novedad-musical-ws")

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

            // Notificás a todos los que estén escuchando
            novedadesListeners.forEach { it(novedad) }

            // Esto siempre se hace, estés donde estés
            Preferencias.guardarValorBooleano("hay_notificaciones_novedades", true)
            Preferencias.guardarValorBooleano("hay_notificaciones", true)
        }

        webSocketManager.listenToEvent("invite-to-playlist-ws") { args ->
            Log.d("LOGS_NOTIS", "llegó invite-to-playlist-ws")

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

            // Notificás a todos los que estén escuchando
            invitacionesListeners.forEach { it(invitacion) }

            // Esto siempre se hace, estés donde estés
            Preferencias.guardarValorBooleano("hay_notificaciones_invitaciones", true)
            Preferencias.guardarValorBooleano("hay_notificaciones", true)
        }

        webSocketManager.listenToEvent("nueva-interaccion-ws") { args ->
            Log.d("LOGS_NOTIS", "llegó nueva-interaccion-ws")

            val data = args[0] as JSONObject
            val nombreUsuario = data.getString("nombreUsuario")
            val noizzy = data.getString("noizzy")
            val noizzito = data.getString("noizzito")
            val texto = data.getString("texto")
            val tipo = data.getString("tipo")

            val interaccion = Interaccion (
                nombreUsuario = nombreUsuario,
                noizzy = noizzy,
                noizzito = noizzito,
                texto = texto,
                tipo = tipo
            )

            // Notificás a todos los que estén escuchando
            interaccionesListeners.forEach { it(interaccion) }

            // Esto siempre se hace, estés donde estés
            Preferencias.guardarValorBooleano("hay_notificaciones_interacciones", true)
            Preferencias.guardarValorBooleano("hay_notificaciones", true)
        }
    }

    fun registrarListenerSeguidor(listener: (Seguidor) -> Unit) {
        seguidoresListeners.add(listener)
    }

    fun eliminarListenerSeguidor(listener: (Seguidor) -> Unit) {
        seguidoresListeners.remove(listener)
    }

    fun registrarListenerNovedad(listener: (Novedad) -> Unit) {
        novedadesListeners.add(listener)
    }

    fun eliminarListenerNovedad(listener: (Novedad) -> Unit) {
        novedadesListeners.remove(listener)
    }

    fun registrarListenerInteraccion(listener: (Interaccion) -> Unit) {
        interaccionesListeners.add(listener)
    }

    fun eliminarListenerInteraccion(listener: (Interaccion) -> Unit) {
        interaccionesListeners.remove(listener)
    }

    fun registrarListenerInvitacion(listener: (InvitacionPlaylist) -> Unit) {
        invitacionesListeners.add(listener)
    }

    fun eliminarListenerInvitacion(listener: (InvitacionPlaylist) -> Unit) {
        invitacionesListeners.remove(listener)
    }

    fun registrarListenerNoizzy(listener: (Noizzy, Boolean) -> Unit) {
        nuevoNoizzyListeners.add(listener)
    }

    fun eliminarListenerNoizzy(listener: (Noizzy, Boolean) -> Unit) {
        nuevoNoizzyListeners.remove(listener)
    }

    fun registrarListenerNoizzito(listener: (NoizzitoData, String) -> Unit) {
        nuevoNoizzitoListeners.add(listener)
    }

    fun eliminarListenerNoizzito(listener: (NoizzitoData, String) -> Unit) {
        nuevoNoizzitoListeners.remove(listener)
    }
}