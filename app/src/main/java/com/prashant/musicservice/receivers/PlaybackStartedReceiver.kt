package com.prashant.musicservice.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.prashant.musicservice.services.MusicService.Companion.EXTRA_SONG_TITLE
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

class PlaybackStartedReceiver constructor(
    private val notificationManager: NotificationManagerCompat,
    private val notificationBuilder: NotificationCompat.Builder
): BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val data = intent?.getStringExtra(EXTRA_SONG_TITLE)
//        notificationManager.notify(1, notificationBuilder.setContentText(data).build())
        Log.e("TAG", "onReceive: $data")
    }


}