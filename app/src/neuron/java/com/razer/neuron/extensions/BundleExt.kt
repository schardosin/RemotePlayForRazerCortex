package com.razer.neuron.extensions

import android.os.Bundle

fun Bundle.getDescription() : String {
    return keySet()?.associateWith { get(it) }.toString() ?: "n/a"
}