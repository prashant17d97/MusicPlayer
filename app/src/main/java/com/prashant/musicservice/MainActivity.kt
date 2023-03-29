package com.prashant.musicservice

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.prashant.musicservice.services.MusicService
import com.prashant.musicservice.services.SONG_LIST
import com.prashant.musicservice.ui.theme.MusicServiceTheme

class MainActivity : ComponentActivity() {
    private lateinit var musicService: MusicService
    private var isBound = false
    private var data = ""

    inner class PlaybackStartedReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val data = intent?.getStringExtra(MusicService.EXTRA_SONG_TITLE)
            Log.e("TAG", "onReceive: $data")
            if (data != null) {
                this@MainActivity.data = data
            }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(
            PlaybackStartedReceiver(),
            IntentFilter(MusicService.ACTION_PLAYBACK_STARTED)
        )
        setContent {
            MusicServiceTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Greeting()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(PlaybackStartedReceiver())
    }

    @Composable
    fun Greeting() {
        val context = LocalContext.current

        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { if (::musicService.isInitialized) musicService.previous() },
            ) {
                Text(text = "Previous")
            }
            Button(onClick = {
                val intent = Intent(context, MusicService::class.java).apply {
                    putStringArrayListExtra(
                        SONG_LIST,retrieveSongs()

                    )
                }
                startService(intent)
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }) {
                Text(text = "Play Music")
            }
            Button(onClick = { if (::musicService.isInitialized) musicService.next() }) {
                Text(text = "Next")
            }

            Button(onClick = {
                if (::musicService.isInitialized) {
                    musicService.pause()
                }
            }) {
                Text(text = "Pause")
            }
            Button(onClick = {
                if (::musicService.isInitialized) {
                    musicService.resume()
                }
            }) {
                Text(text = "Resume")
            }
        }
    }

    private fun retrieveSongs(): ArrayList<String> {
        val musicList = arrayListOf<String>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"
        val query = this.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )
        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val data = cursor.getString(dataColumn)
                musicList.add(data)
            }
        }
        return musicList.also {
            it.add(
                "https://cdnsongs.com/dren/music/data/Single_Track/202007/Excuses/320/Excuses_1.mp3/Excuses.mp3",
            )
        }
    }
}