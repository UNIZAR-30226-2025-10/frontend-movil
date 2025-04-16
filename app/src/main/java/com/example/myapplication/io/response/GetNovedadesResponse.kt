package com.example.myapplication.io.response

data class GetNovedadesResponse (
    val resultado: List<Novedad>
)

data class Novedad (
    val id: String,
    val nombre: String,
    val tipo: String,
    val fotoPortada: String,
    val nombreArtisticoArtista: String,
    val featuring: List<String>
)
