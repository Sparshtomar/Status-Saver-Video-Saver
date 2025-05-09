@file:Suppress("DEPRECATION")

package com.sparsh.statussaver_videodownload.UI.Fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.sparsh.statussaver_videodownload.UI.ADS.AdHelper
import com.sparsh.statussaver_videodownload.UI.Activities.MediaPreviewActivity
import com.sparsh.statussaver_videodownload.UI.Adapters.MediaAdapter
import com.sparsh.statussaver_videodownload.UI.Data.MediaItem
import com.sparsh.statussaver_videodownload.UI.Utils.DeleteUtils
import com.sparsh.statussaver_videodownload.databinding.FragmentDownloadsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DownloadsFragment : Fragment() {

    private lateinit var binding: FragmentDownloadsBinding
    private lateinit var adapter: MediaAdapter
    private val mediaList = ArrayList<MediaItem>()
    private val imageFiles = mutableListOf<String>()
    private val videoFiles = mutableListOf<String>()
    private val imageExtensions = listOf("jpg", "jpeg", "png", "bmp", "gif")
    private val videoExtensions = listOf("mp4", "avi", "mkv", "3gp", "mov")
    private val adUnitIdBanner = "ca-app-pub-7713317467402311/6200606525"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDownloadsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.downloadsrecv.layoutManager = GridLayoutManager(context, 2)
        adapter = MediaAdapter(mediaList, ::onMediaItemClick, ::onDeleteMedia)
        binding.downloadsrecv.adapter = adapter

        AdHelper.loadBannerAd(requireActivity(), binding.adViewContainer, adUnitIdBanner)

        Log.d(TAG, "Checking for permissions and fetching media")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d(TAG, "Android 11 and above detected")
            fetchMediaForAndroid11AndAbove()
        } else {
            Log.d(TAG, "Android 10 or below detected")
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission granted")
                fetchMediaForAndroid10AndBelow()
            } else {
                Log.d(TAG, "Requesting permission")
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_PERMISSION_CODE)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d(TAG, "Android 11 and above detected")
            fetchMediaForAndroid11AndAbove()
        } else {
            Log.d(TAG, "Android 10 or below detected")
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission granted")
                fetchMediaForAndroid10AndBelow()
            } else {
                Log.d(TAG, "Requesting permission")
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_PERMISSION_CODE)
            }
        }
    }

    private fun onMediaItemClick(position: Int) {
        val intent = Intent(context, MediaPreviewActivity::class.java).apply {
            putParcelableArrayListExtra("mediaList", ArrayList(mediaList))
            putExtra("position", position)
        }
        startActivity(intent)
    }

    private fun onDeleteMedia(mediaItem: MediaItem) {
        val filePath = mediaItem.data
        val mediaUri = Uri.parse(filePath)
        val isVideo = mediaItem.isVideo

        val isDeleted = DeleteUtils.deleteMedia(requireContext(), mediaUri, filePath, isVideo)

        if (isDeleted) {
            mediaList.remove(mediaItem)
            adapter.notifyDataSetChanged()
            updateUI()
            Toast.makeText(context, "Media deleted successfully", Toast.LENGTH_SHORT).show()
        } else {
            updateUI()
        }
    }

    private fun updateUI() {
        if (mediaList.isEmpty()) {
            binding.downloadsrecv.visibility = View.GONE
            binding.noImagesLayout.visibility = View.VISIBLE
        } else {
            binding.downloadsrecv.visibility = View.VISIBLE
            binding.noImagesLayout.visibility = View.GONE
        }
    }

    private fun fetchMediaForAndroid10AndBelow() {
        CoroutineScope(Dispatchers.IO).launch {
            // Get the directory path
            Log.d(TAG, "Fetching media from private storage")

            // Get the directory path
            val picturesDir = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Status Saver - Video Download")
            if (picturesDir.exists() && picturesDir.isDirectory) {
                val files = picturesDir.listFiles()
                if (files != null) {
                    mediaList.clear() // Clear existing media list

                    for (file in files) {
                        if (file.isFile) {
                            val isVideo = file.extension in videoExtensions
                            val id = file.hashCode().toLong() // Using file hashCode as a placeholder ID
                            val displayName = file.name
                            val data = file.absolutePath

                            mediaList.add(MediaItem(id, displayName, data, isVideo))
                            Log.d(TAG, "Found media - ID: $id, Name: $displayName, Path: $data, IsVideo: $isVideo")
                        }
                    }
                } else {
                    Log.d(TAG, "No files found in the directory")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No media found in the directory", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Log.d(TAG, "Directory does not exist")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Directory does not exist", Toast.LENGTH_SHORT).show()
                }
            }

            withContext(Dispatchers.Main) {
                adapter.notifyDataSetChanged()
                updateUI()
            }
        }
    }

    private fun fetchMediaForAndroid11AndAbove() {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Fetching media for Android 11 and above")

            // Query images
            val imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            val imageProjection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA
            )
            val imageSelection = "${MediaStore.Images.Media.DATA} LIKE ?"
            val imageSelectionArgs = arrayOf("%Status Saver - Video Download%")
            Log.d(TAG, "Querying image collection: $imageCollection with selection: $imageSelection and args: $imageSelectionArgs")

            val imageCursor = requireContext().contentResolver.query(
                imageCollection,
                imageProjection,
                imageSelection,
                imageSelectionArgs,
                null
            )

            if (imageCursor != null) {
                Log.d(TAG, "Image cursor obtained with ${imageCursor.count} entries")
                val idColumn = imageCursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val displayNameColumn = imageCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dataColumn = imageCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

                mediaList.clear() // Clear the list before adding new items

                while (imageCursor.moveToNext()) {
                    val id = imageCursor.getLong(idColumn)
                    val displayName = imageCursor.getString(displayNameColumn)
                    val data = imageCursor.getString(dataColumn)

                    Log.d(TAG, "Found image - ID: $id, DisplayName: $displayName, Data: $data")

                    if (data.contains("Status Saver - Video Download")) {
                        mediaList.add(MediaItem(id, displayName, data, isVideo = false))
                    }
                }
                imageCursor.close()
            } else {
                Log.d(TAG, "No image cursor obtained")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No images found in the directory", Toast.LENGTH_SHORT).show()
                }
            }

            // Query videos
            val videoCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA
            )
            val videoSelection = "${MediaStore.Video.Media.DATA} LIKE ?"
            val videoSelectionArgs = arrayOf("%Status Saver - Video Download%")
            Log.d(TAG, "Querying video collection: $videoCollection with selection: $videoSelection and args: $videoSelectionArgs")

            val videoCursor = requireContext().contentResolver.query(
                videoCollection,
                videoProjection,
                videoSelection,
                videoSelectionArgs,
                null
            )

            if (videoCursor != null) {
                Log.d(TAG, "Video cursor obtained with ${videoCursor.count} entries")
                val idColumn = videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val displayNameColumn = videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataColumn = videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

                while (videoCursor.moveToNext()) {
                    val id = videoCursor.getLong(idColumn)
                    val displayName = videoCursor.getString(displayNameColumn)
                    val data = videoCursor.getString(dataColumn)

                    Log.d(TAG, "Found video - ID: $id, DisplayName: $displayName, Data: $data")

                    if (data.contains("Status Saver - Video Download")) {
                        mediaList.add(MediaItem(id, displayName, data, isVideo = true))
                    }
                }
                videoCursor.close()
            } else {
                Log.d(TAG, "No video cursor obtained")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No videos found in the directory", Toast.LENGTH_SHORT).show()
                }
            }

            withContext(Dispatchers.Main) {
                adapter.notifyDataSetChanged()
                updateUI()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission granted")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    fetchMediaForAndroid11AndAbove()
                } else {
                    fetchMediaForAndroid10AndBelow()
                }
            } else {
                Log.d(TAG, "Permission denied")
                Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val TAG = "DownloadsFragment"
        private const val REQUEST_PERMISSION_CODE = 1
    }
}
