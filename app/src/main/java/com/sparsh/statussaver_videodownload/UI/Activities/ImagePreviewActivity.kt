package com.sparsh.statussaver_videodownload.UI.Activities

import android.Manifest
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
import com.sparsh.statussaver_videodownload.UI.ADS.InterstitialAdHelper
import com.sparsh.statussaver_videodownload.UI.Adapters.ImagePreviewAdapter
import com.sparsh.statussaver_videodownload.databinding.ActivityImagePreviewBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImagePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImagePreviewBinding
    private lateinit var images: List<String>
    private var position: Int = 0
    private val STORAGE_PERMISSION_CODE = 100
    private lateinit var fragmentType: String
    private val adUnitId = "ca-app-pub-7713317467402311/5772127469"

    companion object {
        private const val IMAGES_KEY = "images"
        private const val POSITION_KEY = "position"
        private const val FRAGMENT_TYPE_KEY = "fragment_type"

        const val FRAGMENT_STATUS = "status_fragment"
        const val FRAGMENT_BUSINESS = "business_fragment"

        fun newIntent(context: Context, images: List<String>, position: Int, fragmentType: String): Intent {
            val intent = Intent(context, ImagePreviewActivity::class.java)
            intent.putExtra(IMAGES_KEY, images.toTypedArray())
            intent.putExtra(POSITION_KEY, position)
            intent.putExtra(FRAGMENT_TYPE_KEY, fragmentType)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        InterstitialAdHelper.loadInterstitialAd(this, adUnitId)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        images = intent.getStringArrayExtra(IMAGES_KEY)?.toList() ?: emptyList()
        position = intent.getIntExtra(POSITION_KEY, 0)
        fragmentType = intent.getStringExtra(FRAGMENT_TYPE_KEY) ?: FRAGMENT_STATUS

        setupViewPager()

        binding.addtostatusbutton.setOnClickListener {
            InterstitialAdHelper.showInterstitialAd(this,adUnitId) {
                addToStatus()
                InterstitialAdHelper.loadInterstitialAd(this,adUnitId) // Load new ad after task
            }
        }

        binding.savebutton.setOnClickListener {
            InterstitialAdHelper.showInterstitialAd(this,adUnitId) {
                checkStoragePermission()
                InterstitialAdHelper.loadInterstitialAd(this,adUnitId)
            }
        }

        binding.sharebutton.setOnClickListener {
            InterstitialAdHelper.showInterstitialAd(this,adUnitId) {
                shareImage()
                InterstitialAdHelper.loadInterstitialAd(this,adUnitId)
            }
        }
    }

    private fun setupViewPager() {
        val adapter = ImagePreviewAdapter(images)
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(position, false)
    }

    private fun addToStatus() {
        val currentImagePosition = binding.viewPager.currentItem
        val currentImagePath = images.getOrNull(currentImagePosition)

        currentImagePath?.let { imagePath ->
            Log.d("ImagePreviewActivity", "Adding image to status: $imagePath")

            val originalFileName = imagePath.substringAfterLast("%2F")

            // Decide which package to use based on fragment type
            val packageName = if (fragmentType == FRAGMENT_BUSINESS) {
                "com.whatsapp.w4b" // WhatsApp Business
            } else {
                "com.whatsapp" // Regular WhatsApp
            }

            shareImageThroughWA(Uri.parse(imagePath), packageName)
        } ?: showToast("No image path found")
    }

    private fun shareImageThroughWA(imageUri: Uri, packageName: String) {
        try {

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, imageUri)
                type = "image/*"
                setPackage(packageName)
            }

            // Grant read permission to the intent
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            // Start the chooser dialog with a custom title
            startActivity(Intent.createChooser(shareIntent, "Share Image via..."))
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ImagePreviewActivity", "Exception while sharing image: ${e.message}")
            showToast("Failed to share image")
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No need to request permission on Android 10 (API level 29) and above
            saveImage()
        } else {
            // Request WRITE_EXTERNAL_STORAGE permission for devices below Android 10
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                saveImage()
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
                saveImage()
            } else {
                showToast("Permission denied, cannot save image")
            }
        }
    }

    private fun saveImage() {
        val currentImagePosition = binding.viewPager.currentItem
        val currentImagePath = images.getOrNull(currentImagePosition)

        currentImagePath?.let { imagePath ->
            // Extract the original file name from the image path
            val originalFileName = imagePath.substringAfterLast("%2F")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (isImageAlreadySaved(originalFileName)) {
                    Log.d("ImagePreviewActivity", "Image already saved: $originalFileName")
                    showToast("Image already saved")
                    return
                }

                // Continue with saving the image via MediaStore
                try {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, originalFileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Status Saver - Video Download")
                    }

                    val resolver = contentResolver
                    val insertedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                    insertedUri?.let { uri ->
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            contentResolver.openInputStream(Uri.parse(imagePath))?.use { inputStream ->
                                val buffer = ByteArray(4 * 1024)
                                var read: Int
                                while (inputStream.read(buffer).also { read = it } != -1) {
                                    outputStream.write(buffer, 0, read)
                                }
                                outputStream.flush()
                                Log.d("ImagePreviewActivity", "Image saved successfully: ${uri.path}")
                                showToast("Image saved")
                            }
                        }
                    } ?: run {
                        Log.e("ImagePreviewActivity", "Failed to save image to MediaStore")
                        showToast("Failed to save image")
                    }
                } catch (e: Exception) {
                    Log.e("ImagePreviewActivity", "Exception while saving image: ${e.message}")
                    showToast("Failed to save image")
                }
            } else {
                if (isImageAlreadySavedFile(originalFileName)) {
                    Log.d("ImagePreviewActivity", "Image already saved: $originalFileName")
                    showToast("Image already saved")
                    return
                }

                // Save the image using file operations
                try {
                    val directory = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Status Saver - Video Download")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }

                    val file = File(directory, originalFileName)
                    contentResolver.openInputStream(Uri.parse(imagePath))?.use { inputStream ->
                        FileOutputStream(file).use { outputStream ->
                            val buffer = ByteArray(4 * 1024)
                            var read: Int
                            while (inputStream.read(buffer).also { read = it } != -1) {
                                outputStream.write(buffer, 0, read)
                            }
                            outputStream.flush()
                            Log.d("ImagePreviewActivity", "Image saved successfully: ${file.path}")
                            showToast("Image saved")
                        }
                    }
                } catch (e: IOException) {
                    Log.e("ImagePreviewActivity", "Exception while saving image: ${e.message}")
                    showToast("Failed to save image")
                }
            }
        } ?: run {
            Log.e("ImagePreviewActivity", "No image path found to save")
            showToast("No image to save")
        }
    }

    private fun isImageAlreadySaved(fileName: String): Boolean {
        val resolver = contentResolver
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
        Log.d("ImagePreviewActivity", "Checking if image exists: $fileName -> $imageExists")
        return imageExists
    }

    private fun isImageAlreadySavedFile(fileName: String): Boolean {
        val directory = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Status Saver - Video Download")
        val file = File(directory, fileName)
        val fileExists = file.exists()
        Log.d("ImagePreviewActivity", "Checking if image exists (file): $fileName -> $fileExists")
        return fileExists
    }

    private fun shareImage() {
        // Get the current image path
        val currentImagePosition = binding.viewPager.currentItem
        val currentImagePath = images.getOrNull(currentImagePosition)

        currentImagePath?.let { imagePath ->
            // Create an intent to share the image
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, Uri.parse(imagePath))
            }

            // Grant read permission to the intent
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            // Start the chooser dialog with a custom title
            startActivity(Intent.createChooser(shareIntent, "Share Image via..."))
        } ?: showToast("No image to share")
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@ImagePreviewActivity, message, Toast.LENGTH_SHORT).show()
        }
    }
}
