package com.razer.neuron.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.Locale

private const val SAMSUNG = "samsung"

val isRazerEdgeDevice by lazy { Build.MODEL.contains("Razer Edge") }
val isSamsungDevice by lazy { SAMSUNG.equals(Build.BRAND, true) || Build.MANUFACTURER.contains(SAMSUNG, true) }
val isGooglePixel by lazy {
    Build.BRAND.lowercase().contains("google") ||
            Build.MODEL.lowercase().contains("pixel")
}


/**
 * [Build.BRAND] can return lower case. (e.g. "google" instead of "Google"), so we artifically
 * upper case the first char.
 *
 * @return device name e.g. "Google Pixel 7 pro"
 */
fun defaultDeviceName() = "${Build.BRAND.replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(
        Locale.ENGLISH
    ) else it.toString()
}} + ${Build.MODEL}"
/**
 * @return the user given name of the device (if possible) else return [default] which is
 * [defaultDeviceName]
 */
fun Context.getDeviceNickName(default: String = defaultDeviceName()): String {
    val functions: List<() -> Result<String>> = listOf(
        { runCatching { Settings.System.getString(contentResolver, "bluetooth_name") } },
        { runCatching { Settings.Secure.getString(contentResolver, "bluetooth_name") } },
        { runCatching { Settings.System.getString(contentResolver, "device_name") } },
        { runCatching { Settings.Secure.getString(contentResolver, "lock_screen_owner_info") } }
    )
    functions.forEachIndexed { _, function ->
        val result = function()
        if (result.isSuccess) {
            result.getOrNull()?.takeIf { it.isNotBlank() }?.let {
                return it
            }
        }
    }
    return default
}