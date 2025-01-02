package com.razer.neuron.nexus.sources

import android.content.Context
import com.limelight.binding.crypto.AndroidCryptoProvider
import com.razer.neuron.extensions.convertToHex
import com.razer.neuron.extensions.getStringByColumnName
import com.razer.neuron.extensions.hexToByteArray
import com.razer.neuron.shared.SharedConstants
import com.razer.neuron.nexus.NexusContentSource
import java.io.File

class NexusAndroidCryptoSource : NexusContentSource {
    override val path get() = SharedConstants.ANDROID_CRYPTO_PROVIDER
    private val uriPath get() = SharedConstants.baseNexusUri.buildUpon().appendPath(path).build()

    private fun getByteArray(context: Context, key: String): ByteArray? {
        return context.contentResolver.query(
            uriPath,
            null,
            "${SharedConstants.KRATE_ITEM_NAME} = ?",
            arrayOf(key),
            null,
        ).use { c ->
            val value =
                if (c?.moveToFirst() == true) c.getStringByColumnName(SharedConstants.KRATE_ITEM_VALUE) else null
            value?.hexToByteArray()
        }
    }

    /**
     * See [com.limelight.binding.crypto.AndroidCryptoProvider]
     */
    override suspend fun sync(context: Context) {
        return checkOrThrowPermission(context) {
            synchronized(AndroidCryptoProvider.globalCryptoLock) {
                val dataPath = context.filesDir.absolutePath
                val clientCrt = getByteArray(context, SharedConstants.KEY_CLIENT_CRT)
                if (clientCrt != null) {
                    with(File(dataPath + File.separator + "client.crt")) {
                        writeBytes(clientCrt)
                        logger("clientCrt found. ${clientCrt.size} bytes written to ${this.path}")
                        logger(String(clientCrt))
                    }
                } else {
                    logger("clientCrt not found")
                }
                val clientKey = getByteArray(context, SharedConstants.KEY_CLIENT_KEY)
                if (clientKey != null) {
                    with(File(dataPath + File.separator + "client.key")) {
                        writeBytes(clientKey)
                        logger("clientKey found. ${clientKey.size} bytes written to ${this.path}")
                        logger(clientKey.convertToHex(lowercase = false))
                    }
                } else {
                    logger("clientKey not found")
                }
            }
        }
    }


}