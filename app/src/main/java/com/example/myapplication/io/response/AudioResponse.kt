package com.example.myapplication.io.response

import okhttp3.ResponseBody

data class AudioResponse(
    val respuestaHTTP: Int,
    val audio: String,
    val nombreUsuarioArtista: String,
    val fav: Boolean
)