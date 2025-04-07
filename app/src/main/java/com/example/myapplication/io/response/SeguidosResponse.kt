package com.example.myapplication.io.response

data class SeguidosResponse(
    val respuestaHTTP: Int,
    val seguidos: List<Seguidos>,
)

class Seguidos (
    val tipo: String,
    val fotoPerfil: String,
    val nombreUsuario: String,
)