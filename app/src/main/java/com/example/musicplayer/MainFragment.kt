/*package com.example.musicplayer

import android.content.ContentResolver
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.icu.util.TimeUnit
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.convertTo
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.IOException
import java.util.*
import java.util.jar.Manifest
import kotlin.collections.ArrayList


class MainFragment : IOnFocusListenable, SongChangeListener, Fragment(R.layout.fragment_main) {

    var btnPlayPause: ImageView? = null
    var btnNext: ImageView? = null
    var btnPrevious: ImageView? = null

    var playPauseCard: CardView? = null
    var musicRecyclerView: RecyclerView? = null

    var menuBtn: LinearLayout? = null
    var searchBtn: LinearLayout? = null

    private var musicLists: ArrayList<MusicList>? = null

    var mediaPlayer: MediaPlayer? = null

    var startTime: TextView? = null
    var endTime: TextView? = null
    var isPlaying: Boolean = false

    var playerSeekBar: SeekBar? = null

    var timer: Timer? = null

    var currentSongListPosition = 0

    var musicAdapter: MusicAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val decodeView = activity?.window?.decorView

        val options: Int = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        decodeView?.systemUiVisibility = options

        val view: View = inflater.inflate(R.layout.fragment_main, container, false)


        btnPrevious = view.findViewById(R.id.previousImg)
        btnNext = view.findViewById(R.id.nextBtn)
        btnPlayPause = view.findViewById(R.id.playPauseImg)

        playPauseCard = view.findViewById(R.id.playPauseCard)
        musicRecyclerView = view.findViewById(R.id.musicRecyclerView)

        menuBtn = view.findViewById(R.id.menuBtn)
        searchBtn = view.findViewById(R.id.searchBtn)

        startTime = view.findViewById(R.id.startTime)
        endTime = view.findViewById(R.id.endTime)

        playerSeekBar = view.findViewById(R.id.playerSeekBar)

        musicRecyclerView?.setHasFixedSize(true)
        musicRecyclerView?.layoutManager = LinearLayoutManager(context)

        musicLists = ArrayList()

        mediaPlayer = MediaPlayer()


        //if(context?.let { ContextCompat.checkSelfPermission(it, android.Manifest.permission.READ_EXTERNAL_STORAGE) } == PackageManager.PERMISSION_GRANTED) {
            getMusicFiles()

        /*val adapter = MusicAdapter(musicLists!!, requireContext())
        musicRecyclerView?.adapter = adapter
        adapter.setMusicList(musicLists!!)*/
       // }
        /*else {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(object : ArrayList<String>{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 11)
            }
        }*/

        btnNext?.setOnClickListener {

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

        btnPrevious?.setOnClickListener {

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

        return view
    }


    private fun getMusicFiles() {

        val contentResolver: ContentResolver = requireContext().contentResolver

        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val cursor: Cursor? = contentResolver.query(uri, null, MediaStore.Audio.Media.DATA+" LIKE?", arrayOf("%.mp3%"), null)

        if (cursor != null && cursor.moveToFirst()) {

            while (cursor.moveToNext()) {
                val getMusicFileName: String = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val getArtistName: String = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val cursorId: Long = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))

                val musicFileUri: Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursorId)

                var getDuration = "00:00"

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    getDuration  = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                }

                val musicList = MusicList(getMusicFileName, getArtistName, getDuration, false, musicFileUri)
                musicLists?.add(musicList)
            }

            musicAdapter =  MusicAdapter(musicLists!!, requireContext())
            musicRecyclerView!!.adapter = musicAdapter
        }

        cursor?.close()

        /*if(cursor == null) {
            Toast.makeText(context, "Something went Wrong!", Toast.LENGTH_SHORT).show()
        }
        else if(!cursor.moveToNext()) {
            Toast.makeText(context, "No music found!", Toast.LENGTH_SHORT).show()
        }
        else {
            while (cursor.moveToNext()) {
                val getMusicFileName: String = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val getArtistName: String = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val cursorId: Long = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))

                val musicFileUri: Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursorId)

                var getDuration = "00:00"

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    getDuration = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                }

                val musicList = MusicList(getMusicFileName, getArtistName, getDuration, false, musicFileUri)
                musicLists?.add(musicList)
            }

            musicRecyclerView?.adapter = musicLists?.let { MusicAdapter(it, context) }
        }*/
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) { super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getMusicFiles()
        }
        else {
            Toast.makeText(context, "Permissions Declined by User", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            val decodeView = activity?.window?.decorView

            val options: Int = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            decodeView?.systemUiVisibility = options
        }
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
                musicLists?.get(position)?.musicFile?.let {
                    mediaPlayer?.setDataSource(
                        requireContext(),
                        it
                    )
                }
                mediaPlayer?.prepare()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(context, "Unable to play track", Toast.LENGTH_SHORT).show()
            }
        }.start()

        mediaPlayer?.setOnPreparedListener {
            val getTotalDuration: Int = it.duration

            val generateDuration = String.format(
                Locale.getDefault(), "%02d:%02d",
            java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(getTotalDuration.toLong()),
            java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(getTotalDuration.toLong()),
            java.util.concurrent.TimeUnit.MINUTES.toMinutes(getTotalDuration.toLong()))

            endTime?.text = generateDuration

            isPlaying = true

            it.start()

            playerSeekBar?.max = getTotalDuration

            btnPlayPause?.setImageResource(R.drawable.ic_pause)
        }

        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {

                activity?.runOnUiThread {
                    val getCurrentDuration = mediaPlayer?.currentPosition

                    val generateDuration = String.format(
                        Locale.getDefault(), "%02d:%02d",
                        java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(getCurrentDuration!!.toLong()),
                        java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(getCurrentDuration.toLong()),
                        java.util.concurrent.TimeUnit.MINUTES.toMinutes(getCurrentDuration.toLong()))

                    playerSeekBar?.progress = getCurrentDuration

                    startTime?.text = generateDuration
                }
            }

        }, 1000, 1000)

        mediaPlayer?.setOnCompletionListener {
            mediaPlayer?.reset()

            timer?.purge()
            timer?.cancel()

            isPlaying = false

            btnPlayPause?.setImageResource(R.drawable.ic_play)

            playerSeekBar?.progress = 0
        }
    }
}
*/

