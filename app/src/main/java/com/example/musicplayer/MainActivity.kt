package com.example.musicplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.database.Cursor
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class MainActivity :AppCompatActivity(), SongChangeListener, Playable {

    var mediaPlayer: MediaPlayer? = null
    var musicAdapter: MusicAdapter? = null

    private var musicLists: ArrayList<MusicList>? = null

    var btnPlayPause: ImageView? = null
    var btnNext: ImageView? = null
    var btnPrevious: ImageView? = null

    var startTime: TextView? = null
    var endTime: TextView? = null

    var textViewTitle: TextView? = null
    var textViewArtist: TextView? = null

    var playPauseCard: CardView? = null
    var musicRecyclerView: RecyclerView? = null

    var menuBtn: LinearLayout? = null
    var searchBtn: LinearLayout? = null

    var isPlaying: Boolean = false

    var playerSeekBar: SeekBar? = null

    var timer: Timer? = null

    private var currentSongListPosition = 0

    var createNotification = CreateNotification()
    var notificationManager: NotificationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        btnPrevious = findViewById(R.id.previousImg)
        btnNext = findViewById(R.id.nextBtn)
        btnPlayPause = findViewById(R.id.playPauseImg)
        playPauseCard = findViewById(R.id.playPauseCard)
        musicRecyclerView = findViewById(R.id.musicRecyclerView)
        menuBtn = findViewById(R.id.menuBtn)
        searchBtn = findViewById(R.id.searchBtn)
        startTime = findViewById(R.id.startTime)
        endTime = findViewById(R.id.endTime)
        playerSeekBar = findViewById(R.id.playerSeekBar)

        musicRecyclerView?.setHasFixedSize(true)
        musicRecyclerView?.layoutManager = LinearLayoutManager(this)

        musicLists = ArrayList()
        mediaPlayer = MediaPlayer()

        textViewTitle = findViewById(R.id.musicTitle)
        textViewArtist = findViewById(R.id.musicArtist)

        getMusicFiles()

        buttonPrevious()
        buttonNext()
        buttonPlayPauseCard()
        playerSeekBar()

        createChannel()
        registerReceiver(broadcastReceiver, IntentFilter("TRACKS_TRACKS"))
        startService(Intent(baseContext, OnClearFromRecentServices::class.java))
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            createNotification.CHANNEL_ID,
            "KOD Dev", NotificationManager.IMPORTANCE_LOW
        )
        notificationManager = getSystemService(NotificationManager::class.java)
        if (notificationManager != null) {
            notificationManager!!.createNotificationChannel(channel)
        }
    }

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.extras!!.getString("actionname")) {
                createNotification.ACTION_PREVIOUS -> buttonNotificationPrevious()
                createNotification.ACTION_PLAY -> if (isPlaying) {
                    buttonNotificationPause()
                } else {
                    buttonNotificationPlay()
                }
                createNotification.ACTION_NEXT -> buttonNotificationNext()
            }
        }
    }

    override fun buttonNotificationPrevious() {

        var prevSongListPosition: Int = currentSongListPosition - 1

        if(prevSongListPosition < 0) {
            prevSongListPosition = musicLists?.size!! - 1 //play last song
        }

        musicLists?.get(currentSongListPosition)?.isPlaying = false
        musicLists?.get(prevSongListPosition)?.isPlaying = true

        musicAdapter?.updateList(musicLists!!)

        musicRecyclerView?.scrollToPosition(prevSongListPosition)
        onChanged(prevSongListPosition)

        createNotification.createNotification(this, musicLists!![prevSongListPosition], R.drawable.ic_pause, prevSongListPosition, musicLists!!.size-1)
        textViewTitle?.text = musicLists!![prevSongListPosition].title
    }

    override fun buttonNotificationPlay() {

        createNotification.createNotification(this, musicLists!![currentSongListPosition], R.drawable.ic_pause, currentSongListPosition, musicLists!!.size-1)
        textViewTitle?.text = musicLists!![currentSongListPosition].title
        if(isPlaying) {
            isPlaying = false
            mediaPlayer?.pause()
            btnPlayPause?.setImageResource(R.drawable.ic_play)
        }
        else {
            isPlaying = true
            mediaPlayer?.start()
            btnPlayPause?.setImageResource(R.drawable.ic_pause)
        }
    }

    override fun buttonNotificationPause() {

        createNotification.createNotification(this, musicLists!![currentSongListPosition], R.drawable.ic_play, currentSongListPosition, musicLists!!.size-1)
        textViewTitle?.text = musicLists!![currentSongListPosition].title
        if(isPlaying) {
            isPlaying = false
            mediaPlayer?.pause()
            btnPlayPause?.setImageResource(R.drawable.ic_play)
        }
        else {
            isPlaying = true
            mediaPlayer?.start()
            btnPlayPause?.setImageResource(R.drawable.ic_pause)
        }
    }

    override fun buttonNotificationNext() {

        var nextSongListPosition: Int = currentSongListPosition + 1

        if(nextSongListPosition >= musicLists?.size!!) {
            nextSongListPosition = 0
        }

        musicLists?.get(currentSongListPosition)?.isPlaying = false
        musicLists?.get(nextSongListPosition)?.isPlaying = true

        musicAdapter?.updateList(musicLists!!)

        musicRecyclerView?.scrollToPosition(nextSongListPosition)

        onChanged(nextSongListPosition)

        createNotification.createNotification(this, musicLists!![nextSongListPosition], R.drawable.ic_pause, nextSongListPosition, musicLists!!.size-1)
        textViewTitle?.text = musicLists!![nextSongListPosition].title
    }

    private fun getMusicFiles() {

        val contentResolver: ContentResolver = contentResolver
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cursor: Cursor? = contentResolver.query(uri, null, MediaStore.Audio.Media.DATA+" LIKE?", arrayOf("%.mp3%"), null)

        if(cursor == null) {
            Toast.makeText(this, "Something went Wrong!", Toast.LENGTH_SHORT).show()
        }
        else if(!cursor.moveToNext()) {
            Toast.makeText(this, "No music found!", Toast.LENGTH_SHORT).show()
        }
        else {
            while (cursor.moveToNext()) {
                val getMusicFileName: String = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val getArtistName: String = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val cursorId: Long = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                val musicFileUri: Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursorId)
                val getDuration = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))

                val musicList = MusicList(getMusicFileName, getArtistName, getDuration, false, musicFileUri)
                musicLists?.add(musicList)
            }

            musicAdapter =  MusicAdapter(musicLists!!, this)
            musicRecyclerView!!.adapter = musicAdapter
        }
        cursor?.close()
    }

    override fun onChanged(position: Int) {

        currentSongListPosition = position

        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            mediaPlayer?.reset()
        }

        mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)

        Thread {
            try {
                mediaPlayer?.setDataSource(this, musicLists!![position].musicFile!!)
                mediaPlayer?.prepare()
            }
            catch (e: IOException) {

                if (mediaPlayer != null) {
                    mediaPlayer?.reset();
                }

                e.printStackTrace()
                Toast.makeText(this, "Unable to play track", Toast.LENGTH_SHORT).show()
            }
        }.start()

        mediaPlayer?.setOnPreparedListener {
            val getTotalDuration: Int = it.duration
            val generateDuration = String.format(
                Locale.getDefault(), "%02d:%02d",
                java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(getTotalDuration.toLong()),
                java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(getTotalDuration.toLong()) -
                java.util.concurrent.TimeUnit.MINUTES.toSeconds(java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(getTotalDuration.toLong())))

            endTime?.text = generateDuration

            isPlaying = true

            it.start()

            playerSeekBar?.max = getTotalDuration

            btnPlayPause?.setImageResource(R.drawable.ic_pause)

            createNotification.createNotification(this, musicLists!![currentSongListPosition], R.drawable.ic_pause, 1, musicLists!!.size-1)
        }

        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {

                runOnUiThread {
                    val getCurrentDuration = mediaPlayer?.currentPosition

                    val generateDuration = String.format(
                        Locale.getDefault(), "%02d:%02d",
                        java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(getCurrentDuration!!.toLong()),
                        java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(getCurrentDuration.toLong()) -
                                java.util.concurrent.TimeUnit.MINUTES.toSeconds(java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(getCurrentDuration.toLong())))

                    playerSeekBar?.progress = getCurrentDuration

                    startTime?.text = generateDuration
                }
            }

        }, 1000, 1000)

        mediaPlayer?.setOnCompletionListener {

            it.reset()

            timer?.purge()
            timer?.cancel()

            isPlaying = false

            btnPlayPause?.setImageResource(R.drawable.ic_play)

            playerSeekBar?.progress = 0
        }
    }

    private fun buttonPrevious() {

        btnPrevious?.setOnClickListener {

            var prevSongListPosition: Int = currentSongListPosition - 1

            if(prevSongListPosition < 0) {
                prevSongListPosition = musicLists?.size!! - 1 //play last song
            }

            musicLists?.get(currentSongListPosition)?.isPlaying = false
            musicLists?.get(prevSongListPosition)?.isPlaying = true

            musicAdapter?.updateList(musicLists!!)

            musicRecyclerView?.scrollToPosition(prevSongListPosition)
            onChanged(prevSongListPosition)
        }
    }

    private fun buttonNext() {

        btnNext?.setOnClickListener {

            var nextSongListPosition: Int = currentSongListPosition + 1

            if(nextSongListPosition >= musicLists?.size!!) {
                nextSongListPosition = 0
            }

            musicLists?.get(currentSongListPosition)?.isPlaying = false
            musicLists?.get(nextSongListPosition)?.isPlaying = true

            musicAdapter?.updateList(musicLists!!)

            musicRecyclerView?.scrollToPosition(nextSongListPosition)

            onChanged(nextSongListPosition)
        }
    }

    private fun buttonPlayPauseCard() {

        playPauseCard?.setOnClickListener {
            if(isPlaying) {
                isPlaying = false
                mediaPlayer?.pause()
                btnPlayPause?.setImageResource(R.drawable.ic_play)
            }
            else {
                isPlaying = true
                mediaPlayer?.start()
                btnPlayPause?.setImageResource(R.drawable.ic_pause)
            }
        }
    }

    private fun playerSeekBar() {

        playerSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if(p2) {
                    if(isPlaying) {
                        mediaPlayer?.seekTo(p1)
                    }
                    else {
                        mediaPlayer?.seekTo(0)
                    }
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })

    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager!!.cancelAll()
        unregisterReceiver(broadcastReceiver)
    }
}