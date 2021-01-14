package com.securefilemanager.app.helpers.crypto

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException

abstract class Key {
    companion object {

        @Throws(GeneralSecurityException::class, IOException::class)
        fun getKey(
            context: Context,
            keyStrongBox: KeyGenParameterSpec,
            keyNotStrongBox: KeyGenParameterSpec,
            keyAlias: String
        ): MasterKey {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return try {
                    MasterKey.Builder(context, keyAlias).apply {
                        setKeyGenParameterSpec(keyStrongBox)
                    }.build()
                } catch (exception: GeneralSecurityException) {
                    return getKeyNotStrongBox(context, keyNotStrongBox, keyAlias)
                }
            } else {
                return getKeyNotStrongBox(context, keyNotStrongBox, keyAlias)
            }
        }

        @Throws(GeneralSecurityException::class, IOException::class)
        private fun getKeyNotStrongBox(
            context: Context,
            keyNotStrongBox: KeyGenParameterSpec,
            keyAlias: String
        ): MasterKey {
            return MasterKey.Builder(context, keyAlias).apply {
                setKeyGenParameterSpec(keyNotStrongBox)
            }.build()
        }
    }
}
