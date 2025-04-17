package com.example.myapplication.io.response

data class DatosArtistaResponse(
    val artista: DatosArtista
)

data class DatosArtista(
    val nombreUsuario: String,
    val nombreArtistico: String,
    val biografia: String,
    val numSeguidos: Int,
    var numSeguidores: Int,
    val siguiendo: Boolean,
    val fotoPerfil: String
)