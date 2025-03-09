package com.example.myapplication.io.response

data class HistorialCancionesResponse(val historial_canciones: List<Cancion>)

data class HistorialEscuchasResponse(val historial_Escuchas: List<Cancion>)

data class PlaylistsResponse(val playlists: List<Playlist>)

data class RecomendacionesResponse(val canciones_recomendadas: List<Cancion>)

