package com.example.myapplication.io.response

data class GetSignatureResponse(
    val respuestaHTTP: Int,
    val signature: String,
    val api_key: String,
    val timestamp: Long,
    val cloud_name: String
)