package com.example.myapplication.io.response

data class GetEtiquetasResponse(
    val respuestaHTTP: Int,
    val tags: List<String>
)