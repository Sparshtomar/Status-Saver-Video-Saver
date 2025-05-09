package com.sparsh.statussaver_videodownload.UI.ADS

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialAdHelper {
    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false

    // Call this function in onCreate() of Activity or onViewCreated() of Fragment
    fun loadInterstitialAd(context: Context, adUnitId: String) {
        if (isAdLoading || interstitialAd != null) return // Avoid multiple loads
        isAdLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                isAdLoading = false
                Log.d("InterstitialAdHelper", "Ad loaded successfully")
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                interstitialAd = null
                isAdLoading = false
                Log.e("InterstitialAdHelper", "Ad failed to load: ${adError.message}")
            }
        })
    }

    // Call this function when a button is clicked to show the ad
    fun showInterstitialAd(activity: Activity, adUnitId: String, onAdClosed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null // Reset ad so a new one can be loaded
                    loadInterstitialAd(activity, adUnitId) // Load new ad immediately
                    onAdClosed() // Perform the action after the ad is closed
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    loadInterstitialAd(activity, adUnitId) // Load new ad even if failed
                    onAdClosed() // Proceed without showing the ad
                }
            }
            interstitialAd?.show(activity)
        } else {
            Log.w("InterstitialAdHelper", "Ad not ready, loading new ad")
            loadInterstitialAd(activity, adUnitId) // Load ad if missing
            onAdClosed() // Proceed if no ad was available
        }
    }
}
