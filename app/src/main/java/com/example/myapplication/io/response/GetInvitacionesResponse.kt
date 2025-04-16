package com.example.myapplication.io.response

data class GetInvitacionesResponse (
    val respuestaHTTP: Int,
    val invitaciones: List<InvitacionPlaylist>
)

class InvitacionPlaylist (
    val id: String,
    val nombre: String,
    val nombreUsuario: String,
    val fotoPortada: String
)