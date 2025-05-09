package com.sparsh.statussaver_videodownload.UI.Activities

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import com.sparsh.statussaver_videodownload.R
import com.sparsh.statussaver_videodownload.UI.Fragments.BstatusFragment
import com.sparsh.statussaver_videodownload.UI.Fragments.ChatFragment
import com.sparsh.statussaver_videodownload.UI.Fragments.DownloadsFragment
import com.sparsh.statussaver_videodownload.UI.Fragments.StatusFragment
import com.sparsh.statussaver_videodownload.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

private lateinit var binding: ActivityMainBinding
private lateinit var viewPager: ViewPager2
private var lastSelectedMenuItemId: Int = R.id.status
private lateinit var NavigationBar: BottomNavigationView
val playStoreUrl = "https://play.google.com/store/apps/details?id=com.sparsh.statussaver_videodownload"

private lateinit var appUpdateManager: AppUpdateManager
private val updateType = AppUpdateType.IMMEDIATE

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(this@MainActivity) {}
        }


        appUpdateManager = AppUpdateManagerFactory.create(this)
        if (updateType == AppUpdateType.FLEXIBLE) {
            appUpdateManager.registerListener(installStateUpdatedlistner)
        }
        checkForAppUpdates()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setSupportActionBar(binding.appToolbar)

        viewPager = binding.viewPager
        NavigationBar = binding.bottomNav

        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = false

        // Restore last selected item if any
        lastSelectedMenuItemId = savedInstanceState?.getInt("selectedFragmentId", R.id.status) ?: R.id.status
        NavigationBar.selectedItemId = lastSelectedMenuItemId

        // Add a listener to update lastSelectedMenuItemId when changing fragments manually
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                lastSelectedMenuItemId = when (position) {
                    0 -> R.id.status
                    1 -> R.id.statusB
                    2 -> R.id.downloads
                    3 -> R.id.chat
                    else -> -1
                }
                NavigationBar.selectedItemId = lastSelectedMenuItemId
            }
        })

        NavigationBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.status -> viewPager.currentItem = 0
                R.id.statusB -> viewPager.currentItem = 1
                R.id.downloads -> viewPager.currentItem = 2
                R.id.chat -> viewPager.currentItem = 3
            }
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        Log.d("MainActivity", "Menu created")
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("MainActivity", "Menu item selected: ${item.itemId}")
        return when (item.itemId) {
            R.id.settings -> {
                // Open the new activity
                val intent = Intent(this, AppSettings::class.java)
                startActivity(intent)
                true
            }

            R.id.help -> {
                val intent = Intent(this,HelpActivity::class.java)
                startActivity(intent)
                true
            }

            R.id.rateApp -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl))
                startActivity(intent)
                true
            }

            R.id.shareApp -> {
                shareApp()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.setType("text/plain")
        val shareBody = "Download and try out our awesome Status Saving app! 'Status Saver - Video Download' $playStoreUrl"
        val shareSubject = "Status Saver - Video Download"

        shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareSubject)
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody)

        startActivity(Intent.createChooser(shareIntent, "Share using"))
    }



    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("selectedFragmentId", lastSelectedMenuItemId)
        super.onSaveInstanceState(outState)
    }

    private inner class ViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 4 // Update count to match number of fragments

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> StatusFragment()
                1 -> BstatusFragment()
                2 -> DownloadsFragment()
                3 -> ChatFragment()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (updateType == AppUpdateType.IMMEDIATE) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        updateType,
                        this,
                        123
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123) {
            when (resultCode) {
                RESULT_OK -> {
                    // The update was successful, handle accordingly
                    Toast.makeText(this, "App updated successfully", Toast.LENGTH_SHORT).show()
                }
                RESULT_CANCELED -> {
                    // The user cancelled the update, force app closure or show a dialog
                    showForceCloseDialog()
                }
                else -> {
                    // The update failed, handle accordingly
                    Toast.makeText(this, "App update failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkForAppUpdates() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            Log.d("UpdateCheck", "Update Availability: ${info.updateAvailability()}")
            Log.d("UpdateCheck", "Flexible Update Allowed: ${info.isFlexibleUpdateAllowed}")
            Log.d("UpdateCheck", "Immediate Update Allowed: ${info.isImmediateUpdateAllowed}")

            val isUpdateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            val isUpdateAllowed = when(updateType) {
                AppUpdateType.FLEXIBLE -> info.isFlexibleUpdateAllowed
                AppUpdateType.IMMEDIATE -> info.isImmediateUpdateAllowed
                else -> false
            }
            if (isUpdateAvailable && isUpdateAllowed) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    updateType,
                    this,
                    123
                )
            }

        }
    }

    private val installStateUpdatedlistner = InstallStateUpdatedListener { state ->
        if(state.installStatus() == InstallStatus.DOWNLOADED) {
            Toast.makeText(
                applicationContext,
                "Download successful. Restarting app in 5 seconds",
                Toast.LENGTH_LONG
            ).show()
            lifecycleScope.launch {
                delay(5.seconds)
                appUpdateManager.completeUpdate()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(updateType == AppUpdateType.FLEXIBLE) {
            appUpdateManager.unregisterListener(installStateUpdatedlistner)
        }
    }

    private fun showForceCloseDialog() {
        val dialogBuilder = AlertDialog.Builder(this) // Use `this` instead of `context`
        dialogBuilder
            .setTitle("Critical Update Required")
            .setMessage("To continue using the app, please update to the latest version.")
            .setCancelable(false)
            .setPositiveButton("Update Now") { dialog, _ ->
                // Optionally, you can restart the update flow here
                checkForAppUpdates()
                dialog.dismiss()
            }
            .setNegativeButton("Exit App") { dialog, _ ->
                // Exit the app if the user chooses not to update (for critical updates)
                dialog.dismiss()
                finish()
                finishAffinity() // Finish all activities in the task
                System.exit(0) // Ensure the app process is terminated
            }

        // Create the AlertDialog
        val alertDialog = dialogBuilder.create()

        // Show the AlertDialog
        alertDialog.show()

        // Set text color for positive button
        if(isDarkTheme()){
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.ok_dark))
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this,R.color.ok_dark))
        } else {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.ok_light))
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this,R.color.ok_light))
        }

        // Set text size for message TextView
        val textView = alertDialog.findViewById<TextView>(android.R.id.message)
        textView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)

    }

    private fun isDarkTheme(): Boolean {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }
}
