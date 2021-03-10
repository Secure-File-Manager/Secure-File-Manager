package com.securefilemanager.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.securefilemanager.app.R
import com.securefilemanager.app.extensions.getVersion
import com.securefilemanager.app.extensions.openUri
import kotlinx.android.synthetic.main.fragment_about.*

class AboutFragment : Fragment() {

    companion object {
        private const val GITHUB_URI: String =
            "https://github.com/Secure-File-Manager/Secure-File-Manager"
        private const val CHANGELOG_URI: String = "$GITHUB_URI/blob/master/CHANGELOG.md"
        private const val PRIVACY_POLICY_URI: String = "$GITHUB_URI/blob/master/PRIVACY_POLICY.md"
        private const val LICENSE_URI: String = "$GITHUB_URI/blob/master/LICENSE"
        private const val FAQ_URI: String = "$GITHUB_URI/wiki/Frequently-Asked-Questions"
        private const val CONTRIBUTING_URI: String = "$GITHUB_URI/blob/master/CONTRIBUTING.md"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_about, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        about_text_version.text = requireActivity().getVersion()

        about_layout_license.setOnClickListener {
            requireActivity().openUri(LICENSE_URI)
        }

        about_layout_privacy_policy.setOnClickListener {
            requireActivity().openUri(PRIVACY_POLICY_URI)
        }

        about_layout_changelog.setOnClickListener {
            requireActivity().openUri(CHANGELOG_URI)
        }

        about_layout_source.setOnClickListener {
            requireActivity().openUri(GITHUB_URI)
        }

        about_layout_faq.setOnClickListener {
            requireActivity().openUri(FAQ_URI)
        }

        about_layout_contribute.setOnClickListener {
            requireActivity().openUri(CONTRIBUTING_URI)
        }

    }

}
