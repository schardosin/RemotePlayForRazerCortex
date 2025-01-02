package com.razer.neuron.extensions

import android.hardware.usb.UsbDevice
import android.os.Build
import android.view.InputDevice

fun UsbDevice.getInputDevices() = inputDevices { it.matches(this) }

fun UsbDevice.getInputDeviceWithVibrator() = getInputDevices().maxBy { it.vibratorCount() ?: 0 }

fun InputDevice.matches(usbDevice: UsbDevice): Boolean {
    return (usbDevice.vendorId == this.vendorId &&
            usbDevice.productId == this.productId)
}

fun InputDevice.vibratorCount() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) vibratorManager.vibratorIds.size else null

fun inputDevices(filter: (InputDevice) -> Boolean = { true }) =
    InputDevice.getDeviceIds().toList().mapNotNull { InputDevice.getDevice(it) }.filter(filter)

