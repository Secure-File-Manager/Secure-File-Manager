package com.securefilemanager.app.interfaces

import com.securefilemanager.app.helpers.EncryptionAction
import com.securefilemanager.app.helpers.HideAction

interface CopyMoveListener {
    fun copySucceeded(
        copyOnly: Boolean,
        copiedAll: Boolean,
        destinationPath: String,
        encryptionAction: EncryptionAction = EncryptionAction.NONE,
        hideAction: HideAction
    )

    fun copyFailed(encryptionAction: EncryptionAction = EncryptionAction.NONE)
}
