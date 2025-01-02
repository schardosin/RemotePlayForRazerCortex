package com.razer.neuron.model

import android.view.KeyEvent
import com.limelight.R

sealed class ButtonHint(val controllerInput: ControllerInput, val appAction: AppAction) {
    data object Back : ButtonHint(ControllerInput.B, AppAction.Back)
    data object Accept : ButtonHint(ControllerInput.A, AppAction.Accept)
    data object Allow : ButtonHint(ControllerInput.A, AppAction.Allow)
    data object Download : ButtonHint(ControllerInput.A, AppAction.Download)
    data object Done : ButtonHint(ControllerInput.A, AppAction.Done)
    data object Continue : ButtonHint(ControllerInput.A, AppAction.Continue)
    data object Pair : ButtonHint(ControllerInput.A, AppAction.Pair)
    data object StartPlay : ButtonHint(ControllerInput.A, AppAction.StartPlay)
    data object Settings : ButtonHint(ControllerInput.A, AppAction.Settings)
    data object ManuallyPair : ButtonHint(ControllerInput.A, AppAction.ManuallyPair)
    data object Retry : ButtonHint(ControllerInput.A, AppAction.Retry)
    data object Skip : ButtonHint(ControllerInput.Y, AppAction.Skip)
    data object Unpair : ButtonHint(ControllerInput.Y, AppAction.Unpair)
}

enum class ControllerInput(val keyCode : Int) {
    A(KeyEvent.KEYCODE_BUTTON_A),
    B(KeyEvent.KEYCODE_BUTTON_B),
    X(KeyEvent.KEYCODE_BUTTON_X),
    Y(KeyEvent.KEYCODE_BUTTON_Y),
    START(KeyEvent.KEYCODE_BUTTON_START)
}


fun ControllerInput.toIconRes() = when (this) {
    ControllerInput.A -> R.drawable.ic_key_a_primary_container
    ControllerInput.B -> R.drawable.ic_key_b_primary_container
    ControllerInput.Y -> R.drawable.ic_key_y_primary_container
    ControllerInput.X -> R.drawable.ic_key_x_primary_container
    else -> null
}