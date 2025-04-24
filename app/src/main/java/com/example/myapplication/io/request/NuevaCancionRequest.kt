package com.example.myapplication.io.request

import android.net.Uri

data class NuevaCancionRequest (
    val nombre: String,
    val duracion: Int,
    val audio_file: Uri,
    val tags: List<String>,
    val artistasFt: String?
)