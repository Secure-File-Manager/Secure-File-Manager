package com.securefilemanager.app.fragments.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.appintro.SlidePolicy
import com.securefilemanager.app.R
import com.securefilemanager.app.extensions.beGoneIf
import com.securefilemanager.app.extensions.hasPermission
import com.securefilemanager.app.extensions.openSystemSettings
import com.securefilemanager.app.extensions.toastLong
import com.securefilemanager.app.helpers.GENERIC_PERM_HANDLER
import com.securefilemanager.app.helpers.PERMISSION_WRITE_STORAGE
import com.securefilemanager.app.helpers.getPermissionString
import kotlinx.android.synthetic.main.fragment_intro_permission.*

class IntroPermissionFragment : Fragment(), SlidePolicy {

    private var actionOnPermission: ((granted: Boolean) -> Unit)? = { granted: Boolean ->
        this.setUiVisibility(granted)
        if (!granted) {
            this.showToast()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_intro_permission, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        this.handlePermission()
        enable_permission_button.setOnClickListener {
            handlePermission()
        }
        system_settings_button.setOnClickListener {
            this.requireActivity().openSystemSettings()
        }
    }

    override fun onResume() {
        super.onResume()
        if (this.isPermissionGranted()) {
            this.actionOnPermission?.invoke(true)
        }
    }


    override val isPolicyRespected: Boolean get() = this.isPermissionGranted()

    override fun onUserIllegallyRequestedNextPage() {
        this.showToast()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == GENERIC_PERM_HANDLER && grantResults.isNotEmpty()) {
            this.actionOnPermission?.invoke(grantResults[0] == 0)
        }
    }

    private fun handlePermission() {
        if (this.isPermissionGranted()) {
            this.actionOnPermission?.invoke(true)
        } else {
            this.requestPermissions(
                arrayOf(getPermissionString(PERMISSION_ID)),
                GENERIC_PERM_HANDLER
            )
        }
    }

    private fun isPermissionGranted() = this.requireContext().hasPermission(PERMISSION_ID)

    private fun showToast() {
        this.requireActivity().toastLong(
            R.string.intro_permission_illegally_request_next_page
        )
    }

    private fun setUiVisibility(permissionGranted: Boolean) {
        enable_permission_button.beGoneIf(permissionGranted)
        system_settings_button.beGoneIf(permissionGranted)
        summary2_text.beGoneIf(permissionGranted)
        permission_enabled_text.beGoneIf(!permissionGranted)
    }

    companion object {
        private const val PERMISSION_ID = PERMISSION_WRITE_STORAGE

        fun newInstance(): IntroPermissionFragment {
            return IntroPermissionFragment()
        }
    }

}
