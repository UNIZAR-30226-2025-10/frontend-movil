package com.example.myapplication.io.response

data class BuscadorResponse(
    val respuestaHTTP: Int,
    val canciones: List<Cancion>,
    val albumes: List<Album>,
    val playlists: List<Playlist>,
    val artistas: List<Artista>,
    val perfiles: List<Perfil>
)

class Cancion(
    val fotoPortada: String,
    val id: Int,
    val nombre: String,
    val nombreArtisticoArtista: String
)
class Album(
    val fotoPortada: String,
    val id: Int,
    val nombre: String,
    val nombreArtisticoArtista: String
)
class Playlist(
    val fotoPortada: String,
    val id: Int,
    val nombre: String,
    val nombreUsuarioCreador: String
)
class Artista(
    val fotoPerfil: String,
    val nombreArtistico: String,
    val nombreUsuario: String
)
class Perfil(
    val fotoPerfil: String,
    val nombreUsuario: String
)