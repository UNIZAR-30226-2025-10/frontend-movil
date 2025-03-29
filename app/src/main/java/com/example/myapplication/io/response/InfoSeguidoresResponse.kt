package com.example.myapplication.io.response

data class InfoSeguidoresResponse (
    val respuestaHTTP: Int,
    val nombreUsuario : String,
    val numSeguidos : Int,
    val numSeguidores : Int
)

