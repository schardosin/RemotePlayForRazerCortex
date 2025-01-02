package com.razer.neuron.provider

import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor

/**
 * A interface to help provide code structure when writing code that will:
 * - Create [Cursor] from [T] objects
 * - Create object [T] from [ContentValues]
 */
interface NeuronProviderModelHelper<T> {
    /**
     * All fields of class [T]
     *
     * Don't use optional. Only [String], [Any], [Int], [Long], [Float] [Boolean] are supported
     */
    val allFields : Map<String, Class<*>>

    /**
     * Create an instance of [T] using [values]
     */
    fun create(values: ContentValues): T

    /**
     * true if [name] is in [allFields]
     */
    fun isValidField(name: String) = allFields.containsKey(name)

    /**
     * Get field type from [allFields] by [name]
     */
    fun getValueTypeByName(name: String): Class<*> {
        return allFields[name] ?: String::class.java
    }

    /**
     * Return an [Array] of [Pair] for each field of this object (even if it the field value is null)
     */
    fun toKeyValuePairs(obj : T): Array<Pair<String, Any?>>


    /**
     * Convert a [List] of [T] to [Cursor]
     */
    fun createCursor(list: List<T>): Cursor? {
        val iterator = list.listIterator()
        val firstRow = if (iterator.hasNext()) toKeyValuePairs(iterator.next()) else return null
        val cursor = MatrixCursor(firstRow.map { it.first }.toTypedArray()).apply {
            addRow(firstRow.map { it.second }.toTypedArray())
        }
        while (iterator.hasNext()) {
            val row = toKeyValuePairs(iterator.next()).takeIf { it.size == firstRow.size }
                ?: throw IllegalStateException("Key values size must be consistent with first row")
            cursor.addRow(row.map { it.second }.toTypedArray())
        }
        return cursor
    }
}