package com.sparsh.statussaver_videodownload.UI.Adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.sparsh.statussaver_videodownload.R
import com.sparsh.statussaver_videodownload.UI.Data.MediaItem
import com.sparsh.statussaver_videodownload.UI.Utils.DeleteUtils

class MediaAdapter(
    private val mediaList: MutableList<MediaItem>,
    private val onClick: (Int) -> Unit,
    private val onDeleteClick: (MediaItem) -> Unit
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_d, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaItem = mediaList[position]
        holder.bind(mediaItem)
        holder.itemView.setOnClickListener { onClick(position) }

        holder.deleteButton.setOnClickListener {
            val context = holder.itemView.context
            val mediaUri = Uri.parse(mediaItem.data)
            val filePath = mediaItem.data
            val isVideo = mediaItem.isVideo

            if (DeleteUtils.deleteMedia(context, mediaUri, filePath, isVideo)) {
                mediaList.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, mediaList.size)
                onDeleteClick(mediaItem)
                Toast.makeText(context, "Media deleted successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to delete media", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int = mediaList.size

    class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        private val imageViewPlay: ImageView = itemView.findViewById(R.id.imageViewPlay)
        internal val deleteButton: ImageView = itemView.findViewById(R.id.deleteext)

        fun bind(mediaItem: MediaItem) {
            Glide.with(itemView.context)
                .load(mediaItem.data)
                .apply(RequestOptions().centerCrop())
                .into(imageView)

            imageViewPlay.visibility = if (mediaItem.isVideo) View.VISIBLE else View.GONE
        }
    }
}
