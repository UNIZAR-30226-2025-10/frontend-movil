package com.example.myapplication.io.response

data class GetPlaylistOyenteResponse(
    val playlists: List<PlaylistOyente>,
    val n_playlists: Int
)

data class PlaylistOyente(
    val id: String,
    val fotoPortada: String?,
    val nombre: String
)
