package com.example.myapplication.io.response

data class GetDatosOyenteResponse (
    val respuestaHTTP: Int,
    val oyente : OtroPerfil,
    val ultimoNoizzy : Noizzi,
)

data class OtroPerfil(
    val nombreUsuario: String,
    val numSeguidos: String,
    val numSeguidores: String,
    val siguiendo: Boolean,
    val fotoPerfil: String,
)

data class Noizzi(
    val fotoPerfil: String,
    val nombreUsuario: String
)