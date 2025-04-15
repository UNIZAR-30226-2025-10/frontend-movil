package com.example.myapplication.io.response

data class InfoPerfilArtistaResponse (
    val respuestaHTTP: Int,
    val nombre : String,
    val nombreArtistico : String,
    val numSeguidos : Int,
    val numSeguidores : Int,
    val biografia: String,
    val fotoPerfil: String
)

