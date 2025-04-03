package com.example.myapplication.io.response

import com.google.gson.annotations.SerializedName

data class PendientesResponse(
    @SerializedName("pendientes") val pendientes: List<PendienteItem>
)

data class PendienteItem(
    @SerializedName("correo") val correo: String,
    @SerializedName("nombreArtistico") val nombreArtistico: String
)