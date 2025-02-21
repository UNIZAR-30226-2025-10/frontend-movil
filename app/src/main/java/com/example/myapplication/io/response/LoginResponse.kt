package com.example.myapplication.io.response

data class LoginResponse(
    val error: String?,     //si hay algun error
    val token: String?    //token de autenticación
)