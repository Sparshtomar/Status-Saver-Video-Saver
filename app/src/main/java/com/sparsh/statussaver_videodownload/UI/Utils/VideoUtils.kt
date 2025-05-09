package com.sparsh.statussaver_videodownload.UI.Utils

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object VideoUtils {

    private const val STORAGE_PERMISSION_CODE = 100
    private const val SAVED_VIDEO_PATHS_KEY = "saved_video_paths"

    fun checkStoragePermission(context: Context, videoUri: Uri?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No need to request permission on Android 10 (API level 29) and above
            videoUri?.let { uri ->
                saveVideo(context, uri)
            } ?: run {
                Log.e("VideoUtils", "No video to save")
                showToast(context, "No video to save")
            }
        } else {
            // Request WRITE_EXTERNAL_STORAGE permission for devices below Android 10
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                videoUri?.let { uri ->
                    saveVideo(context, uri)
                } ?: run {
                    Log.e("VideoUtils", "No video to save")
                    showToast(context, "No video to save")
                }
            } else {
                ActivityCompat.requestPermissions(
                    context as androidx.appcompat.app.AppCompatActivity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            }
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray, context: Context, videoUri: Uri?) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                videoUri?.let { uri ->
                    saveVideo(context, uri)
                } ?: run {
                    Log.e("VideoUtils", "No video to save")
                    showToast(context, "No video to save")
                }
            } else {
                showToast(context, "Permission denied, cannot save video")
            }
        }
    }

    private fun saveVideo(context: Context, videoUri: Uri) {
        val resolver: ContentResolver = context.contentResolver
        val videoPath = videoUri.toString()
        val originalFileName = videoPath.substringAfterLast("%2F")

        // Check if the image is already saved
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (isVideoAlreadySaved(context, originalFileName)) {
                Log.d("VideoUtils", "Video already saved: $originalFileName")
                showToast(context, "Video already saved")
                return
            }

            // Save the video using MediaStore
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, originalFileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Status Saver - Video Download")
                }

                val insertedUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

                insertedUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        resolver.openInputStream(videoUri)?.use { inputStream ->
                            val buffer = ByteArray(4 * 1024)
                            var read: Int
                            while (inputStream.read(buffer).also { read = it } != -1) {
                                outputStream.write(buffer, 0, read)
                            }
                            outputStream.flush()
                            Log.d("VideoUtils", "Video saved successfully: ${uri.path}")
                            showToast(context, "Video saved")
                        }
                    }
                } ?: run {
                    Log.e("VideoUtils", "Failed to save video")
                    showToast(context, "Failed to save video")
                }
            } catch (e: IOException) {
                Log.e("VideoUtils", "IOException while saving video: ${e.message}")
                showToast(context, "Failed to save video")
            }
        } else {
            if (isVideoAlreadySavedFile(context, originalFileName)) {
                Log.d("VideoUtils", "Video already saved: $originalFileName")
                showToast(context, "Video already saved")
                return
            }

            // Save the video using file operations
            try {
                val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Status Saver - Video Download")
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                val file = File(directory, originalFileName)
                context.contentResolver.openInputStream(videoUri)?.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        val buffer = ByteArray(4 * 1024)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                        }
                        outputStream.flush()
                        Log.d("VideoUtils", "Video saved successfully: ${file.path}")
                        showToast(context, "Video saved")
                    }
                }
            } catch (e: IOException) {
                Log.e("VideoUtils", "IOException while saving video: ${e.message}")
                showToast(context, "Failed to save video")
            }
        }
    }

    private fun isVideoAlreadySaved(context: Context, fileName: String): Boolean {
        val resolver = context.contentResolver
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
        Log.d("VideoUtils", "Checking if video exists: $fileName -> $videoExists")
        return videoExists
    }

    private fun isVideoAlreadySavedFile(context: Context, fileName: String): Boolean {
        val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Status Saver - Video Download")
        val file = File(directory, fileName)
        val fileExists = file.exists()
        Log.d("VideoUtils", "Checking if video exists (file): $fileName -> $fileExists")
        return fileExists
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

}
