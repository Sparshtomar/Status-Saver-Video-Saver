@file:Suppress("DEPRECATION")

package com.sparsh.statussaver_videodownload.UI.Fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.sparsh.statussaver_videodownload.UI.ADS.AdHelper
import com.sparsh.statussaver_videodownload.UI.Adapters.StatusImageAdapter
import com.sparsh.statussaver_videodownload.databinding.FragmentImageSBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageSFragment : Fragment() {

    private lateinit var binding: FragmentImageSBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var selectedFolderUri: Uri? = null
    private val adUnitIdBanner = "ca-app-pub-7713317467402311/6200606525"

    private val PATH_URI_ANDROID =
        Uri.parse("content://com.android.externalstorage.documents/document/primary%3AWhatsApp%2FMedia%2F.Statuses")
    private val PATH_URI_ANDROID_11 =
        Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentImageSBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        AdHelper.loadBannerAd(requireActivity(), binding.adViewContainer, adUnitIdBanner)

        // Check if folder URI is already stored
        selectedFolderUri = getStoredFolderUri()

        if (selectedFolderUri != null) {
            Log.d("StatusFragment", "Using stored folder URI")
            // Start loading images asynchronously
            loadStatusImages()
        } else {
            binding.noPermissionLayout.visibility = View.VISIBLE
            binding.button.setOnClickListener {
                getFolderPermission()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if we need to reload images
        selectedFolderUri?.let {
            loadStatusImages()
        }
    }

    private fun getFolderPermission() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, getUri())
            putExtra("android.content.extra.SHOW_ADVANCED", true)
        }
        startActivityForResult(intent, REQUEST_CODE_FOLDER_PERMISSION)
    }

    private fun getUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            PATH_URI_ANDROID_11
        } else {
            PATH_URI_ANDROID
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FOLDER_PERMISSION && resultCode == Activity.RESULT_OK) {
            data?.data?.let { treeUri ->
                requireContext().contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                saveFolderUri(treeUri)
                selectedFolderUri = treeUri
                // Start loading images asynchronously
                loadStatusImages()
            } ?: Toast.makeText(requireContext(), "Permission Denied!", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadStatusImages() {
        binding.noPermissionLayout.visibility = View.GONE
        // Run the image loading in a background thread
        CoroutineScope(Dispatchers.IO).launch {
            val statusImages = getStatusImages()
            withContext(Dispatchers.Main) {
                if (statusImages.isEmpty()) {
                    binding.noImagesLayout.visibility = View.VISIBLE
                    binding.statusImagerecv.visibility = View.GONE
                } else {
                    binding.noImagesLayout.visibility = View.GONE
                    binding.statusImagerecv.visibility = View.VISIBLE
                    binding.statusImagerecv.layoutManager = GridLayoutManager(requireContext(), 2)
                    binding.statusImagerecv.adapter = StatusImageAdapter(statusImages, requireActivity() as AppCompatActivity)
                }
            }
        }
    }

    private fun getStatusImages(): List<String> {
        val statusImages = mutableListOf<String>()
        try {
            selectedFolderUri?.let { uri ->
                val documentFile = DocumentFile.fromTreeUri(requireContext(), uri)
                documentFile?.listFiles()?.forEach { file ->
                    if (file.isFile && (file.name?.endsWith(".jpg") == true || file.name?.endsWith(".png") == true)) {
                        statusImages.add(file.uri.toString())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("StatusFragment", "Error getting status images: ${e.message}")
        }
        return statusImages
    }

    private fun saveFolderUri(uri: Uri) {
        sharedPreferences.edit().putString("folder_uri", uri.toString()).apply()
    }

    private fun getStoredFolderUri(): Uri? {
        val uriString = sharedPreferences.getString("folder_uri", null)
        return uriString?.let { Uri.parse(it) }
    }

    companion object {
        private const val REQUEST_CODE_FOLDER_PERMISSION = 1234
    }
}