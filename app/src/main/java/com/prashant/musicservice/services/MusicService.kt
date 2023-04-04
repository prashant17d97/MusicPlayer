package com.prashant.musicservice.services

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.prashant.musicservice.MusicModel
import com.prashant.musicservice.R
import com.prashant.musicservice.activity.MainActivity
import com.prashant.musicservice.activity.MainActivity.Companion.weakReference
import com.prashant.musicservice.receivers.PlaybackStartedReceiver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : Service(), MediaPlayer.OnCompletionListener,
    MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnPreparedListener {

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    @Inject
    lateinit var notificationBuilder: NotificationCompat.Builder
    private var mediaPlayer: MediaPlayer? = null
    private var songList: List<String> = listOf()
    private var currentSongIndex: Int = 0
    private var isPaused: Boolean = false
    var isPrepared: Boolean = false
    private var isSeeking: Boolean = false
    private var seekTarget: Int = 0

    private val binder = MusicBinder()


    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
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
        }
        return START_STICKY
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }

    override fun onCompletion(mediaPlayer: MediaPlayer?) {
        if (currentSongIndex < songList.lastIndex) {
            currentSongIndex++
            startPlaying(songList[currentSongIndex])
            (weakReference.get())?.musicUpdate(
                MusicModel(
                    isPaused = isPaused,
                    isPrepared = isPrepared,
                )
            )
        } else {
            // All songs in the list have been played
            (weakReference.get())?.musicUpdate(
                MusicModel(
                    isPaused = false,
                    isPrepared = false,
                )
            )
            stopSelf()
        }
    }

    override fun onSeekComplete(mediaPlayer: MediaPlayer?) {
        isSeeking = false
        if (isPrepared) {
            mediaPlayer?.start()
        }

    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onPrepared(mediaPlayer: MediaPlayer?) {
        isPrepared = true
        if (!isSeeking) {
            mediaPlayer?.start()
            sendPlaybackStartedBroadcast()
            (weakReference.get())?.musicUpdate(
                MusicModel(
                    isPaused = isPaused,
                    isPrepared = isPrepared,
                )
            )
        } else {
            mediaPlayer?.seekTo(seekTarget)
            isSeeking = false
        }
    }

    fun initiatePlayer() {
        if (songList.isNotEmpty()) {
            startPlaying(songList[currentSongIndex])
        } else {
            Toast.makeText(this, "No songs found!", Toast.LENGTH_SHORT).show()
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

    fun next(hasNext: (Boolean) -> Unit) {
        hasNext(
            if (currentSongIndex < songList.size.minus(1)) {
                currentSongIndex++
                startPlaying(songList[currentSongIndex])
                true
            } else {
                false
            }
        )
    }

    fun previous(isSkipping: (Boolean) -> Unit) {
        if (currentSongIndex > 0) {
            currentSongIndex--
            isSkipping(false)
            startPlaying(songList[currentSongIndex])
        } else {
            isSkipping(true)
            if (songList.isNotEmpty()) {
                skipTo(0)
            }
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

    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }

    private fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    private fun trackInfo(): Array<out MediaPlayer.TrackInfo>? {
        return mediaPlayer?.trackInfo
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun sendPlaybackStartedBroadcast() {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(songList[currentSongIndex], HashMap())

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
        val count = mediaMetadataRetriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_IMAGE_COUNT
        )

        mediaMetadataRetriever.release()
        Log.e(
            "TAG",
            "info: title:$title, artist: $artist, album: $album, duration:$duration,  image: $image,  imageCount:  $count",
        )

        val intent = Intent(this, PlaybackStartedReceiver::class.java).apply {
            putExtra(EXTRA_SONG_TITLE, title)
        }
        sendBroadcast(intent)
       /* val flag = PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            flag
        )
        notificationManager.notify(
            1,
            notificationBuilder
                .setContentTitle(title)
                .setContentText("Plyer has Started")
                .addAction(0, "Pause", pendingIntent)
                .build()
        )*/
    }

    fun playFromStart() {
        startPlaying(songList[currentSongIndex])
    }
}