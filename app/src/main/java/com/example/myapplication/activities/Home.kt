package com.example.myapplication.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.ItemAdapter
import com.example.myapplication.R

class Home : AppCompatActivity() {

    /*private lateinit var recentlyListenedAdapter: SongAdapter
    private lateinit var playlistsAdapter: PlaylistAdapter
    private lateinit var latestSongsAdapter: SongAdapter
    private lateinit var recommendationsAdapter: SongAdapter

    private lateinit var recyclerViewRecentlyListened: RecyclerView
    private lateinit var recyclerViewPlaylists: RecyclerView
    private lateinit var recyclerViewLatestSongs: RecyclerView
    private lateinit var recyclerViewRecommendations: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Inicializamos los RecyclerViews
        recyclerViewRecentlyListened = findViewById(R.id.recentlyListened)
        recyclerViewPlaylists = findViewById(R.id.playlistsRecyclerView)
        recyclerViewLatestSongs = findViewById(R.id.latestSongsRecyclerView)
        recyclerViewRecommendations = findViewById(R.id.recommendationsRecyclerView)

        // Configuramos los RecyclerViews
        recyclerViewRecentlyListened.layoutManager = LinearLayoutManager(this)
        recyclerViewPlaylists.layoutManager = LinearLayoutManager(this)
        recyclerViewLatestSongs.layoutManager = LinearLayoutManager(this)
        recyclerViewRecommendations.layoutManager = LinearLayoutManager(this)

        // Cargar datos simulados
        loadSimulatedData()
    }

    private fun loadSimulatedData() {
        // Simulamos los datos de cada sección
        val recentSongs = listOf(
            Song("Song 1", "Artist A", "3:15"),
            Song("Song 2", "Artist B", "4:00")
        )

        val playlists = listOf(
            Playlist("My Playlist", 10),
            Playlist("Top Hits", 20)
        )

        val latestSongs = listOf(
            Song("Song 3", "Artist C", "2:45"),
            Song("Song 4", "Artist D", "3:30")
        )

        val recommendations = listOf(
            Song("Song 5", "Artist E", "3:10"),
            Song("Song 6", "Artist F", "4:20")
        )

        // Asignamos los datos a los adaptadores
        recentlyListenedAdapter = SongAdapter(recentSongs)
        playlistsAdapter = PlaylistAdapter(playlists)
        latestSongsAdapter = SongAdapter(latestSongs)
        recommendationsAdapter = SongAdapter(recommendations)

        // Establecemos los adaptadores a los RecyclerViews
        recyclerViewRecentlyListened.adapter = recentlyListenedAdapter
        recyclerViewPlaylists.adapter = playlistsAdapter
        recyclerViewLatestSongs.adapter = latestSongsAdapter
        recyclerViewRecommendations.adapter = recommendationsAdapter
    }*/
}