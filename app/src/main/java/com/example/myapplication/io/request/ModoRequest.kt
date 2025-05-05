package com.example.myapplication.io.request

data class ModoRequest(
    val modo: String,
    val orden: List<String>,
    val index: Int
)