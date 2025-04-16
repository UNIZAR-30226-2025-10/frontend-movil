package com.example.myapplication.io.request

import com.example.myapplication.io.response.PlaylistP

data class InvitarPlaylistRequest (
    val playlist: PlaylistP?,
    val nombreUsuario: String?
)