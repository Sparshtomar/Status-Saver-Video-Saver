package com.sparsh.statussaver_videodownload.UI.Activities

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.sparsh.statussaver_videodownload.UI.ADS.InterstitialAdHelper
import com.sparsh.statussaver_videodownload.databinding.ActivityVideoPreviewBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class VideoPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPreviewBinding
    private lateinit var videos: List<String>
    private val STORAGE_PERMISSION_CODE = 100
    private lateinit var exoPlayer: ExoPlayer
    private var position: Int = 0
    private lateinit var fragmentType: String
    private val adUnitId = "ca-app-pub-7713317467402311/5772127469"

    companion object {
        private const val VIDEOS_KEY = "videos"
        private const val POSITION_KEY = "position"
        private const val FRAGMENT_TYPE_KEY = "fragment_type"

        const val FRAGMENT_STATUS = "status_fragment"
        const val FRAGMENT_BUSINESS = "business_fragment"

        fun newIntent(context: Context, videos: List<String>, position: Int, fragmentType: String): Intent {
            val intent = Intent(context, VideoPreviewActivity::class.java)
            intent.putExtra(VIDEOS_KEY, videos.toTypedArray())
            intent.putExtra(POSITION_KEY, position)
            intent.putExtra(FRAGMENT_TYPE_KEY, fragmentType)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)


        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        InterstitialAdHelper.loadInterstitialAd(this,adUnitId)

        exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer

        videos = intent.getStringArrayExtra(VIDEOS_KEY)?.toList() ?: emptyList()
        position = intent.getIntExtra(POSITION_KEY, -1)
        fragmentType = intent.getStringExtra(FRAGMENT_TYPE_KEY) ?: FRAGMENT_STATUS

        val position = intent.getIntExtra(POSITION_KEY, -1)
        if (position != -1 && position < videos.size) {
            setupVideoPlayer(videos[position])
        } else {
            showToast("Invalid video position")
        }


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
                shareVideo()
                InterstitialAdHelper.loadInterstitialAd(this,adUnitId) // Load new ad after task
            }
        }
    }



    private fun setupVideoPlayer(videoUri: String) {
        try {
            val uri = Uri.parse(videoUri)
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (e: Exception) {
            Log.e("VideoPreviewActivity", "Error setting up video player: ${e.message}")
            showToast("Error setting up video player")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }


    private fun addToStatus() {
        val position = intent.getIntExtra(POSITION_KEY, -1)
        if (position != -1 && position < videos.size) {
            val videoUri = Uri.parse(videos[position]) // Get the correct video URI
            // Decide which package to use based on fragment type
            val packageName = if (fragmentType == FRAGMENT_BUSINESS) {
                "com.whatsapp.w4b" // WhatsApp Business
            } else {
                "com.whatsapp" // Regular WhatsApp
            }
            shareVideoThroughWA(videoUri, packageName)
        } else {
            showToast("Invalid video position")
        }
    }

    private fun shareVideoThroughWA(videoUri: Uri, packageName: String) {
        try {

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, videoUri)
                type = "video/*"
                setPackage(packageName)
            }

            // Grant read permission to the intent
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            // Start the chooser dialog with a custom title
            startActivity(Intent.createChooser(shareIntent, "Share Video via..."))
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("VideoPreviewActivity", "Exception while sharing video: ${e.message}")
            showToast("Failed to share video")
        }
    }

    private fun shareVideo() {
        val position = intent.getIntExtra(POSITION_KEY, -1)
        if (position != -1 && position < videos.size) {
            val videoUri = Uri.parse(videos[position]) // Get the correct video URI
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, videoUri)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Video via..."))
        } else {
            showToast("Invalid video position")
        }
    }


    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No need to request permission on Android 10 (API level 29) and above
            saveVideo()
        } else {
            // Request WRITE_EXTERNAL_STORAGE permission for devices below Android 10
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                saveVideo()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
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
                saveVideo()
            } else {
                showToast("Permission denied, cannot save video")
            }
        }
    }



    private fun saveVideo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Save video using MediaStore on Android 10 and above
            saveVideoUsingMediaStore()
        } else {
            // Save video using file operations for devices below Android 10
            saveVideoUsingFileOperations()
        }
    }

    private fun saveVideoUsingMediaStore() {
        val videoUri = Uri.parse(videos[position])
        val originalFileName = videoUri.toString().substringAfterLast("%2F")
      //  val originalFileName = videoUri.lastPathSegment ?: "video_${System.currentTimeMillis()}.mp4"

        if (isVideoAlreadySaved(originalFileName)) {
            showToast("Video already saved")
            return
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, originalFileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH,"${Environment.DIRECTORY_PICTURES}/Status Saver - Video Download")
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let { safeUri ->
            resolver.openOutputStream(safeUri)?.use { outputStream ->
                contentResolver.openInputStream(videoUri)?.use { inputStream ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                    showToast("Video saved successfully")
                }
            }
        } ?: run {
            showToast("Failed to save video")
        }
    }

    private fun saveVideoUsingFileOperations() {
        val videoUri = Uri.parse(videos[position])
        val originalFileName = videoUri.toString().substringAfterLast("%2F")
//        val originalFileName = videoUri.lastPathSegment ?: "video_${System.currentTimeMillis()}.mp4"
        val directory = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Status Saver - Video Download")

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, originalFileName)

        if (isVideoAlreadySavedFile(originalFileName)) {
            showToast("Video already saved")
            return
        }

        try {
            FileOutputStream(file).use { outputStream ->
                contentResolver.openInputStream(videoUri)?.use { inputStream ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                    showToast("Video saved successfully")
                }
            }
        } catch (e: IOException) {
            Log.e("VideoPreviewActivity", "Error saving video: ${e.message}")
            showToast("Failed to save video")
        }
    }

    private fun isVideoAlreadySaved(fileName: String): Boolean {
        val resolver = contentResolver
        val projection = arrayOf(MediaStore.Video.Media._ID)
        val selection = "${MediaStore.Video.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        val cursor = resolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        val videoExists = cursor?.use { it.count > 0 } ?: false
        Log.d("VideoPreviewActivity", "Checking if image exists: $fileName -> $videoExists")
        return videoExists
    }

    private fun isVideoAlreadySavedFile(fileName: String): Boolean {
        val directory = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Status Saver - Video Download")
        val file = File(directory, fileName)
        val fileExists = file.exists()
        Log.d("VideoPreviewActivity", "Checking if video exists (file): $fileName -> $fileExists")
        return fileExists
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
