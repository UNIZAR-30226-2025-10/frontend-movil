package com.example.myapplication.io.response

data class MisAlbumesResponse(val respuestaHTTP: Int, val albumes: List<MiAlbum>)

data class MiAlbum (
    val id: String,
    val nombre: String,
    val fotoPortada: String
)