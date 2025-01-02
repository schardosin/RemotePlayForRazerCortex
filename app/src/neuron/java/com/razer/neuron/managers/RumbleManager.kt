package com.razer.neuron.managers

import android.content.Context
import com.razer.neuron.RnApp
import com.razer.neuron.extensions.getInputDeviceWithVibrator
import com.razer.neuron.extensions.isRazerDevice
import com.razer.neuron.extensions.usbManager
import com.razer.neuron.extensions.vibratorCount
import com.razer.neuron.managers.AIDLManager.NEXUS_AIDL_VERSION_CODE
import com.razer.neuron.nexus.NexusContentProvider
import timber.log.Timber

object RumbleManager {

    /**
     * Use AIDL rumble if:
     * 1. Razer controller with sensa haptics (and haptics)
     * 2. BUT not if it is in x-input mode and the OS has no problem getting vibrator count.
     */
    suspend fun Context.isRumbleWithNexus(isPrioritizeSensaHaptics : Boolean = true): Boolean {
        val razerUsbDevice = usbManager()?.deviceList?.values?.firstOrNull {
            it.isRazerDevice()
        } ?: return false // no haptics controller at all
        val isSensaSupported = NexusContentProvider.isControllerSensaSupported(RnApp.appContext) == true
        val isManualXInputVibrationSupported = NexusContentProvider.isControllerManualXInputVibrationSupported(RnApp.appContext) == true
        val hasAIDLInterface = hasAIDLInterface()
        val hasVibrators = (razerUsbDevice.getInputDeviceWithVibrator().vibratorCount() ?: 0) > 0
        val canRumbleViaNexus = isManualXInputVibrationSupported && isSensaSupported && hasAIDLInterface
        Timber.v("isRumbleWithNexus: isSensaSupported=$isSensaSupported isManualXInputVibrationSupported=$isManualXInputVibrationSupported hasAIDLInterface=$hasAIDLInterface, hasVibrators=$hasVibrators ${razerUsbDevice}")
        return if(!isPrioritizeSensaHaptics && hasVibrators) {
            false
        } else {
            canRumbleViaNexus
        }
    }

    private suspend fun hasAIDLInterface(): Boolean {
        val nexusVersionCode = NexusContentProvider.getAIDLVersionCode(RnApp.appContext) ?: 0
        return nexusVersionCode >= NEXUS_AIDL_VERSION_CODE
    }

}