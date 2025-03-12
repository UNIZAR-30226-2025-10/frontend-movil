package com.example.myapplication.io.request

data class PlayPauseRequest(
    val id: String,
    val reproduciendo: Boolean,
    val progreso: Int
)