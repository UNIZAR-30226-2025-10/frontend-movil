package com.example.myapplication.io.response

data class CancionesArtistaResponse(
    val canciones: List<CancionesArtista>
)

data class CancionesArtista(
    val id: Int,
    val nombre: String,
    val fotoPortada: String
)