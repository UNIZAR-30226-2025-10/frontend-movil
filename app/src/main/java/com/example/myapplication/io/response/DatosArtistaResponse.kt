package com.example.myapplication.io.response

data class DatosArtistaResponse(
    val artista: DatosArtista
)

data class DatosArtista(
    val nombreUsuario: String,
    val nombreArtistico: String,
    val biografia: String,
    val numSeguidos: Int,
    val numSeguidores: Int,
    val siguiendo: Boolean,
    val fotoPerfil: String
)