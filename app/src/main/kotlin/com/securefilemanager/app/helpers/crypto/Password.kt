package com.securefilemanager.app.helpers.crypto

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import com.lambdapioneer.argon2kt.Argon2Version
import com.securefilemanager.app.extensions.convertKBtoKiB
import java.io.RandomAccessFile
import java.security.SecureRandom
import java.util.regex.Matcher
import java.util.regex.Pattern

class Password {
    private var argon2Kt: Argon2Kt = Argon2Kt()

    private fun getSecureRandomSalt(): ByteArray {
        val secureRandom = SecureRandom()
        val random = ByteArray(saltSize)
        secureRandom.nextBytes(random)
        return random
    }

    private fun getMemoryCost(): Int {
        val randomAccessFile = RandomAccessFile("/proc/meminfo", "r")
        val readLine: String = randomAccessFile.readLine()
        randomAccessFile.close()
        val p: Pattern = Pattern.compile("(\\d+)")
        val m: Matcher = p.matcher(readLine)
        if (m.find()) {
            val group: String? = m.group(1)
            if (group != null) {
                val memoryContKB: Int = (group.toInt() * 0.05).toInt() // 5 % of memory in device
                return memoryContKB.convertKBtoKiB()
            }
        }
        return defaultMemoryCost
    }

    private fun getParallelism(): Int = Runtime.getRuntime().availableProcessors()

    fun hash(password: ByteArray): String =
        this.argon2Kt.hash(
            mode = mode,
            password = password,
            salt = this.getSecureRandomSalt(),
            tCostInIterations = tCostInIterations,
            mCostInKibibyte = this.getMemoryCost(),
            parallelism = this.getParallelism(),
            hashLengthInBytes = hashLengthInBytes,
            version = version
        ).encodedOutputAsString()

    fun verify(hash: String, password: ByteArray): Boolean =
        this.argon2Kt.verify(
            mode = mode,
            encoded = hash,
            password = password
        )

    companion object {
        private const val tCostInIterations: Int = 1
        private const val hashLengthInBytes: Int = 64 //  258 bit
        private const val defaultMemoryCost: Int = 65536
        private const val saltSize: Int = 64

        private val version: Argon2Version = Argon2Version.V13
        private val mode: Argon2Mode = Argon2Mode.ARGON2_ID
    }
}
