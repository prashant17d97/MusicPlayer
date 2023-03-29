package com.prashant.musicservice.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.prashant.musicservice.services.MusicService.Companion.EXTRA_SONG_TITLE

class PlaybackStartedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val data = intent?.getStringExtra(EXTRA_SONG_TITLE)
        Log.e("TAG", "onReceive: $data")

    }


}