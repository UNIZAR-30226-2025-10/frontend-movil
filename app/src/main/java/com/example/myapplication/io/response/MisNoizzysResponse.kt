package com.example.myapplication.io.response

data class MisNoizzysResponse(
    val noizzys: List<Noizzy>
)

data class Noizzy (
    var nombreUsuario: String,
    val fotoPerfil: String,
    val fecha: String,
    val id: Int,
    val texto: String,
    var like: Boolean,
    val cancion: CancionNoizzy?,
    var num_likes: Int,
    var num_comentarios: Int
)

data class CancionNoizzy (
    val id: Int,
    val nombre: String,
    val fotoPortada: String,
    val nombreArtisticoArtista: String
)
