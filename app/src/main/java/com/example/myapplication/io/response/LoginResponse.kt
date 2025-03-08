package com.example.myapplication.io.response

data class LoginResponse(
    val respuestaHTTP: Int,
    val tipo: String,
    val token: String?,
    val usuario: Usuario?
)

class Usuario (
    val correo: String,
    val fotoPerfil: String,
    val nombreUsuario: String,
    val volumen: Int,
)