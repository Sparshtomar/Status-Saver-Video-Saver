package com.sparsh.statussaver_videodownload.UI.Adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.sparsh.statussaver_videodownload.R
import com.sparsh.statussaver_videodownload.UI.Data.MediaItem

@Suppress("DEPRECATION")
class MediaPagerAdapter(private var mediaList: List<MediaItem>) :
    RecyclerView.Adapter<MediaPagerAdapter.MediaViewHolder>() {

    private var currentPlayingPosition: Int? = null
    private var currentExoPlayer: ExoPlayer? = null
    private val viewHolders = mutableMapOf<Int, MediaViewHolder>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media_preview, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaItem = mediaList[position]
        holder.bind(mediaItem)
        viewHolders[position] = holder

        if (position == currentPlayingPosition) {
            holder.playPlayback()
        } else {
            holder.pausePlayback()
        }
    }

    override fun onViewRecycled(holder: MediaViewHolder) {
        super.onViewRecycled(holder)
        viewHolders.entries.find { it.value == holder }?.let {
            viewHolders.remove(it.key)
        }
        if (holder.adapterPosition == currentPlayingPosition) {
            holder.pausePlayback()
            releaseCurrentPlayer()
        }
    }

    fun startPlayback(position: Int) {
        if (position != currentPlayingPosition) {
            currentPlayingPosition?.let {
                viewHolders[it]?.pausePlayback()
            }
            currentPlayingPosition = position
            viewHolders[position]?.playPlayback()
        }
    }



    fun stopPlayback() {
        currentPlayingPosition?.let {
            viewHolders[it]?.pausePlayback()
            releaseCurrentPlayer()
        }
        currentPlayingPosition = null
    }

    fun releasePlayers() {
        viewHolders.values.forEach { it.releasePlayer() }
        releaseCurrentPlayer()
    }

    private fun releaseCurrentPlayer() {
        currentExoPlayer?.stop()
        currentExoPlayer?.release()
        currentExoPlayer = null
    }

    override fun getItemCount(): Int = mediaList.size

    fun updateData(newMediaList: List<MediaItem>) {
        mediaList = newMediaList
        val removedPosition = currentPlayingPosition ?: -1  // Get removed position (if playing)
        if (removedPosition >= 0) {
            notifyItemRemoved(removedPosition)
            notifyItemRangeChanged(removedPosition, mediaList.size - removedPosition)
        } else {
            notifyDataSetChanged()
        }
    }

    inner class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        private val playerView: PlayerView = itemView.findViewById(R.id.playerView)
        private var exoPlayer: ExoPlayer? = null

        fun bind(mediaItem: MediaItem) {
            if (mediaItem.isVideo) {
                playerView.visibility = View.VISIBLE
                imageView.visibility = View.GONE
                initializePlayer(mediaItem.data)
                playPlayback()
            } else {
                playerView.visibility = View.GONE
                imageView.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(mediaItem.data)
                    .apply(RequestOptions())
                    .into(imageView)
                pausePlayback()
                releasePlayer()
            }
        }

        private fun initializePlayer(videoUrl: String) {
            if (exoPlayer == null) {
                exoPlayer = ExoPlayer.Builder(itemView.context).build()
                playerView.player = exoPlayer
                val mediaUri = Uri.parse(videoUrl)
                val exoMediaItem = androidx.media3.common.MediaItem.Builder()
                    .setUri(mediaUri)
                    .build()
                exoPlayer?.setMediaItem(exoMediaItem)
                exoPlayer?.prepare()
                exoPlayer?.playWhenReady = true
                currentExoPlayer = exoPlayer
            }
        }

        fun playPlayback() {
            exoPlayer?.playWhenReady = true
        }

        fun pausePlayback() {
            exoPlayer?.playWhenReady = false
        }

        fun releasePlayer() {
            exoPlayer?.stop()
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    fun getViewHolderForPosition(position: Int): MediaViewHolder? {
        return viewHolders[position]
    }
}
