package com.razer.neuron.extensions

import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlin.math.abs

typealias NumberDropDownSetter<T> = (T) -> Unit
/**
 * Setup a [MaterialAutoCompleteTextView] so that it when user select an item
 * [saveValue] will be called.
 *
 * It will first retrieve the current value via [loadValue]
 *
 * @param names is the [String] for each [values]
 * @param values is the actual [Number] (e.g. [Int]) that will be saved
 * @param loadValue function to read the current [Number]
 * @param saveValue function to write the selected [Number]
 *
 * @return a [NumberDropDownSetter] for you to set the value with out triggering the listener
 */
fun <T : Number> MaterialAutoCompleteTextView.setNumberDropDown(
    names : Array<String>,
    values : Array<T>,
    initialValue: T,
    saveValue: (T) -> Unit) : NumberDropDownSetter<T> {
    fun T.findClosestValue() = values.minBy { abs(it.toDouble() - this.toDouble()) }
    fun T.findName() = names.getOrNull(values.indexOf(findClosestValue()))
    setOnClickListener {
        showDropDown()
    }
    var isListenerEnabled = true
    fun setter(value : T) {
        isListenerEnabled = false
        setText(value.let { it.findName() ?: it.toString() }, false)
        isListenerEnabled = true
    }
    setter(initialValue)
    setSimpleItems(names)
    setOnItemClickListener { _, _, position, _ ->
        if(!isListenerEnabled) return@setOnItemClickListener
        val value = values.getOrNull(position)
        value?.let { saveValue(it) }
    }
    return ::setter
}
