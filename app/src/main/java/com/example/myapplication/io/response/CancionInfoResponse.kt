package com.example.myapplication.io.response

data class CancionInfoResponse (
    val respuestaHTTP: Int,
    val nombre : String,
    val nombreArtisticoArtista : String,
    val fotoPortada: String
)