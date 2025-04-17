package com.example.myapplication.io.response

data class PopularesArtistaResponse(
    val canciones_populares: List<CancionPopulares>
)

data class CancionPopulares(
    val id: String,
    val nombre: String,
    val reproducciones: Int,
    val featuring: List<String>,
    val duracion: String,
    var fav: Boolean,
    val fotoPortada: String
)
