package com.example.myapplication.io.response

data class EstadisticasAlbumResponse (
    val respuestaHTTP: Int,
    val nombre: String,
    val fotoPortada: String,
    val nombreArtisticoArtista: String,
    val fechaPublicacion: String,
    val duracion: Int,
    val reproducciones: Int,
    val nPlaylists: Int,
    val favs: Int,
    val canciones: List<CancionEst>
)

class CancionEst (
    val id: String,
    val fotoPortada: String,
    val nombre: String,
    val duracion: Int,
    val fechaPublicacion: String,
    val reproducciones: Int,
    val puesto: Int,
    val nPlaylists: Int,
    val favs: Int
)