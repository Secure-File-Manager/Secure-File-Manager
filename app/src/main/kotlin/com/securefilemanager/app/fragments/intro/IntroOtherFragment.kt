package com.securefilemanager.app.fragments.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.securefilemanager.app.R
import com.securefilemanager.app.fragments.settings.SettingsBlockScreenshotsFragment
import com.securefilemanager.app.fragments.settings.SettingsMediaThumbnailFragment
import com.securefilemanager.app.helpers.htmlText
import kotlinx.android.synthetic.main.fragment_intro_other.*

class IntroOtherFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_intro_other, container, false)

        this.childFragmentManager
            .beginTransaction()
            .replace(R.id.settings_disable_screenshots, SettingsBlockScreenshotsFragment())
            .commit()

        this.childFragmentManager
            .beginTransaction()
            .replace(R.id.settings_show_media_thumbnail, SettingsMediaThumbnailFragment())
            .commit()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        feature_encrypted_zip_text?.text =
            htmlText(this.getString(R.string.intro_other_feature_encrypted_zip))
        feature_media_saving?.text = htmlText(this.getString(R.string.feature_media_saving))
    }

    companion object {
        fun newInstance(): IntroOtherFragment {
            return IntroOtherFragment()
        }
    }
}
