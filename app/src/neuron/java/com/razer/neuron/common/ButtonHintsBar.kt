package com.razer.neuron.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButton
import com.limelight.databinding.RnLayoutButtonHintsBarBinding

class ButtonHintsBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: RnLayoutButtonHintsBarBinding

    val buttonB by lazy { binding.btnHintB }
    val buttonY by lazy { binding.btnHintY }
    val buttonX by lazy { binding.btnHintX }
    val buttonA by lazy { binding.btnHintA }

    val allButtons get() = listOf(buttonA, buttonB, buttonX, buttonY)

    init {
        binding = RnLayoutButtonHintsBarBinding.inflate(LayoutInflater.from(context), this)
    }

    fun setButton(button: MaterialButton, text: String, @DrawableRes icon: Int) {
        button.text = text
        button.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)
    }
}