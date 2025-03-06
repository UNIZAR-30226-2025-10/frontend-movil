package com.example.myapplication.io.response

data class LogOutResponse(
    val respuestaHTTP: Int,
    val tipo: String,
    val token: String?,
    val usuario: Usuario?
)
