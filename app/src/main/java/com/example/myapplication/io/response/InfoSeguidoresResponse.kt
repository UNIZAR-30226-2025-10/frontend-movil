package com.example.myapplication.io.response

data class InfoSeguidoresResponse (
    val respuestaHTTP: Int,
    val nombre : String,
    val seguidos_count : Int,
    val seguidores_count : Int
)

