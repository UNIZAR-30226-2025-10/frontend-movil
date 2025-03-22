package com.example.myapplication.io.request

data class PlayPauseRequest(
    val reproduciendo: Boolean,
    val progreso: Int
)