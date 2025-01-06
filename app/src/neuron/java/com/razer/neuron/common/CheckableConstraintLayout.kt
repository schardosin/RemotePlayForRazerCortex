package com.razer.neuron.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import androidx.annotation.CallSuper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children


open class CheckableConstraintLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), Checkable {


    private var userOnClickListener: View.OnClickListener? = null

    init {
        isClickable = true
        super.setOnClickListener { v ->
            onLayoutClicked(v)
        }
    }

    @CallSuper
    protected open fun onLayoutClicked(v: View) {
        checkSelfAndUncheckSiblings()
        userOnClickListener?.onClick(v)
    }

    /**
     * Fake focus this view (by setting [isChecked] to true and other sibling view to false)
     */
    fun checkSelfAndUncheckSiblings() {
        (parent as? ViewGroup)?.children?.forEach { v ->
            if (v is Checkable) {
                v.isChecked = v == this
            }
        }
    }

    private fun uncheckSiblings() {

    }


    override fun setOnClickListener(onClickListener: View.OnClickListener?) {
        userOnClickListener = onClickListener
    }

    private var isCheckedInternal = false
    override fun setChecked(isChecked: Boolean) {
        if (isChecked != isCheckedInternal) {
            isCheckedInternal = isChecked
            refreshDrawableState()
        }
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked) {
            mergeDrawableStates(drawableState, intArrayOf(android.R.attr.state_checked));
        }
        return drawableState
    }


    override fun isChecked() = isCheckedInternal

    override fun toggle() {
        isChecked = !isChecked
    }
}