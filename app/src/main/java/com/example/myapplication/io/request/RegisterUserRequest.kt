package com.example.myapplication.io.request

data class RegisterUserRequest(
    val nombreUsuario: String,
    val correo: String ,
    val contrasenya: String,
    val esOyente: Boolean
)
