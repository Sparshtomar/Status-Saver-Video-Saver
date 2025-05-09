package com.sparsh.statussaver_videodownload.UI.Adapters

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.sparsh.statussaver_videodownload.R
import com.sparsh.statussaver_videodownload.UI.Activities.ImagePreviewActivity
import com.sparsh.statussaver_videodownload.UI.Utils.ImageUtils

class BstatusImageAdapter(private var images: List<String>, private val activity: AppCompatActivity) :
    RecyclerView.Adapter<BstatusImageAdapter.BStatusViewHolder>() {

    private var mInterstitialAd: InterstitialAd? = null
    private val adUnitId = "ca-app-pub-7713317467402311/5772127469" // Test Ad ID

    init {
        loadInterstitialAd(activity) // Load the ad initially
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BStatusViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_b, parent, false)
        return BStatusViewHolder(view)
    }

    override fun onBindViewHolder(holder: BStatusViewHolder, position: Int) {
        val imagePath = images[position]
        Log.d("StatusImageAdapter", "Loading image: $imagePath")

        Glide.with(holder.itemView.context)
            .load(imagePath)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.StatusImageView)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = ImagePreviewActivity.newIntent(context, images, position, ImagePreviewActivity.FRAGMENT_BUSINESS)
            context.startActivity(intent)
        }
        holder.downloadButton.setOnClickListener {
            Log.d("InterstitialAd", "Download button clicked")
            showInterstitialAd(activity) {
                Log.d("InterstitialAd", "Starting download after ad")
                val imageUri = Uri.parse(imagePath)
                ImageUtils.checkStoragePermission(activity, imageUri)
            }
        }

    }

    override fun getItemCount(): Int {
        return images.size
    }

    @Suppress("DEPRECATION")
    inner class BStatusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val StatusImageView: ImageView = itemView.findViewById(R.id.imageView)
        val downloadButton: ImageView = itemView.findViewById(R.id.downloadext)
    }

        /** Load the interstitial ad */
        private fun loadInterstitialAd(context: Context) {
            Log.d("InterstitialAd", "Loading new interstitial ad...")
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d("InterstitialAd", "Ad Loaded Successfully")
                    mInterstitialAd = interstitialAd
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("InterstitialAd", "Failed to load ad: ${adError.message}")
                    mInterstitialAd = null
                }
            })
        }

        /** Show the interstitial ad */
        private fun showInterstitialAd(context: Context, onAdClosed: () -> Unit) {
            if (mInterstitialAd != null) {
                Log.d("InterstitialAd", "Showing Interstitial Ad")
                mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d("InterstitialAd", "Ad Dismissed. Reloading Ad...")
                        mInterstitialAd = null
                        loadInterstitialAd(context) // Reload after showing
                        onAdClosed() // Start download after the ad is closed
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                        Log.e("InterstitialAd", "Failed to show ad: ${adError.message}")
                        mInterstitialAd = null
                        onAdClosed() // If ad fails, start download immediately
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d("InterstitialAd", "Ad is now visible")
                        mInterstitialAd = null
                    }
                }
                mInterstitialAd?.show(activity)
            } else {
                Log.d("InterstitialAd", "Ad not ready, starting download immediately")
                onAdClosed() // If no ad is loaded, start download immediately
            }
        }
    }