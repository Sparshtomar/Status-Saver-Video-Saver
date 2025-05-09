package com.sparsh.statussaver_videodownload.UI.Activities

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.sparsh.statussaver_videodownload.R
import com.sparsh.statussaver_videodownload.UI.ADS.InterstitialAdHelper
import com.sparsh.statussaver_videodownload.UI.Adapters.MediaPagerAdapter
import com.sparsh.statussaver_videodownload.UI.Data.MediaItem
import com.sparsh.statussaver_videodownload.databinding.ActivityMediaPreviewBinding
import java.io.File

@Suppress("DEPRECATION")
class MediaPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaPreviewBinding
    private lateinit var viewPager: ViewPager2
    private lateinit var mediaList: List<MediaItem>
    private lateinit var mediaAdapter: MediaPagerAdapter
    private val STORAGE_PERMISSION_CODE = 151
    private val adUnitId = "ca-app-pub-7713317467402311/5772127469"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        InterstitialAdHelper.loadInterstitialAd(this,adUnitId)

        binding = ActivityMediaPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        viewPager = findViewById(R.id.viewPager)

        binding.addtostatusbutton.setOnClickListener {
            InterstitialAdHelper.showInterstitialAd(this,adUnitId) {
                addToStatus()
                InterstitialAdHelper.loadInterstitialAd(this,adUnitId) // Load new ad after task
            }
        }

        binding.savebutton.setOnClickListener {
            InterstitialAdHelper.showInterstitialAd(this,adUnitId) {
                checkStoragePermission()
                InterstitialAdHelper.loadInterstitialAd(this,adUnitId) // Load new ad after task
            }

        }

        binding.sharebutton.setOnClickListener {
            InterstitialAdHelper.showInterstitialAd(this,adUnitId) {
                shareMedia()
                InterstitialAdHelper.loadInterstitialAd(this,adUnitId) // Load new ad after task
            }
        }

        mediaList = intent.getParcelableArrayListExtra("mediaList") ?: emptyList()
        val startPosition = intent.getIntExtra("position", 0)

        mediaAdapter = MediaPagerAdapter(mediaList)
        viewPager.adapter = mediaAdapter
        viewPager.setCurrentItem(startPosition, false)

        mediaAdapter.startPlayback(startPosition)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                Log.d(TAG, "onPageSelected: Switching to position $position")
                mediaAdapter.startPlayback(position)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Activity destroyed")
        mediaAdapter.releasePlayers()
    }

    companion object {
        private const val TAG = "MediaPreviewActivity"
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No need to request permission on Android 10 (API level 29) and above
            deleteMedia()
        } else {
            // Request WRITE_EXTERNAL_STORAGE permission for devices below Android 10
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                deleteMedia()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                deleteMedia()
            } else {
                showToast("Permission denied, cannot save media")
            }
        }
    }

    private fun addToStatus() {
        val currentMediaPosition = binding.viewPager.currentItem
        val currentMediaItem = mediaList.getOrNull(currentMediaPosition)

        currentMediaItem?.let { item ->
            val mediaUri = Uri.parse(item.data)
            val mimeType = if (item.isVideo) "video/*" else "image/*"

            // Create explicit intents for WhatsApp and WhatsApp Business
            val whatsappIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, mediaUri)
                type = mimeType
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val whatsappBusinessIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, mediaUri)
                type = mimeType
                setPackage("com.whatsapp.w4b")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Create a custom dialog to let the user choose between WhatsApp and WhatsApp Business
            val shareIntent = Intent.createChooser(whatsappIntent, "Share Media via...")
            shareIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(whatsappBusinessIntent))

            // Start the activity
            startActivity(shareIntent)
        } ?: showToast("No media path found")
    }


    private fun shareMedia() {
        val currentMediaPosition = binding.viewPager.currentItem
        val currentMediaItem = mediaList.getOrNull(currentMediaPosition)

        currentMediaItem?.let { item ->
            val mediaUri = Uri.parse(item.data)
            val mimeType = if (item.isVideo) "video/*" else "image/*"

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, mediaUri)
            }
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(shareIntent, "Share Media via..."))
        } ?: showToast("No media to share")
    }


    private fun deleteMedia() {
        val currentMediaPosition = binding.viewPager.currentItem

        if (currentMediaPosition in mediaList.indices) {
            val mediaItem = mediaList[currentMediaPosition]
            val filePath = mediaItem.data

            if (filePath.isNotEmpty()) {
                if (deleteFile(filePath)) {
                    mediaList = mediaList.toMutableList().apply { removeAt(currentMediaPosition) }
                    mediaAdapter.updateData(mediaList)

                    // Release player for deleted video (if any)
                    mediaAdapter.getViewHolderForPosition(currentMediaPosition)?.releasePlayer()

                    // Determine new position after deletion
                    val newPosition = if (currentMediaPosition < mediaList.size) {
                        currentMediaPosition
                    } else {
                        mediaList.size - 1 // Move to the last item if current position was out of bounds
                    }

                    if (mediaList.isEmpty()) {
                        finish()
                    } else {
                        viewPager.setCurrentItem(newPosition, true)
                    }

                    if (mediaItem.isVideo) {
                        showToast("Video Deleted Successfully")
                    } else {
                        showToast("Image Deleted Successfully")
                    }
                } else {
                    Log.e(TAG, "Failed to delete file: $filePath")
                    showToast("Failed to delete media")
                }
            } else {
                Log.e(TAG, "Invalid media path: $filePath")
                showToast("Invalid media path")
            }
        } else {
            Log.e(TAG, "Invalid media position: $currentMediaPosition")
            showToast("Invalid media position")
        }
    }



    override fun deleteFile(filePath: String): Boolean {
        val file = File(filePath)
        if (file.exists()) {
            return file.delete()
        } else {
            Log.e(TAG, "File not found: $filePath")
            return false
        }
    }


    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@MediaPreviewActivity, message, Toast.LENGTH_SHORT).show()
        }
    }
}
