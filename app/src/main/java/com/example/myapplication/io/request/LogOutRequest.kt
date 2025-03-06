package com.example.myapplication.io.request

data class LogOutRequest(
    val nombreUsuario: String?,
    val correo: String? ,
    val contrasenya: String,
)