package com.example.myapplication.io.request

data class CrearCancionRequest (
    val nombre: String,
    val duracion: Int,
    val audio_url: String,
    val album_id: String?,
    val tags: List<String>,
    val artistasFt: List<String>
)