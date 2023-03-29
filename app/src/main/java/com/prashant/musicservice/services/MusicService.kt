package com.prashant.musicservice.services

import android.app.Service
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi

class MusicService : Service(), MediaPlayer.OnCompletionListener,
    MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnPreparedListener {

    private var mediaPlayer: MediaPlayer? = null
    private var songList: List<String>? = null
    private var currentSongIndex: Int = 0
    var isPaused: Boolean = false
    private var isPrepared: Boolean = false
    private var isSeeking: Boolean = false
    private var seekTarget: Int = 0

    private val binder = MusicBinder()


    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    companion object {
        const val ACTION_PLAYBACK_STARTED = "com.prashant.musicservice.action.PLAYBACK_STARTED"
        const val EXTRA_SONG_TITLE = "com.prashant.musicservice.extra.SONG_TITLE"
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val songUriList = intent?.getStringArrayListExtra(SONG_LIST)
        if (songUriList != null) {
            songList = songUriList
            Log.e("TAG", "startPlaying: $songList")
            startPlaying(songList!![currentSongIndex])
        }
        return START_STICKY
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }

    override fun onCompletion(mediaPlayer: MediaPlayer?) {
        if (currentSongIndex < (songList?.size?.minus(1) ?: 0)) {
            currentSongIndex++
            startPlaying(songList!![currentSongIndex])
        } else {
            // All songs in the list have been played
            stopSelf()
        }
    }

    override fun onSeekComplete(mediaPlayer: MediaPlayer?) {
        isSeeking = false
        if (isPrepared) {
            mediaPlayer?.start()
        }
    }

    override fun onPrepared(mediaPlayer: MediaPlayer?) {
        isPrepared = true
        if (!isSeeking) {
            mediaPlayer?.start()
            sendPlaybackStartedBroadcast()
        } else {
            mediaPlayer?.seekTo(seekTarget)
            isSeeking = false
        }
    }

    private fun startPlaying(songUri: String) {
        mediaPlayer?.stop()
        mediaPlayer?.reset()
        Log.e("TAG", "play: $songUri")
        mediaPlayer?.setDataSource(songUri)
        mediaPlayer?.setOnCompletionListener(this@MusicService)
        mediaPlayer?.setOnSeekCompleteListener(this@MusicService)
        mediaPlayer?.setOnPreparedListener(this@MusicService)
        mediaPlayer?.prepareAsync()
        isPaused = false
        isPrepared = false
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            isPaused = true
        }
    }

    fun resume() {
        if (isPaused) {
            mediaPlayer?.start()
            isPaused = false
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.reset()
        isPaused = false
        currentSongIndex = 0
        stopSelf()
    }

    fun next() {
        if (currentSongIndex < (songList?.size?.minus(1) ?: 0)) {
            currentSongIndex++
            startPlaying(songList!![currentSongIndex])
        }
    }

    fun previous() {
        if (currentSongIndex > 0) {
            currentSongIndex--
            startPlaying(songList!![currentSongIndex])
        }
    }

    fun skipTo(position: Int) {
        if (isPrepared) {
            mediaPlayer?.seekTo(position)
        } else {
            isSeeking = true
            seekTarget = position
        }
    }

    private fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }

    private fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    private fun trackInfo(): Array<out MediaPlayer.TrackInfo>? {
        return mediaPlayer?.trackInfo
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun info() {

        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(songList?.get(currentSongIndex) ?: "", HashMap())

        val title =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)

        // Retrieve the artist of the audio
        val artist =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)

        // Retrieve the album of the audio
        val album =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)

        // Retrieve the duration of the audio
        val duration =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        // Retrieve the duration of the audio
        val image =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_IMAGE_PRIMARY)
        val count=mediaMetadataRetriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_IMAGE_COUNT
        )

        // Release the resources used by the MediaMetadataRetriever
        mediaMetadataRetriever.release()
        Log.e(
            "TAG",
            "info: title:$title, artist: $artist, album: $album, duration:$duration,  image: $image,  imageCount:  $count",
        )
    }

    private fun sendPlaybackStartedBroadcast() {
        val currentSongTitle =
            "info: ${trackInfo()?.get(0)?.format} ${getCurrentPosition()} ${getDuration()}"
        val intent = Intent(ACTION_PLAYBACK_STARTED)
        intent.putExtra(EXTRA_SONG_TITLE, currentSongTitle)
        sendBroadcast(intent)
    }
}