package com.razer.neuron.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Checkable
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import com.limelight.R
import com.limelight.databinding.RnRadioButtonViewBinding

/**
 * Do not implement [Checkable] or subclass [CheckableConstraintLayout] since the
 * design requires the image to not have a checkable state
 */
class RnRadioButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val binding = RnRadioButtonViewBinding.inflate(LayoutInflater.from(context), this)

    private val tvTitle by lazy { binding.tvTitle }
    val radioBtn by lazy { binding.radioBtn }
    val clickableLayout by lazy { binding.focusLayout }

    private var internalOnClickListener: OnClickListener? = null

    var onCheckedChangeListener: OnCheckedChangeListener? = null

    var isChecked: Boolean
        get() = radioBtn.isChecked
        set(value) {
            if (radioBtn.isChecked != value) {
                radioBtn.isChecked = value
            }
        }

    init {
        setBackgroundResource(R.drawable.bg_transparent)
        isFocusable = false
        isClickable = false
        clickableLayout.isFocusable = true
        clickableLayout.setOnClickListener {
            radioBtn.toggle()
            internalOnClickListener?.onClick(this)
        }
        radioBtn.isFocusable = false
        radioBtn.isClickable = false
        radioBtn.setOnCheckedChangeListener { compoundButton, isButtonChecked ->
            onCheckedChangeListener?.onCheckedChanged(compoundButton, isButtonChecked)
            uncheckSiblings()
        }
    }

    private fun uncheckSiblings() {
        if (isChecked) {
            (parent as? ViewGroup)?.children?.mapNotNull { if (it != this@RnRadioButton && it is RnRadioButton && it.isChecked) it else null }
                ?.forEach {
                    it.isChecked = false
                }
        }
    }

    fun update(
        title: CharSequence,
        isChecked: Boolean
    ) {
        tvTitle.text = title
        this.isChecked = isChecked
    }

    override fun setOnClickListener(l: OnClickListener?) {
        internalOnClickListener = l
    }

}
