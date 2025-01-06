package com.razer.neuron.extensions

import timber.log.Timber

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Convert a [X509Certificate.getEncoded] [ByteArray] back to [X509Certificate]
 */
@Throws(CertificateException::class)
fun ByteArray.toX509Cert(): X509Certificate {
    val certFactory = CertificateFactory.getInstance("X.509")
    return ByteArrayInputStream(this).use { stream ->
        certFactory.generateCertificate(stream) as X509Certificate
    }
}


/**
 * Convert this [ByteArray] to a standard size (probably smaller) size [ByteArray] as hash
 *
 */
fun ByteArray.MD5(): ByteArray? {
    return try {
        val md = MessageDigest.getInstance("MD5")
        md.update(this, 0, this.size)
        md.digest()
    } catch (t: Throwable) {
        Timber.w(t)
        null
    }
}
