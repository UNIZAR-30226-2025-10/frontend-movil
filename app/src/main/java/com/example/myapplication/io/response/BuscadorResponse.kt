package com.example.myapplication.io.response

// Modelo de respuesta de la API
data class BuscadorResponse(
    val respuestaHTTP: Int,
    val canciones: List<Cancion>,
    val albumes: List<Album>,
    val playlists: List<Playlist>,
    val artistas: List<Artista>,
    val perfiles: List<Perfil>
)

// Clases de datos para cada entidad
data class Cancion(
    val fotoPortada: String,
    val id: String,
    val nombre: String,
    val nombreArtisticoArtista: String
)

data class Album(
    val fotoPortada: String,
    val id: String,
    val nombre: String,
    val nombreArtisticoArtista: String
)

data class Playlist(
    val fotoPortada: String,
    val id: String,
    val nombre: String,
    val nombreUsuarioCreador: String
)

/*data class Artista(  // La descomenté y la convertí en data class
    val fotoPerfil: String,
    val nombreArtistico: String,
    val nombreUsuario: String
)*/

data class Perfil(
    val fotoPerfil: String,
    val nombreUsuario: String
)

// Clases selladas para usar en el adaptador
sealed class SearchResultItem {
    data class CancionItem(val cancion: List<Cancion>) : SearchResultItem()
    data class AlbumItem(val album: List<Album>) : SearchResultItem()
    data class ArtistaItem(val artista: List<Artista>) : SearchResultItem()
    data class PlaylistItem(val playlist: List<Playlist>) : SearchResultItem()
    data class PerfilItem(val perfil: List<Perfil>) : SearchResultItem()
    data class HeaderItem(val title: String) : SearchResultItem()
}

