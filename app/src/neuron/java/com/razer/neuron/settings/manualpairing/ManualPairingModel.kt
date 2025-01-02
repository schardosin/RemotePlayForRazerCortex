package com.razer.neuron.settings.manualpairing

sealed class ManualPairingState {

    class ShowLoading(val tag: String) : ManualPairingState()

    class HideLoading(val tag: String) : ManualPairingState()

    class ShowPin(val computerName: String, val pinCode: String) : ManualPairingState() {
        override fun toString() = "computerName=${computerName}, pinCode=${pinCode}"
    }

    data object HidePin : ManualPairingState()

    data object DismissPinDialog : ManualPairingState()

    data object OnPaired : ManualPairingState()

    class Error(val exception: Throwable) : ManualPairingState() {
        override fun toString() = "exception=${exception.message}"
    }

}
