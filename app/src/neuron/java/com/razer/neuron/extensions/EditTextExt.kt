package com.razer.neuron.extensions

import android.view.View.OnFocusChangeListener
import android.widget.EditText
import androidx.core.widget.addTextChangedListener

/**
 * @param hint the text to show
 * @param existingOnFocusChangeListener is the [OnFocusChangeListener] you want to use for other purposes, since [EditText] only allow one
 * @param showHintPredicate is the predicate for when to show [hint]
 */
fun EditText.setHintOnlyWhenFocused(hint : CharSequence, existingOnFocusChangeListener : OnFocusChangeListener? = null, showHintPredicate : (CharSequence) -> Boolean = { it.isEmpty() }) {
    fun updateHint(text : CharSequence = this.text) = setHint(if(hasFocus() && showHintPredicate(text)) hint else "")
    setOnFocusChangeListener { v, hasFocus ->
        updateHint()
        existingOnFocusChangeListener?.onFocusChange(v, hasFocus)
    }
    updateHint()
    addTextChangedListener(onTextChanged = {
        text, _, _, _ ->
        updateHint(text.toString())
    })
}