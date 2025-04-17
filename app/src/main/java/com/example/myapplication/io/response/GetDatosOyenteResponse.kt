package com.example.myapplication.io.response

data class GetDatosOyenteResponse (
    val respuestaHTTP: Int,
    val oyente : OtroPerfil,
    val ultimoNoizzy : LastNoizzy,
)

data class OtroPerfil(
    val nombreUsuario: String,
    val numSeguidos: String,
    val numSeguidores: String,
    val siguiendo: Boolean,
    val fotoPerfil: String,
)

data class LastNoizzy (
    val fecha: String,
    val id: Int,
    val texto: String,
    var like: Boolean,
    val cancion: CancionLastNoizzy?,
    var num_likes: Int,
    var num_comentarios: Int
)

data class CancionLastNoizzy (
    val id: Int,
    val nombre: String,
    val fotoPortada: String,
    val nombreArtisticoArtista: String
)