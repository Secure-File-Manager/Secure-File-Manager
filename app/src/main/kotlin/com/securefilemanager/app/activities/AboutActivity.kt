package com.securefilemanager.app.activities

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.mikepenz.aboutlibraries.LibsBuilder
import com.securefilemanager.app.R
import com.securefilemanager.app.fragments.AboutFragment
import kotlinx.android.synthetic.main.activity_about.*

// This activity is inspired by the andOTP - https://github.com/andOTP/andOTP
class AboutActivity : BaseAbstractActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        pager.adapter = AboutPageAdapter(this)
        TabLayoutMediator(tab_layout, pager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.about_tab_about)
                1 -> getString(R.string.about_tab_libraries)
                else -> null
            }
        }.attach()
    }

    private class AboutPageAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> AboutFragment()
                1 -> LibsBuilder()
                    .withFields(R.string::class.java.fields)
                    .withLicenseShown(true)
                    .withVersionShown(true)
                    .withAboutIconShown(false)
                    .withAboutVersionShown(false)
                    .supportFragment()
                else -> Fragment() // this should not happened
            }
        }
    }
}
