package com.example.myapplication.io.response

data class GetInteraccionesResponse (
    val resultado: List<Interaccion>
)

data class Interaccion(
    val nombreUsuario: String,
    val noizzy: String,
    val noizzito: String?,
    val texto: String,
    val tipo: String
)