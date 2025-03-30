package com.example.myapplication.io.response


data class PlaylistResponse(
    val respuestaHTTP: Int,
    val playlist: PlaylistP,
    val canciones: List<CancionP>,
    val rol: String
)

data class PlaylistP(
    val nombrePlaylist: String,
    val fotoPortada: String,
    val duracion: Int,
    val creador: String,
    val colaboradores: List<String>,
    val privacidad: String
)

data class CancionP(
    val id: String,
    val nombre: String,
    val nombreArtisticoArtista: String,
    val featuring: List<String>,
    val reproducciones: Int,
    val duracion: Int,
    val fav: Boolean,
    val nombreUsuarioArtista: String,
    val fotoPortada: String,
    val fecha: String
)