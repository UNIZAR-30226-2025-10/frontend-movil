package com.example.myapplication.io.response

data class SeguidoresResponse(
    val respuestaHTTP: Int,
    val seguidores: List<Seguidores>,
)

class Seguidores (
    val tipo: String,
    val fotoPerfil: String,
    val nombreUsuario: String,
    var followBack: Boolean,
)