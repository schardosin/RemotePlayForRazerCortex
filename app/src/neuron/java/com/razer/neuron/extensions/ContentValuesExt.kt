package com.razer.neuron.extensions

import android.content.ContentValues

/**
 * Creates a [ContentValues] with [Pair]s
 */
@Throws(RuntimeException::class)
fun contentValuesOf(vararg values : Pair<String, Any?>) = ContentValues().apply {
    values.forEach { (key, value) ->
        if(value == null) {
            putNull(key)
            return@forEach
        }
        when(value) {
            is Int -> put(key, value)
            is Long -> put(key, value)
            is Float -> put(key, value)
            is Boolean -> put(key, value)
            is String -> put(key, value.toString())
            else -> throw IllegalArgumentException("$key of type ${value.javaClass.simpleName} not supported")
        }
    }
}