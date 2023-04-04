package com.prashant.musicservice.activity

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prashant.musicservice.MusicModel
import com.prashant.musicservice.R
import com.prashant.musicservice.interfaces.MusicServiceUpdate
import com.prashant.musicservice.services.EXTRA_SONG_TITLE
import com.prashant.musicservice.services.MusicService
import com.prashant.musicservice.services.SONG_LIST
import com.prashant.musicservice.ui.theme.MusicServiceTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.lang.ref.WeakReference
import java.time.Duration

@AndroidEntryPoint
class MainActivity : ComponentActivity(), MusicServiceUpdate {
    private lateinit var musicService: MusicService
    private var isBound = false

    private var pausePlay by mutableStateOf(R.drawable.ic_play)
    private var isPrepared by mutableStateOf(false)
    private var isPaused by mutableStateOf(false)
    private var showLoader by mutableStateOf(false)
    private var timeInMillis by mutableStateOf(0)
    private var progress by mutableStateOf(0f)


    private val mainVM by viewModels<MainVM>()
//    private lateinit var receiver: PlaybackStartedReceiver

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (!it) {
                launchPermission()
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

    private fun launchPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(POST_NOTIFICATIONS)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        receiver = PlaybackStartedReceiver(mainVM.notificationManager, mainVM.notificationBuilder)
        launchPermission()
        weakReference = WeakReference(this)
        /*registerReceiver(
            receiver,
            IntentFilter(MusicService.ACTION_PLAYBACK_STARTED)
        )*/
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

    private fun intentListener() {
        val data = intent?.getStringExtra("my_data")

        // Do whatever you need to do with the data
        Log.e("MainActivity", "Received data: $data")
    }

    override fun onStart() {
        super.onStart()
        intentListener()
        val intent = Intent(this@MainActivity, MusicService::class.java).apply {
            putStringArrayListExtra(
                SONG_LIST, retrieveSongs()
            )
        }
        startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
//        unregisterReceiver(receiver)
        weakReference = WeakReference(null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun TimeProgressBar(inMillis: Int, onSeek: (Float) -> Unit) {
        progress = if (timeInMillis == 0) 0f else progress
        if (inMillis > 0 && timeInMillis > 0) {
            LaunchedEffect(Unit) {
                while (progress < 1.0) {
                    delay(1000)
                    progress += 1f / (inMillis / 1000f) // increase progress by 1 second every second
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            val progressTime = Duration.ofMillis((inMillis * progress).toLong())
            val remainingTime = inMillis - (inMillis * progress).toLong()
            val remainingDuration = Duration.ofMillis(remainingTime)
            Text(
                text = "${String.format("%02d", progressTime.toMinutes())}:${
                    String.format(
                        "%02d",
                        progressTime.seconds % 60
                    )
                }",
                textAlign = TextAlign.Center,
            )

            Slider(
                value = progress, onValueChange = {
                    progress = it
                    onSeek(it)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp)
            )

            Text(
                text = "${
                    String.format(
                        "%02d",
                        remainingDuration.toMinutes()
                    )
                }:${String.format("%02d", remainingDuration.seconds % 60)}",
                textAlign = TextAlign.Center,
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun Greeting() {

        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            TimeProgressBar(timeInMillis) {
                musicService.skipTo((timeInMillis * it).toInt())
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    25.dp,
                    alignment = Alignment.CenterHorizontally
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                IconButton(
                    onClick = {
                        musicService.previous { isSkipping ->
                            if (!isSkipping) {
                                timeInMillis = 0
                                showLoader = true
                            } else {
                                progress = 0f
                                timeInMillis = musicService.getDuration()
                            }
                        }

                    },
                ) {
                    Icon(
                        modifier = Modifier.size(50.dp),
                        painter = painterResource(id = R.drawable.ic_previous),
                        contentDescription = "Previous"
                    )
                }

                IconButton(
                    onClick = {
                        pausePlay = if (!isPaused && !isPrepared) {
                            showLoader = true
                            musicService.initiatePlayer()
                            R.drawable.pause
                        } else if (isPaused) {
                            musicService.resume()
                            isPaused = false
                            R.drawable.pause
                        } else {
                            musicService.pause()
                            isPaused = true
                            R.drawable.ic_resume
                        }

                    }) {
                    if (showLoader) {
                        CircularProgressIndicator(
                            color = Color.Green
                        )
                    } else {
                        Icon(
                            modifier = Modifier.size(50.dp),
                            painter = painterResource(id = pausePlay),
                            contentDescription = ""
                        )
                    }
                }
                IconButton(
                    onClick = {
                        musicService.next {
                            if (it) {
                                showLoader = true
                                timeInMillis = 0
                            }
                        }

                    }) {
                    Icon(
                        modifier = Modifier.size(50.dp),
                        painter = painterResource(id = R.drawable.ic_next),
                        contentDescription = "Next"
                    )
                }
            }
            Button(onClick = mainVM::showNotification) {
                Text(text = "Send notification")
            }
            Button(onClick = mainVM::updateNotification) {
                Text(text = "Update")
            }
            Button(onClick = mainVM::cancelNotification) {
                Text(text = "Cancel")
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
//            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
//            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {

                val data = cursor.getString(dataColumn)
                musicList.add(data)
            }
        }
        return musicList.also {
            it.addAll(
                arrayListOf(
                    "https://cdnsongs.com/dren/music/data/Single_Track/202007/Excuses/320/Excuses_1.mp3/Excuses.mp3",
                    "https://pagalfree.com/download/320-Besharam%20Rang%20-%20Pathaan%20320%20Kbps.mp3"
                )
            )
        }
    }

    override fun musicUpdate(musicModel: MusicModel) {
        Log.e("TAG", "musicUpdate: $musicModel")
        isPaused = musicModel.isPaused
        isPrepared = musicModel.isPrepared
        pausePlay = R.drawable.pause.takeIf { musicModel.isPrepared } ?: R.drawable.ic_play
        showLoader = false
        timeInMillis =
            musicService.getDuration().takeIf { musicModel.isPrepared || musicModel.isPaused } ?: 0
    }

    companion object {
        var weakReference = WeakReference<MainActivity>(null)
    }
}