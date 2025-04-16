package com.example.myapplication.io.request

import com.example.myapplication.io.response.CancionP
import com.example.myapplication.io.response.PlaylistP

class DeleteFromPlaylistRequest (
    val cancion: String,
    val playlist: String
)

