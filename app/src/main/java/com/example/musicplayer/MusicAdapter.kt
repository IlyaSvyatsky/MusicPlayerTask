package com.example.musicplayer

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.jetbrains.annotations.NotNull
import java.util.*
import java.util.concurrent.TimeUnit

class MusicAdapter(var musicList: List<MusicList>, var context: Context):
    RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    var playingPosition: Int = 0
    private var songChangeListener: SongChangeListener? = null

    init {
        this.songChangeListener = context as SongChangeListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicAdapter.MusicViewHolder {
        val view: View
        val layoutInflater = LayoutInflater.from(parent.context)
        view = layoutInflater.inflate(R.layout.music_adapter_layout, parent, false)
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder: MusicAdapter.MusicViewHolder, position: Int) {

        val musicList2 = musicList[position]

        if (musicList2.isPlaying){

            playingPosition = holder.adapterPosition
            holder.relativeLayoutRootLayout.setBackgroundResource(R.drawable.round_back_blue_10)
        }
        else {
            holder.relativeLayoutRootLayout.setBackgroundResource(R.drawable.round_back_10)
        }

        holder.textViewTitle.text = musicList2.title
        holder.textViewArtist.text = musicList2.artist

        val generateDuration = String.format(Locale.getDefault(), "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(musicList[position].duration!!.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(musicList[position].duration!!.toLong()) -
            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(musicList[position].duration!!.toLong())))

        holder.textViewMusicDuration.text = generateDuration

        holder.relativeLayoutRootLayout.setOnClickListener {

            musicList[playingPosition].isPlaying = false
            musicList2.isPlaying = true

            songChangeListener?.onChanged(position)
            notifyDataSetChanged()
        }
    }

    fun updateList(list: List<MusicList>) {
        this.musicList = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return musicList.size
    }

    inner class MusicViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val textViewTitle: TextView
        val textViewArtist: TextView
        val textViewMusicDuration: TextView
        val relativeLayoutRootLayout: RelativeLayout

        init {
            context = itemView.context
            textViewTitle = itemView.findViewById(R.id.musicTitle)
            textViewArtist = itemView.findViewById(R.id.musicArtist)
            textViewMusicDuration = itemView.findViewById(R.id.endTime)
            relativeLayoutRootLayout = itemView.findViewById(R.id.rootLayout)
        }
    }
}