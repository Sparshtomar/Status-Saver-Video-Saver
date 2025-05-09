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

object ImageUtils {

    private const val STORAGE_PERMISSION_CODE = 100
    private const val SAVED_IMAGE_PATHS_KEY = "saved_image_paths"

    fun checkStoragePermission(context: Context, imageUri: Uri?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No need to request permission on Android 10 (API level 29) and above
            imageUri?.let { uri ->
                saveImage(context, uri)
            } ?: run {
                Log.e("ImageUtils", "No image to save")
                showToast(context, "No image to save")
            }
        } else {
            // Request WRITE_EXTERNAL_STORAGE permission for devices below Android 10
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                imageUri?.let { uri ->
                    saveImage(context, uri)
                } ?: run {
                    Log.e("ImageUtils", "No image to save")
                    showToast(context, "No image to save")
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

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray, context: Context, imageUri: Uri?) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                imageUri?.let { uri ->
                    saveImage(context, uri)
                } ?: run {
                    Log.e("ImageUtils", "No image to save")
                    showToast(context, "No image to save")
                }
            } else {
                showToast(context, "Permission denied, cannot save image")
            }
        }
    }

    private fun saveImage(context: Context, imageUri: Uri) {
        val resolver: ContentResolver = context.contentResolver
        val imagePath = imageUri.toString()
        val originalFileName = imagePath.substringAfterLast("%2F")

        // Check if the image is already saved
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (isImageAlreadySaved(context, originalFileName)) {
                Log.d("ImageUtils", "Image already saved: $originalFileName")
                showToast(context, "Image already saved")
                return
            }

            // Save the image using MediaStore
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, originalFileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Status Saver - Video Download")
                }

                val insertedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                insertedUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        resolver.openInputStream(imageUri)?.use { inputStream ->
                            val buffer = ByteArray(4 * 1024)
                            var read: Int
                            while (inputStream.read(buffer).also { read = it } != -1) {
                                outputStream.write(buffer, 0, read)
                            }
                            outputStream.flush()
                            Log.d("ImageUtils", "Image saved successfully: ${uri.path}")
                            showToast(context, "Image saved")
                        }
                    }
                } ?: run {
                    Log.e("ImageUtils", "Failed to save image")
                    showToast(context, "Failed to save image")
                }
            } catch (e: IOException) {
                Log.e("ImageUtils", "IOException while saving image: ${e.message}")
                showToast(context, "Failed to save image")
            }
        } else {
            if (isImageAlreadySavedFile(context, originalFileName)) {
                Log.d("ImageUtils", "Image already saved: $originalFileName")
                showToast(context, "Image already saved")
                return
            }

            // Save the image using file operations
            try {
                val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Status Saver - Video Download")
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                val file = File(directory, originalFileName)
                context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        val buffer = ByteArray(4 * 1024)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                        }
                        outputStream.flush()
                        Log.d("ImageUtils", "Image saved successfully: ${file.path}")
                        showToast(context, "Image saved")
                    }
                }
            } catch (e: IOException) {
                Log.e("ImageUtils", "IOException while saving image: ${e.message}")
                showToast(context, "Failed to save image")
            }
        }
    }

    private fun isImageAlreadySaved(context: Context, fileName: String): Boolean {
        val resolver = context.contentResolver
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        val cursor = resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        val imageExists = cursor?.use { it.count > 0 } ?: false
        Log.d("ImageUtils", "Checking if image exists: $fileName -> $imageExists")
        return imageExists
    }

    private fun isImageAlreadySavedFile(context: Context, fileName: String): Boolean {
        val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Status Saver - Video Download")
        val file = File(directory, fileName)
        val fileExists = file.exists()
        Log.d("ImageUtils", "Checking if image exists (file): $fileName -> $fileExists")
        return fileExists
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

}
