package com.example.myapplication.io.request

data class LoginRequest(
    val email: String, //puede ser correo o nombre de usuario
    val password: String
)