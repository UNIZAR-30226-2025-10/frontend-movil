package com.example.myapplication.io.response

data class RegisterArtistResponse(
    val respuestaHTTP: Int,
    val pendiente: Pendiente?,
    val tipo: String,
    val token: String?

)

class Pendiente(
    val correo: String,
    val nombreUsuario: String
)