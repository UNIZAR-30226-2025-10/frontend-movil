package com.example.myapplication.io.request

data class RegisterArtistRequest(
    val correo: String ,
    val nombreUsuario: String,
    val contrasenya: String,
    val nombreArtistico: String
)
