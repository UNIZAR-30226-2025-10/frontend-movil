package com.example.myapplication.io.response

data class CancionesFavsArtistaResponse(
    val canciones_favoritas: List<cancionFavoritaArtista> ,
)

data class cancionFavoritaArtista(
    val id: String,
    val nombre: String,
    val fotoPortada: String,
    val album: String,
    val duracion: String,
    val featuring: List<String>,
    val fecha: String
)