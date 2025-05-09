package com.sparsh.statussaver_videodownload.UI.Activities

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sparsh.statussaver_videodownload.UI.ADS.AdHelper
import com.sparsh.statussaver_videodownload.databinding.ActivityHelpBinding

@Suppress("DEPRECATION")
class HelpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpBinding
    private val adUnitId = "ca-app-pub-7713317467402311/6200606525"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AdHelper.loadBannerAd(this, binding.adViewContainer, adUnitId)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        binding.settingsbackbtn.setOnClickListener {
            onBackPressed()
        }
    }
}