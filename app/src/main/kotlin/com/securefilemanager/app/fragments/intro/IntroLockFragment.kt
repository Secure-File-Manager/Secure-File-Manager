package com.securefilemanager.app.fragments.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.securefilemanager.app.R
import com.securefilemanager.app.fragments.settings.SettingsLockFragment


class IntroLockFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_intro_lock, container, false)

        this.childFragmentManager
            .beginTransaction()
            .replace(R.id.settings_lock, SettingsLockFragment()).commit()

        return view
    }

    companion object {
        fun newInstance(): IntroLockFragment {
            return IntroLockFragment()
        }
    }
}
