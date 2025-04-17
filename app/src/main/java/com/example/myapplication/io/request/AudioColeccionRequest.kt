package com.example.myapplication.io.request

data class AudioColeccionRequest(
    val coleccion: String,
    val modo: String,
    val orden: List<String>,
    val index: Int
)