package com.example.myapplication.io.response

data class DiscografiaAlbumArtistaResponse(
    val albumes: List<AlbumArtista>
)

data class AlbumArtista(
    val id: Int,
    val nombre: String,
    val fotoPortada: String
)