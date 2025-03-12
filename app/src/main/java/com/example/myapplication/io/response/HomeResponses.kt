package com.example.myapplication.io.response

data class HistorialArtistasResponse(val respuestaHTTP: Int, val historial_artistas: List<HArtistas>)

data class HistorialEscuchasResponse(val respuestaHTTP: Int, val historial_escuchas: List<HCancion>)

data class PlaylistsResponse(val respuestaHTTP: Int, val mis_playlists: List<MisPlaylist>)

data class RecomendacionesResponse(val respuestaHTTP: Int, val canciones_recomendadas: List<Recomendaciones>)


class HArtistas(
    val nombreUsuario: String,
    val nombreArtistico: String,
    val fotoPerfil: String
)

class HCancion(
    val id: Int,
    val nombre: String,
    val nombreArtisticoArtista: String,
    val fotoPortada: String
)


class MisPlaylist(
    val id: Int,
    val nombre: String,
    val fotoPortada: String
)

class Recomendaciones(
    val id: Int,
    val fotoPortada: String,
    val nombre: String
)
