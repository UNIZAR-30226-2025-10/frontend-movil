package com.example.myapplication.io.response

data class GetNuevosSeguidoresResponse (
    val resultado: List<Seguidor>
)

data class Seguidor (
    val nombre: String,
    val nombreUsuario: String,
    val fotoPerfil: String,
    val tipo: String
)