package com.razer.neuron.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Checkable
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import com.limelight.R
import com.limelight.databinding.RnImageRadioButtonViewBinding
import com.razer.neuron.extensions.gone
import com.razer.neuron.extensions.visible

/**
 * Do not implement [Checkable] or subclass [CheckableConstraintLayout] since the
 * design requires the image to not have a checkable state
 */
class ImageRadioButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val binding = RnImageRadioButtonViewBinding.inflate(LayoutInflater.from(context), this)

    private val tvTitle by lazy { binding.irbTvTitle }
    private val tvSubtitle by lazy { binding.irbTvSubtitle }

    val ivIcon by lazy { binding.irbIvIcon }
    val radioBtn by lazy { binding.irbRadioBtn }
    val clickableLayout by lazy { binding.irbFocusLayout }

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
            (parent as? ViewGroup)?.children?.mapNotNull { if (it != this@ImageRadioButton && it is ImageRadioButton && it.isChecked) it else null }
                ?.forEach {
                    it.isChecked = false
                }
        }
    }

    fun update(
        title: CharSequence,
        subtitle: CharSequence?,
        @DrawableRes iconRes: Int,
        isChecked: Boolean
    ) {
        tvTitle.text = title
        subtitle?.let {
            tvSubtitle.text = it
            tvSubtitle.visible()
        } ?: run {
            tvSubtitle.text = ""
            tvSubtitle.gone()
        }
        this.isChecked = isChecked
        ivIcon.setImageResource(iconRes)
    }


    override fun setOnClickListener(l: OnClickListener?) {
        internalOnClickListener = l
    }

}
