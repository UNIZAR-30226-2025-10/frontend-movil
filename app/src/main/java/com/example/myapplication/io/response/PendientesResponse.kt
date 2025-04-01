package com.example.myapplication.io.response

data class PendientesResponse(
    val respuestaHTTP: Int,
    val pendientes: List<PendienteItem>
)

data class PendienteItem(
    val correo: String,
    val nombreArtistico: String
)