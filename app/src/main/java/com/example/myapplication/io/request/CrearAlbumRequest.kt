package com.example.myapplication.io.request

data class CrearAlbumRequest (
    val nombre_album: String,
    val fotoPortada: String,
    val notificar: Boolean
)