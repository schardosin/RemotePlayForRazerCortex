package com.razer.neuron

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import com.razer.neuron.managers.AIDLManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber


class NeuronReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Timber.v("onReceive: $intent")
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                AIDLManager.unbindService(context)
            }
        }
    }


}