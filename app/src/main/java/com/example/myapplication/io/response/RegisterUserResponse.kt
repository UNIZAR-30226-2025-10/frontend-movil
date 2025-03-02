package com.example.myapplication.io.response

data class RegisterUserResponse(
    val respuestaHTTP: Int,
    val token: String?,
    val oyente: Oyente?
)

class Oyente (
    val correo: String,
    val fotoPerfil: String,
    val nombreUsuario: String,
    val tipo: String,
    val volumen: Int,
)