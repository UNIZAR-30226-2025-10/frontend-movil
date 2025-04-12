package com.example.myapplication.io.request

data class UpdatePlaylistRequest(
    val id: String,
    val nuevaFoto: String?,
    val nuevoNombre: String?
)