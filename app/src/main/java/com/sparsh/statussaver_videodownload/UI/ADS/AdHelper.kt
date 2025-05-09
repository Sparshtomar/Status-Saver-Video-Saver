package com.sparsh.statussaver_videodownload.UI.ADS

import android.content.Context
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

object AdHelper {

    fun loadBannerAd(activity: Context, adContainer: FrameLayout, adUnitId: String) {
        // Remove any existing views in the ad container
        adContainer.removeAllViews()

        // Create a new AdView instance every time to avoid reuse issues
        val adView = AdView(activity).apply {
            this.adUnitId = adUnitId
            this.adListener = object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    super.onAdFailedToLoad(adError)
                    adContainer.removeAllViews()  // Hide ad if failed to load
                }
            }
        }

        // Wait for layout to be measured before setting ad size
        adContainer.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                adContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val adSize = getAdSize(activity, adContainer)
                adView.setAdSize(adSize)
                adContainer.addView(adView)

                // Load the ad
                val adRequest = AdRequest.Builder().build()
                adView.loadAd(adRequest)
            }
        })
    }

    private fun getAdSize(activity: Context, adContainer: FrameLayout): AdSize {
        val adWidthPixels = adContainer.width.takeIf { it > 0 } ?: activity.resources.displayMetrics.widthPixels
        val density = activity.resources.displayMetrics.density
        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }
}
