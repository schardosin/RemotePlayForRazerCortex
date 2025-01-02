package com.razer.neuron.extensions

import android.hardware.usb.UsbDevice

fun UsbDevice.isRazerDevice() = vendorId == 5426

/**
 * @return true if controller is in x-input mode, but x-input does not 100% implies
 * that controller has motors (e.g. Bianca T2 with latest FW update)
 */
fun UsbDevice.isXInput() = isRazerDevice() && productId == 55



const val RAZER_KISHI_V2_PRO = "Razer Kishi V2 Pro"
const val RAZER_KISHI_ULTRA = "Razer Kishi Ultra"