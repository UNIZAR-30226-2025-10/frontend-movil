package com.example.myapplication.io.response

data class GetEstadisticasPlaylistResponse (
    val n_privadas: Int,
    val playlists_publicas: List<Publicas>
)

class Publicas (
    val id: String,
    val nombre: String,
    val fotoPortada: String,
    val creador: String
)