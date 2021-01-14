package com.securefilemanager.app.helpers.crypto

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException

class PrefCrypto : Key() {

    companion object {
        const val KEY_ALIAS =
            "__app_pref_key_com.securefilemanager.app__"

        private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val PURPOSE = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        private const val APP_KEY_SIZE = MasterKey.DEFAULT_AES_GCM_MASTER_KEY_SIZE

        val PREF_KEY_ENCRYPTION_SCHEME =
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV
        val PREF_VALUE_ENCRYPTION_SCHEME =
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM

        @JvmField
        val key: KeyGenParameterSpec =
            KeyGenParameterSpec.Builder(KEY_ALIAS, PURPOSE).apply {
                setBlockModes(ENCRYPTION_BLOCK_MODE)
                setEncryptionPaddings(ENCRYPTION_PADDING)
                setKeySize(APP_KEY_SIZE)
            }.build()

        @JvmField
        val keyStrongBox: KeyGenParameterSpec =
            KeyGenParameterSpec.Builder(KEY_ALIAS, PURPOSE).apply {
                setBlockModes(ENCRYPTION_BLOCK_MODE)
                setEncryptionPaddings(ENCRYPTION_PADDING)
                setKeySize(APP_KEY_SIZE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    setIsStrongBoxBacked(true)
                }
            }.build()

        fun getKey(context: Context): MasterKey {
            try {
                return getKey(context, keyStrongBox, key, KEY_ALIAS)
            } catch (e: Exception) {
                when (e) {
                    is GeneralSecurityException, is IOException -> {
                        throw IllegalStateException("Can't generate or create key")
                    }
                    else -> throw e
                }
            }
        }


    }
}
