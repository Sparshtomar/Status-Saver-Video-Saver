package com.sparsh.statussaver_videodownload.UI.Fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.sparsh.statussaver_videodownload.UI.ADS.AdHelper
import com.sparsh.statussaver_videodownload.UI.ADS.InterstitialAdHelper
import com.sparsh.statussaver_videodownload.databinding.FragmentChatBinding


class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding
    private val adUnitIdInterstitial = "ca-app-pub-7713317467402311/5772127469"
    private val adUnitIdBanner = "ca-app-pub-7713317467402311/6200606525"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
       binding = FragmentChatBinding.inflate(inflater,container,false)



            binding.button.setOnClickListener {
                InterstitialAdHelper.showInterstitialAd(requireActivity(),adUnitIdInterstitial) {
                    sendMessage()
                    InterstitialAdHelper.loadInterstitialAd(requireContext(),adUnitIdInterstitial) // Load new ad after task
                }
            }

            binding.buttonB.setOnClickListener{
                InterstitialAdHelper.showInterstitialAd(requireActivity(),adUnitIdInterstitial) {
                   sendMessageB()
                    InterstitialAdHelper.loadInterstitialAd(requireContext(),adUnitIdInterstitial) // Load new ad after task
                }
            }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AdHelper.loadBannerAd(requireActivity(), binding.adViewContainer, adUnitIdBanner)
    }

    private fun sendMessage() {
        val countryCode: String = binding.countryCode.text.toString().trim()
        val phoneNumber: String = binding.phoneNumber.text.toString().trim()
        val message: String = binding.message.text.toString().trim()

        if (countryCode.isEmpty() || phoneNumber.isEmpty() || message.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val phoneNumberWithCountryCode = "+" + countryCode + phoneNumber
        val encodedMessage = Uri.encode(message)

        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumberWithCountryCode&text=$encodedMessage")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp") // Ensure the intent is handled by WhatsApp
        }

        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
        }
    }


    private fun sendMessageB() {
        val countryCode: String = binding.countryCode.text.toString().trim()
        val phoneNumber: String = binding.phoneNumber.text.toString().trim()
        val message: String = binding.message.text.toString().trim()

        if (countryCode.isEmpty() || phoneNumber.isEmpty() || message.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val phoneNumberWithCountryCode = "+" + countryCode + phoneNumber
        val encodedMessage = Uri.encode(message)

        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumberWithCountryCode&text=$encodedMessage")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp.w4b") // Ensure the intent is handled by WhatsApp Business
        }

        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "WhatsApp Business is not installed", Toast.LENGTH_SHORT).show()
        }
    }



}