package com.example.myapplication.io.response

import com.google.gson.annotations.SerializedName

data class GetSignatureResponse(
    val respuestaHTTP: Int,
    val signature: String,
    @SerializedName("api_key") val apiKey: String,
    val timestamp: Long,
    @SerializedName("cloud_name") val cloudName: String
)