package com.example.myapplication.io.response

data class GetMisAlbumesResponse(
    val respuestaHTTP: Int,
    val albumes: List<MisAlbums>,
)

data class MisAlbums(
    val id: String,
    val nombre: String,
    val fotoPortada: String,
)