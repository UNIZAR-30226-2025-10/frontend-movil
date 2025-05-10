package com.example.myapplication.io.response

data class CancionActualResponse(
    val respuestaHTTP: Int,
    val cancion: CancionActual?,
    val coleccion: ColeccionActual?
)

data class CancionActual(
    val id: String?,
    val audio: String?,
    val nombre: String?,
    val nombreArtisticoArtista: String?,
    val nombreUsuarioArtista: String?,
    val progreso: Double?,
    val featuring: List<String>?,
    val fav: Boolean?,
    val fotoPortada: String?
)

data class ColeccionActual(
    val id: String?,
    val orden: List<String>?,
    val ordenNatural: List<String>?,
    val index: Int?,
    val modo: String?
)
