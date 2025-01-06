package com.razer.neuron.extensions

import android.content.Intent
import android.os.Parcelable
import com.razer.neuron.utils.API_LEVEL33
import com.razer.neuron.utils.isAboveOrEqual

inline fun <reified T : Parcelable> Intent.getParcelableExtraExt(key: String): T? = when {
    isAboveOrEqual(API_LEVEL33) -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}
