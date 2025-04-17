package com.example.myapplication.io.request

import com.example.myapplication.io.response.Cancion

data class PostNoizzitoRequest (
    val texto: String,
    val noizzy: Int,
    val cancion: String?
)


