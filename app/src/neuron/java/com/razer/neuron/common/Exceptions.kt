package com.razer.neuron.common

import timber.log.Timber

fun logAndRecordException(t: Throwable, vararg keyValuePairs: Pair<String, Any?>) {
    debugToast(t.message)
    Timber.e(t)
}


open class PairingException(val msg: String, cause: Throwable? = null) : Exception(msg, cause)