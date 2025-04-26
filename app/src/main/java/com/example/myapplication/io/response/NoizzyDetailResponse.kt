package com.example.myapplication.io.response

// Clases de datos para parsear la respuesta
data class NoizzyDetailResponse(
    val id: String,
    val fotoPerfil: String,
    val nombreUsuario: String,
    val mio: Boolean,
    val fecha: String,
    val texto: String,
    var num_likes: Int,
    var num_comentarios: Int,
    var like: Boolean,
    val cancion: CancionData?,
    val noizzitos: List<NoizzitoData>
)

data class CancionData(
    val id: String,
    val nombre: String,
    val nombreArtisticoArtista: String,
    val fotoPortada: String
)

data class NoizzitoData(
    val id: String,
    val nombreUsuario: String,
    val mio: Boolean,
    val fotoPerfil: String,
    val fecha: String,
    val texto: String,
    var like: Boolean,
    val cancion: Cancion?,
    var num_likes: Int,
    var num_comentarios: Int
)
