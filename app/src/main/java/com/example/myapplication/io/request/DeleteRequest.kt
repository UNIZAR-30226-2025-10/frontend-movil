package com.example.myapplication.io.request

data class DeleteRequest(
    val nombreUsuario: String?,
    val correo: String? ,
    val contrasenya: String,
)