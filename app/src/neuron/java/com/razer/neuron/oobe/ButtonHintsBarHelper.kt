package com.razer.neuron.oobe

import android.view.View
import com.razer.neuron.common.ButtonHintsBar
import com.razer.neuron.extensions.getDrawableExt
import com.razer.neuron.extensions.gone
import com.razer.neuron.extensions.visible
import com.razer.neuron.model.ButtonHint
import com.razer.neuron.model.ControllerInput
import com.razer.neuron.model.toButtonText
import com.razer.neuron.model.toIconRes


fun ButtonHintsBar.getButtonHintView(controllerInput: ControllerInput) = when (controllerInput) {
    ControllerInput.A -> this.buttonA
    ControllerInput.B -> this.buttonB
    ControllerInput.Y -> this.buttonY
    ControllerInput.X -> this.buttonX
    else -> null
}

fun ButtonHintsBar.viewToControllerInput(button: View) = when (button) {
    this.buttonA -> ControllerInput.A
    this.buttonB -> ControllerInput.B
    this.buttonX -> ControllerInput.X
    this.buttonY -> ControllerInput.Y
    else -> null
}

interface ButtonHintsBarHelper {
    val buttonHintsBar: ButtonHintsBar

    fun updateButtonHints(buttonHints: Set<ButtonHint>) {
        buttonHintsBar.allButtons.forEach { it.gone() }
        var hasRequestedFocus = false
        buttonHints.forEach { buttonHint ->
            val controllerInput = buttonHint.controllerInput
            with(buttonHintsBar.getButtonHintView(controllerInput) ?: return@forEach) {
                text = buttonHint.appAction.toButtonText()
                icon = controllerInput.toIconRes()?.let { context.getDrawableExt(it) }
                setOnClickListener { v ->
                    buttonHintsBar.viewToControllerInput(v)
                        ?.let { onButtonHintsBarButtonHintClicked(buttonHint) }
                }
                visible()
                if (!hasRequestedFocus) {
                    hasRequestedFocus = true
                    requestFocus()
                }
            }
        }
    }

    fun onButtonHintsBarButtonHintClicked(buttonHint: ButtonHint)


}

