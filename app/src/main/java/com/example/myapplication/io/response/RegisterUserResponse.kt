package com.example.myapplication.io.response

class RegisterUserResponse (
    val token: String?,
    val respuestaHTTP: Int,
    val correo: String?,
    val nombreUsuario: String?,
    val fotoUsuario: String?,
    val esOyente: Boolean?,
    val esArtista: Boolean?,
    val esPendiente: Boolean?,
    val nombreArtistico: String?
)
