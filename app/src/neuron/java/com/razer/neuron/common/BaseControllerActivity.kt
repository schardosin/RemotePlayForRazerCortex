package com.razer.neuron.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager

open class BaseControllerActivity : BaseActivity() {

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BOOT_COMPLETED -> onActionBootCompleted()
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> onUsbDeviceAttached()
                UsbManager.ACTION_USB_DEVICE_DETACHED -> onUsbDeviceDetached()
                else -> Unit
            }
        }
    }

    open fun onActionBootCompleted() = Unit
    open fun onUsbDeviceAttached() = Unit
    open fun onUsbDeviceDetached() = Unit


    override fun onResume() {
        super.onResume()
        registerUsbBroadcaster()
    }

    override fun onPause() {
        super.onPause()
        unregisterUsbBroadcaster()
    }

    private fun registerUsbBroadcaster() {
        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction(Intent.ACTION_BOOT_COMPLETED)
        registerReceiver(usbReceiver, filter)
    }

    private fun unregisterUsbBroadcaster() = runCatching {
        unregisterReceiver(usbReceiver)
    }

}