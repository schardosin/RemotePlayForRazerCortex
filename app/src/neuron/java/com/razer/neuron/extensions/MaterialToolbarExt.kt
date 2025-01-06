package com.razer.neuron.extensions

import android.view.View
import androidx.core.view.descendants
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import com.google.android.material.appbar.MaterialToolbar

/**
 * BAA-2401
 * Make all the child [View] unfocusable (i.e [View.setFocusable] to false)
 * until [View.doOnLayout].
 *
 * @return the number of children that matches [predicate] and already focusable and visible
 */
fun MaterialToolbar.setDescendantsUnfocusableUntilLayout(predicate: (View) -> Boolean = { it.isFocusable && it.isVisible }): Int {
    val n = 0
    descendants.toList().filter(predicate).forEach { descendant ->
        descendant.isFocusable = false
        doOnLayout {
            descendant.isFocusable = true
        }
    }
    return n
}