package com.example.myapplication.io.response

data class GetEstadisticasFavsResponse (
    val oyentes_favs: List<PersonasLike>
)

class PersonasLike (
    val nombreUsuario: String,
    val fotoPerfil: String
)