package com.sparsh.statussaver_videodownload.UI.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.sparsh.statussaver_videodownload.databinding.FragmentStatusBinding

private lateinit var binding: FragmentStatusBinding

@Suppress("DEPRECATION")
class StatusFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
       binding = FragmentStatusBinding.inflate(inflater,container,false)

        val viewPager = binding.viewPager
        val tabs = binding.tabs


        val adapter = MyViewPagerAdapter(childFragmentManager)
        adapter.addFragment(ImageSFragment(),"Image")
        adapter.addFragment(VideoSFragment(),"Video")
        viewPager.adapter = adapter
        tabs.setupWithViewPager(viewPager)


        return binding.root
    }


    class MyViewPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager) {

        private val fragmentList: MutableList<Fragment> = ArrayList()
        private val titleList: MutableList<String> = ArrayList()

        override fun getCount(): Int {
            return fragmentList.size
        }

        override fun getItem(position: Int): Fragment {
            return fragmentList[position]
        }

        fun addFragment(fragment: Fragment,title: String) {
            fragmentList.add(fragment)
            titleList.add(title)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return titleList[position]
        }
    }
}