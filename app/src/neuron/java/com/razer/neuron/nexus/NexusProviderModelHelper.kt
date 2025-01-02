package com.razer.neuron.nexus

import android.content.ContentValues
import android.database.Cursor


/**
 * A interface to help provide code structure when writing code that will:
 * - Create [T] objects from [Cursor]
 * - Create [ContentValues] from object [T]
 */
interface NexusProviderModelHelper<T> {
    /**
     * All fields of class [T]
     *
     * Don't use optional. Only [String], [Any], [Int], [Long], [Float] [Boolean] are supported
     */
    val allFields: Map<String, Class<*>>

    /**
     * Standard function to convert [Cursor] to [List] of [T]
     */
    fun createList(cursor: Cursor) : List<T>


    /**
     * Standard function to convert [obj] [T] to [ContentValues]
     */
    fun contentValuesOf(obj: T) : ContentValues
}