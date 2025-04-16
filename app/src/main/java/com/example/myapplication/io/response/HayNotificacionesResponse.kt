package com.example.myapplication.io.response

data class HayNotificacionesResponse (
    val respuestaHTTP: Int,
    val invitaciones: Boolean,
    val novedadesMusicales: Boolean,
    val interacciones: Boolean,
    val seguidores: Boolean
)