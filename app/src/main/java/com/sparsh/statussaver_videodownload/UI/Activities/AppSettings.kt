package com.sparsh.statussaver_videodownload.UI.Activities

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.sparsh.statussaver_videodownload.R
import com.sparsh.statussaver_videodownload.UI.ADS.AdHelper
import com.sparsh.statussaver_videodownload.databinding.ActivityAppSettingsBinding

@Suppress("DEPRECATION")
class AppSettings : AppCompatActivity() {
    private lateinit var binding: ActivityAppSettingsBinding
    private val adUnitId = "ca-app-pub-7713317467402311/6200606525"
    val playStoreUrl = "https://play.google.com/store/apps/details?id=com.sparsh.statussaver_videodownload"
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityAppSettingsBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        AdHelper.loadBannerAd(this, binding.adViewContainer, adUnitId)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT


        binding.settingsbackbtn.setOnClickListener {
            super.onBackPressed()
        }
        binding.PrivacyPolicyCard.setOnClickListener {
            showPrivacyPolicyDialog(this)
        }
        binding.CopyrightCard.setOnClickListener {
            shareApp()
        }
        binding.termscard.setOnClickListener {
            showtermsDialog(this)
        }
        binding.ContactCard.setOnClickListener {
            sendEmail()
        }
        binding.RateAppCard.setOnClickListener {

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl))
            startActivity(intent)
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

    private fun showtermsDialog(context: Context) {
        val privacyPolicyText = context.getString(R.string.terms)

        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder
            .setTitle(R.string.Terms_title)
            .setMessage(privacyPolicyText)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }

        // Create the AlertDialog
        val alertDialog = dialogBuilder.create()

        // Show the AlertDialog
        alertDialog.show()

        // Set text color for positive button
        if(isDarkTheme()){
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(context, R.color.ok_dark))
        } else {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(context, R.color.ok_light))
        }

        // Set text size for message TextView
        val textView = alertDialog.findViewById<TextView>(android.R.id.message)
        textView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)

    }


    fun showPrivacyPolicyDialog(context: Context) {
        val privacyPolicyText = context.getString(R.string.privacy_policy)

        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder
            .setTitle(R.string.privacy_policy_title)
            .setMessage(privacyPolicyText)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }

        // Create the AlertDialog
        val alertDialog = dialogBuilder.create()

        // Show the AlertDialog
        alertDialog.show()

        // Set text color for positive button
        if(isDarkTheme()){
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(context, R.color.ok_dark))
        } else {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(context, R.color.ok_light))
        }

        // Set text size for message TextView
        val textView = alertDialog.findViewById<TextView>(android.R.id.message)
        textView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
    }

    private fun sendEmail() {
        val email = "sparshtomar.res@gmail.com"
        val subject = "Regarding Status Saver - Video Download"
        val body = ""

        val uri = Uri.parse("mailto:").buildUpon()
            .appendQueryParameter("to", email)
            .appendQueryParameter("subject", subject)
            .appendQueryParameter("body", body)
            .build()

        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            setPackage("com.google.android.gm")
        }

        try {
            startActivity(intent)
        } catch (ex: ActivityNotFoundException) {
            showAlternativeContactMethod()
        }
    }





    private fun showAlternativeContactMethod() {
        AlertDialog.Builder(this)
            .setTitle("Contact Support")
            .setMessage("No email app is available. You can contact us via email at sparshtomar.res@gmail.com. Would you like to copy the email address?")
            .setPositiveButton("Copy Email") { dialog, _ ->
                // Copy the email address to clipboard
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Email", "sparshtomar.res@gmail.com")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Email copied to clipboard", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }



    private fun isDarkTheme(): Boolean {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }



}