package com.example.musicplayer

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class CreateNotification {

    val CHANNEL_ID = "channel1"
    val ACTION_PREVIOUS = "actionprevious"
    val ACTION_PLAY = "actionplay"
    val ACTION_NEXT = "actionnext"

    var notification: Notification? = null

    fun createNotification(context: Context, musicList: MusicList, playButton: Int, pos: Int, size: Int) {

        val notificationManagerCompat = NotificationManagerCompat.from(context)
        val mediaSessionCompat = MediaSessionCompat(context, "tag")

        val pendingIntentPrevious: PendingIntent?

        val drw_previous: Int?
        val drw_next: Int?

        if(pos == 0) {
            pendingIntentPrevious = null
            drw_previous = 0
        }
        else {
            val intentPrevious: Intent = Intent(context, NotificationActionService::class.java)
                .setAction(ACTION_PREVIOUS)
            pendingIntentPrevious = PendingIntent.getBroadcast(context, 0, intentPrevious, PendingIntent.FLAG_UPDATE_CURRENT)
            drw_previous = R.drawable.ic_previous
        }

        val intentPlay: Intent = Intent(context, NotificationActionService::class.java)
            .setAction(ACTION_PLAY)
        val paddingIntentPlay = PendingIntent.getBroadcast(context, 0, intentPlay, PendingIntent.FLAG_UPDATE_CURRENT)
        val paddingIntentNext: PendingIntent?


        if(pos == size) {
            paddingIntentNext = null
            drw_next = 0
        }
        else {
            val intentNext: Intent = Intent(context, NotificationActionService::class.java)
                .setAction(ACTION_NEXT)
            paddingIntentNext = PendingIntent.getBroadcast(context, 0, intentNext, PendingIntent.FLAG_UPDATE_CURRENT)
            drw_next = R.drawable.ic_next
        }

        notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_search)
            .setContentTitle(musicList.title)
            .setContentText(musicList.artist)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .addAction(drw_previous, "Previous", pendingIntentPrevious)
            .addAction(playButton, "Play", paddingIntentPlay)
            .addAction(drw_next, "Next", paddingIntentNext)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
                .setMediaSession(mediaSessionCompat.sessionToken))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManagerCompat.notify(1, notification!!)
    }
}