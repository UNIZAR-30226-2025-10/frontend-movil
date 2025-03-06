package com.example.myapplication.io.response

data class DeleteResponse(
    val respuestaHTTP: Int,
    val tipo: String,
    val token: String?,
    val usuario: Usuario?
)

