package com.razer.neuron.provider.sources

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.limelight.binding.crypto.AndroidCryptoProvider
import com.razer.neuron.RnApp
import com.razer.neuron.extensions.convertToHex
import com.razer.neuron.provider.NeuronContentSource
import com.razer.neuron.provider.ObjectContentSource
import com.razer.neuron.provider.NeuronProviderModelHelper
import com.razer.neuron.shared.SharedConstants
import java.lang.UnsupportedOperationException

/**
 * [NeuronContentSource] for [AndroidCryptoProviderPref]
 */
class NeuronAndroidCryptoSource :
    ObjectContentSource<AndroidCrypto, NeuronAndroidCryptoSource.Companion> {

    override val path: String
        get() = SharedConstants.ANDROID_CRYPTO_PROVIDER

    private val androidCryptoProvider by lazy {
        AndroidCryptoProvider(RnApp.appContext)
    }


    private val clientCertHex get() = androidCryptoProvider.pemEncodedClientCertificate.convertToHex()
    private val clientKeyHex get() = androidCryptoProvider.clientPrivateKey.encoded.convertToHex()


    override fun query(
        context: Context, uri: Uri, matchKeyValues: Array<Pair<String, Any?>>?
    ): List<AndroidCrypto> {
        val nameValue =
            matchKeyValues?.firstOrNull { it.first == SharedConstants.NAME }?.second as? String
        val result = mutableListOf<AndroidCrypto>()
        if (nameValue == null || nameValue == SharedConstants.CLIENT_CRT) {
            result += AndroidCrypto(SharedConstants.CLIENT_CRT, clientCertHex)
        }
        if (nameValue == null || nameValue == SharedConstants.CLIENT_KEY) {
            result += AndroidCrypto(SharedConstants.CLIENT_KEY, clientKeyHex)
        }
        return result
    }


    override val modelHelper = Companion

    override fun delete(
        context: Context, uri: Uri, matchKeyValues: Array<Pair<String, Any?>>?
    ) = throw UnsupportedOperationException("delete not supported")

    override fun insertOrReplace(
        context: Context, uri: Uri, item: AndroidCrypto, matchKeyValues: Array<Pair<String, Any?>>?
    ) = throw UnsupportedOperationException("insertOrReplace not supported")

    companion object : NeuronProviderModelHelper<AndroidCrypto> {
        override val allFields = listOf(
            SharedConstants.NAME to String::class.java, SharedConstants.VALUE to String::class.java
        ).toMap()

        override fun create(values: ContentValues): AndroidCrypto {
            val name = values.getAsString(SharedConstants.NAME)
                ?: throw IllegalArgumentException("${SharedConstants.NAME} not specified")
            val value = values.getAsString(SharedConstants.VALUE)
            return AndroidCrypto(name, value)
        }

        override fun toKeyValuePairs(obj: AndroidCrypto): Array<Pair<String, Any?>> {
            return arrayOf(
                SharedConstants.NAME to obj.name, SharedConstants.VALUE to obj.valueHex
            )
        }
    }
}

class AndroidCrypto(val name: String, val valueHex: String?)
