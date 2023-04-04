package com.prashant.musicservice.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.prashant.musicservice.activity.MainActivity
import com.prashant.musicservice.services.EXTRA_SONG_TITLE


class PlaybackStartedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.e("PlaybackStartedReceiver", "onReceive: ${intent?.getStringExtra(EXTRA_SONG_TITLE)}")
    }


}