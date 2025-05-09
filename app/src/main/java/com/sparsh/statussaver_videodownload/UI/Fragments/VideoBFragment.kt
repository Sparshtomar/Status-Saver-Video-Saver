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
import com.sparsh.statussaver_videodownload.UI.Adapters.BstatusVideoAdapter
import com.sparsh.statussaver_videodownload.databinding.FragmentVideoBBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class VideoBFragment : Fragment() {

    private lateinit var binding: FragmentVideoBBinding
    private lateinit var sharedPreferencesforBusiness: SharedPreferences
    private var selectedFolderUriforBusiness: Uri? = null
    private val adUnitIdBanner = "ca-app-pub-7713317467402311/6200606525"

    private val BUSINESS_PATH_URI_ANDROID =
        Uri.parse("content://com.android.externalstorage.documents/document/primary%3AWhatsApp%20Business%2FMedia%2F.Statuses")
    private val BUSINESS_PATH_URI_ANDROID_11 =
        Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fmedia%2Fcom.whatsapp.w4b%2FWhatsApp%20Business%2FMedia%2F.Statuses")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentVideoBBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPreferencesforBusiness = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        AdHelper.loadBannerAd(requireActivity(), binding.adViewContainer, adUnitIdBanner)

        // Check if folder URI is already stored
        selectedFolderUriforBusiness = getStoredFolderUriforB()

        if (selectedFolderUriforBusiness != null) {
            Log.d("StatusFragment", "Using stored folder URI")
            loadStatusVideos()
        } else {
            binding.noPermissionLayout.visibility = View.VISIBLE
            binding.button.setOnClickListener {
                checkAndRequestPermissions()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (selectedFolderUriforBusiness != null) {
            loadStatusVideos()
        }
    }

    private fun checkAndRequestPermissions() {
        val businessDirPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "/storage/emulated/0/Android/media/com.whatsapp.w4b/WhatsApp Business/Media"
        } else {
            "/storage/emulated/0/WhatsApp Business/Media"
        }
        val businessDir = File(businessDirPath)
        if (businessDir.exists() && businessDir.isDirectory) {
            getFolderPermission()
        } else {
            Toast.makeText(requireContext(), "App is not set up or installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFolderPermission() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, getBusinessUri())
            putExtra("android.content.extra.SHOW_ADVANCED", true)
        }
        startActivityForResult(intent, REQUEST_CODE_FOLDER_PERMISSION)
    }

    private fun getBusinessUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BUSINESS_PATH_URI_ANDROID_11
        } else {
            BUSINESS_PATH_URI_ANDROID
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
                saveFolderUriforB(treeUri)
                selectedFolderUriforBusiness = treeUri
                loadStatusVideos()
            } ?: Toast.makeText(requireContext(), "Permission Denied!", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadStatusVideos() {
        binding.noPermissionLayout.visibility = View.GONE
        CoroutineScope(Dispatchers.IO).launch {
            val statusVideos = getStatusVideos()
            withContext(Dispatchers.Main) {
                if (statusVideos.isEmpty()) {
                    binding.noVideosLayout.visibility = View.VISIBLE
                    binding.statusImagerecv.visibility = View.GONE
                } else {
                    binding.noVideosLayout.visibility = View.GONE
                    binding.statusImagerecv.visibility = View.VISIBLE
                    binding.statusImagerecv.layoutManager = GridLayoutManager(requireContext(), 2)
                    binding.statusImagerecv.adapter = BstatusVideoAdapter(statusVideos, requireActivity() as AppCompatActivity)
                }
            }
        }
    }

    private fun getStatusVideos(): List<String> {
        val statusVideos = mutableListOf<String>()
        try {
            selectedFolderUriforBusiness?.let { uri ->
                val documentFile = DocumentFile.fromTreeUri(requireContext(), uri)
                documentFile?.listFiles()?.forEach { file ->
                    if (file.isFile && file.name?.endsWith(".mp4") == true) {
                        statusVideos.add(file.uri.toString())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("StatusFragment", "Error getting status videos: ${e.message}")
        }
        return statusVideos
    }

    private fun saveFolderUriforB(uri: Uri) {
        sharedPreferencesforBusiness.edit().putString("folder_uriforB", uri.toString()).apply()
    }

    private fun getStoredFolderUriforB(): Uri? {
        val uriString = sharedPreferencesforBusiness.getString("folder_uriforB", null)
        return uriString?.let { Uri.parse(it) }
    }

    companion object {
        private const val REQUEST_CODE_FOLDER_PERMISSION = 9999
    }
}
