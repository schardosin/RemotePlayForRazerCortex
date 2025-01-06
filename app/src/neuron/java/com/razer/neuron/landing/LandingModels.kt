package com.razer.neuron.landing

import android.content.Intent
import com.limelight.nvstream.http.ComputerDetails

sealed class LandingState {

    /**
     * Tell the UI to stop polling using [com.limelight.computers.ComputerManagerService]
     */
    data class StopComputerUpdates(val wait : Boolean) : LandingState()

    /**
     * Tell the UI to prepare polling using [com.limelight.computers.ComputerManagerService]
     */
    data object StartComputerUpdates : LandingState()

    /**
     * Tell the UI to actually start polling using [com.limelight.computers.ComputerManagerService]
     */
    data object StartComputerPolling : LandingState()

    data class ShowLoading(val tag: String) : LandingState()
    data class HideLoading(val tag: String = "") : LandingState()
    data class ShowError(val error: Throwable) : LandingState()
    data class ShowMessage(val message: String) : LandingState()
    data class ShowPin(val computerDetails: ComputerDetails, val pinCode: String) : LandingState()
    data object HidePin : LandingState()
    data class ShowContent(val items: List<LandingItem>) : LandingState()
    data object StartManualPairing : LandingState()
    data class StartStreaming(val intent: Intent) : LandingState()

    /**
     * Invalidate a [ComputerDetails] in [com.limelight.computers.ComputerManagerService]
     */
    data class InvalidateComputer(val computerDetails: ComputerDetails) : LandingState()
    data class Action(val action: LandingAction) : LandingState()

}

sealed class LandingAction {
    data object Settings: LandingAction()
    data object ManualPairing: LandingAction()
    data class StartStreaming(val computerDetails: ComputerDetails) : LandingAction()
    data class Pair(val computerDetails: ComputerDetails) : LandingAction()
    data class Unpair(val computerDetails: ComputerDetails) : LandingAction()
    data class Retry(val computerDetails: ComputerDetails): LandingAction()
}
