package com.example.myapplication.io.response

data class HistorialRecientesResponse(val respuestaHTTP: Int, val historial_colecciones: List<HRecientes>)

data class HistorialEscuchasResponse(val respuestaHTTP: Int, val historial_canciones: List<HCancion>)

data class PlaylistsResponse(val respuestaHTTP: Int, val playlists: List<MisPlaylist>)

data class RecomendacionesResponse(val respuestaHTTP: Int, val canciones_recomendadas: List<Recomendaciones>)

data class HRecientes(
    val id: String,
    val nombre: String,
    val fotoPortada: String,
    val autor: String
)

data class HCancion(
    val id: String,
    val nombre: String,
    val nombreArtisticoArtista: String,
    val fotoPortada: String
)

data class MisPlaylist(
    val id: String,
    val fotoPortada: String,
    val nombre: String
)

data class Recomendaciones(
    val id: String,
    val fotoPortada: String,
    val nombre: String
)
