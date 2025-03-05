package com.example.myapplication.io.response

data class VerifyArtistResponse(
    val respuestaHTTP: Int,
    val artista_valido: Artista?,
    val tipo: String,
    val token: String
)

class Artista (
    val biografia: String,
    val correo: String,
    val fotoPerfil: String,
    val nombreArtistico: String,
    val nombreUsuario: String,
    val volumen: Int
)

