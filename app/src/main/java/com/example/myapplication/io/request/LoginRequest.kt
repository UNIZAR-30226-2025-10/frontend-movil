package com.example.myapplication.io.request

data class LoginRequest(
    val nombreUsuario: String?,
    val correo: String? ,
    val contrasenya: String,
)