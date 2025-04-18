package com.example.myapplication.io.response

data class DatosAlbumResponse(
    val nombre: String,
    val fotoPortada: String,
    val nombreArtisticoArtista: String,
    val fechaPublicacion: String,
    val duracion: Int,
    val canciones: List<CancionesAlbum>,
    val favs: Int
)

data class CancionesAlbum(
    val id: String,
    val fotoPortada: String,
    val nombre: String,
    val duracion: Int,
    val fechaPublicacion: String,
    var fav: Boolean,
    val featuring: List<String>,
    val puesto: Int
)