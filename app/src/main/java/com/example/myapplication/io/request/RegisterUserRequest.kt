package com.example.myapplication.io.request

data class RegisterUserRequest(
    val username: String,
    val email: String ,
    val password: String,
    val esOyente: Boolean
)
