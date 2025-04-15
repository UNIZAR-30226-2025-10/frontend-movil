package com.example.myapplication.io.request

data class AddToPlaylistRequest(
    val cancion: String,
    val playlist: String
)