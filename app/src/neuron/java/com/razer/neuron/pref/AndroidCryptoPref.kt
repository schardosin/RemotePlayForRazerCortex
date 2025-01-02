package com.razer.neuron.pref

import android.content.Context
import android.content.SharedPreferences
import com.razer.neuron.RnApp
import com.razer.neuron.shared.SharedConstants
import hu.autsoft.krate.Krate

object AndroidCryptoPref : Krate {
    const val NEURON_SETTINGS_NAME = SharedConstants.ANDROID_CRYPTO_PROVIDER

    override val sharedPreferences: SharedPreferences by lazy {
        RnApp.appContext.getSharedPreferences(NEURON_SETTINGS_NAME, Context.MODE_PRIVATE)
    }
}
