package com.sparsh.statussaver_videodownload.UI.Utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.File

object DeleteUtils {
    fun deleteMedia(context: Context, mediaUri: Uri, filePath: String, isVideo: Boolean): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                deleteMediaFromMediaStore(context, mediaUri, filePath, isVideo)
            } else {
                // For Android 9 and below, delete the file directly
                val file = File(filePath)
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("DeleteUtils", "Error deleting media: ${e.message}")
            false
        }
    }

    private fun deleteMediaFromMediaStore(context: Context, mediaUri: Uri, filePath: String, isVideo: Boolean): Boolean {
        return try {
            val contentResolver = context.contentResolver
            val uri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val whereClause = "${MediaStore.Images.Media.DATA} = ?"
            val selectionArgs = arrayOf(filePath)
            val rowsDeleted = contentResolver.delete(uri, whereClause, selectionArgs)
            Log.d("DeleteUtils", "Rows deleted: $rowsDeleted")
            rowsDeleted > 0
        } catch (e: Exception) {
            Log.e("DeleteUtils", "Error deleting media from MediaStore: ${e.message}")
            false
        }
    }
}

