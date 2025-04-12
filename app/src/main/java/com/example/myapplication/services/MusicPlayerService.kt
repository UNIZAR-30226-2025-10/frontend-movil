package com.example.myapplication.services

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder

class MusicPlayerService : Service() {

    private lateinit var mediaPlayer: MediaPlayer
    private val binder = MusicBinder()

    inner class MusicBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun playOrPause() {
        if (::mediaPlayer.isInitialized) {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            } else {
                mediaPlayer.start()
            }
        }
    }


    fun seekTo(millis: Int) {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.seekTo(millis)
        }
    }

    fun playSong(songUrl: String) {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.reset()
        } else {
            mediaPlayer = MediaPlayer()
        }

        mediaPlayer.setDataSource(songUrl)
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener {
            it.start()
        }
    }

    fun pause() {
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    fun resume() {
        if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    fun getProgress(): Int {
        return if (::mediaPlayer.isInitialized) mediaPlayer.currentPosition else 0
    }

    fun getDuration(): Int {
        return if (::mediaPlayer.isInitialized) mediaPlayer.duration else 0
    }

    fun isPlaying(): Boolean {
        return ::mediaPlayer.isInitialized && mediaPlayer.isPlaying
    }

    override fun onDestroy() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        super.onDestroy()
    }
}
