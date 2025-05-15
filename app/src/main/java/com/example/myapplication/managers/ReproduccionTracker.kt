package com.example.myapplication.managers

import android.content.Context
import android.os.Handler
import android.os.Looper

object ReproduccionTracker {

    private var currentSongId: String? = null
    private var secondsPlayed = 0
    private var handler: Handler? = null
    private var runnable: Runnable? = null
    private var isPaused = false

    private const val TARGET_SECONDS = 30

    fun startTracking(context: Context, songId: String, onReached: () -> Unit) {
        stopTracking() // Cancela cualquier tracking previo

        currentSongId = songId
        secondsPlayed = 0
        isPaused = false

        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                if (!isPaused) {
                    secondsPlayed++
                    if (secondsPlayed >= TARGET_SECONDS) {
                        onReached()
                        stopTracking()
                        return
                    }
                }
                handler?.postDelayed(this, 1000) // Repetir cada segundo
            }
        }
        handler?.post(runnable!!)
    }

    fun pauseTracking() {
        isPaused = true
    }

    fun resumeTracking() {
        isPaused = false
    }

    fun stopTracking() {
        handler?.removeCallbacks(runnable!!)
        handler = null
        runnable = null
        currentSongId = null
        secondsPlayed = 0
        isPaused = false
    }

    fun isTrackingSong(songId: String): Boolean {
        return currentSongId == songId
    }
}

