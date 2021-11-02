package com.example.musicplayer

import android.content.ContentResolver
import android.content.ContentUris
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

class MainActivity : SongChangeListener, AppCompatActivity() {

    var mediaPlayer: MediaPlayer? = null

    var musicAdapter: MusicAdapter? = null

    private var musicLists: ArrayList<MusicList>? = null

    var btnPlayPause: ImageView? = null
    var btnNext: ImageView? = null
    var btnPrevious: ImageView? = null

    var startTime: TextView? = null
    var endTime: TextView? = null

    var playPauseCard: CardView? = null
    var musicRecyclerView: RecyclerView? = null

    var menuBtn: LinearLayout? = null
    var searchBtn: LinearLayout? = null

    var isPlaying: Boolean = false

    var playerSeekBar: SeekBar? = null

    var timer: Timer? = null

    var currentSongListPosition = 0

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

        getMusicFiles()

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

    //get
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
}


/*package com.example.musicplayer

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import java.util.*
import kotlin.collections.ArrayList

import android.widget.Toast

import android.os.IBinder

import com.example.musicplayer.MediaPlayerService.LocalBinder
import android.content.ContentResolver
import androidx.cardview.widget.CardView

class MainActivity : AppCompatActivity() {

    var btnPlayPause: ImageView? = null
    var btnNext: ImageView? = null
    var btnPrevious: ImageView? = null

    var playPauseCard: CardView? = null
    var musicRecyclerView: RecyclerView? = null

    var menuBtn: LinearLayout? = null
    var searchBtn: LinearLayout? = null

    private var musicLists: ArrayList<MusicList>? = null

    //var mediaPlayer: MediaPlayer? = null

    var startTime: TextView? = null
    var endTime: TextView? = null
    var isPlaying: Boolean = false

    var playerSeekBar: SeekBar? = null

    var timer: Timer? = null

    var currentSongListPosition = 0

    var musicAdapter: MusicAdapter? = null


    private var player: MediaPlayerService? = null
    var serviceBound = false

    var audioList: ArrayList<Audio>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*val decodeView = window?.decorView

        val options: Int = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        decodeView?.systemUiVisibility = options*/
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

        loadAudio();
        initRecyclerView()
        playAudio()
        //play the first audio in the ArrayList
        //audioList?.get(0)?.data?.let { playAudio(it) };

    }

    //Binding this Client to the AudioPlayer Service
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as LocalBinder
            player = binder.service
            serviceBound = true
            Toast.makeText(this@MainActivity, "Service Bound", Toast.LENGTH_SHORT).show()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            serviceBound = false
        }
    }

    private fun loadAudio() {
        val contentResolver = contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0"
        val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"
        val cursor = contentResolver.query(uri, null, selection, null, sortOrder)
        if (cursor != null && cursor.count > 0) {
            audioList = ArrayList()
            while (cursor.moveToNext()) {
                val data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
                val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))

                // Save to audioList
                audioList!!.add(Audio(data, title, album, artist))
            }
        }
        cursor!!.close()
    }

    private fun initRecyclerView() {
        if (audioList!!.size > 0) {
            val recyclerView = findViewById<RecyclerView>(R.id.musicRecyclerView)
            val adapter = AudioAdapter(audioList!!, application)
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(this)
            /*recyclerView.addOnItemTouchListener(
                CustomTouchListener(
                    this,
                    object : onItemClickListener() {
                        fun onClick(view: View?, index: Int) {
                            playAudio(index)
                        }
                    })
            )*/
        }
    }

    private fun playAudio(media: Int) {
        //Check is service is active
        if (!serviceBound) {
            val playerIntent = Intent(this, MediaPlayerService::class.java)
            playerIntent.putExtra("media", media)
            startService(playerIntent)
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else {
            //Service is active
            //Send media with BroadcastReceiver
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putBoolean("ServiceState", serviceBound)
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        serviceBound = savedInstanceState.getBoolean("ServiceState")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            //service is active
            player!!.stopSelf()
        }
    }
}*/





/*package com.example.musicplayer

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.View.inflate
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.res.ColorStateListInflaterCompat.inflate
import androidx.core.content.res.ComplexColorCompat.inflate
import androidx.core.graphics.drawable.DrawableCompat.inflate
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import java.io.File
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    private var mNavController: NavController? = null

    var btnPlayPause: ImageView? = null
    var btnNext: ImageView? = null
    var btnPrevious: ImageView? = null

    var playPauseCard: CardView? = null
    var musicRecyclerView: RecyclerView? = null

    var menuBtn: LinearLayout? = null
    var searchBtn: LinearLayout? = null

    private var musicLists: ArrayList<MusicList>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_main)

    }
}*/