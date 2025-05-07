package com.example.myapplication.io.request

data class EditarPerfilArtistaRequest(
    val fotoPerfil : String?,
    val nombreUsuario: String?,
    val nombreArtistico: String?,
    val biografia: String?
)