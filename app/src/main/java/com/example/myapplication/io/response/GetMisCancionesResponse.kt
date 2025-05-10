package com.example.myapplication.io.response

data class GetMisCancionesResponse(
    val respuestaHTTP: Int,
    val canciones: List<MisCanciones>,
)

data class MisCanciones(
    val id: String,
    val nombre: String,
    val fotoPortada: String,
)