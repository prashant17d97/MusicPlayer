package com.prashant.musicservice.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainVM @Inject constructor(
    private val notificationManager: NotificationManagerCompat,
    private val notificationBuilder: NotificationCompat.Builder
) : ViewModel() {

    fun showNotification() {
        notificationManager.notify(1, notificationBuilder.build())
    }
    fun updateNotification() {
        notificationManager.notify(1, notificationBuilder.setContentTitle("Singh").build())
    }
    fun cancelNotification() {
        notificationManager.cancel(1)
    }
}