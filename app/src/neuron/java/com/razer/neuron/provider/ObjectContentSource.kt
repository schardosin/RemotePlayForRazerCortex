package com.razer.neuron.provider

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri


/**
 * Implements [NeuronContentSource] but assumes:
 * 0. [query], [insert], [update] and [delete] are all of the same type (ie [T])
 * 1. Only supports 1 select condition and one args
 * 2. No sorting and no projection
 * 3. All writes are insert or replace
 * 4. [T] must implement [ToCursor]
 *
 */
interface ObjectContentSource<T, F : NeuronProviderModelHelper<T>> : NeuronContentSource {
    /**
     * A [SharedModelHelper] to help create the object
     */
    val modelHelper: F

    /**
     * Create a [T] object using [ContentValues]
     */
    fun createItem(values: ContentValues): T = modelHelper.create(values)


    override fun query(context : Context, uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        val result = query(context, uri, toMatchValues(selection, selectionArgs))
        return modelHelper.createCursor(result)
    }

    override fun insert(context : Context, uri: Uri, values: ContentValues?): Uri? {
        values ?: return null
        return insertOrReplace(context, uri, createItem(values), null)
    }

    override fun delete(context : Context, uri: Uri, whereClause: String?, whereArgs: Array<out String>?): Int {
        return delete(context, uri, toMatchValues(whereClause, whereArgs))
    }

    override fun update(context : Context, uri: Uri, values: ContentValues?, whereClause: String?, whereArgs: Array<out String>?): Int {
        values ?: return 0
        insertOrReplace(context, uri, createItem(values), toMatchValues(whereClause, whereArgs))
        return 1
    }

    /**
     * Get a [List] of [T] objects that matches [matchKeyValues]
     *
     * @param uri Can be ignored
     * @param matchKeyValues The field of the [Pair.first] must match
     */
    @Throws(SharedContentProviderException::class)
    fun query(
        context : Context,
        uri: Uri,
        matchKeyValues: Array<Pair<String, Any?>>?,
    ): List<T>


    /**
     * Delete zero or many [T] objects that matches [matchKeyValues]
     *
     * @param uri Can be ignored
     * @param matchKeyValues The field of the [Pair.first] must match
     *
     * @return the number of objects deleted
     */
    fun delete(
        context : Context,
        uri: Uri,
        matchKeyValues: Array<Pair<String, Any?>>?,
    ): Int


    /**
     * insertOrReplace zero or many [T] objects that matches [matchKeyValues]
     *
     * @param uri Can be ignored
     * @param matchKeyValues The field of the [Pair.first] must match
     *
     * @return the number of objects deleted
     */
    fun insertOrReplace(
        context : Context,
        uri: Uri,
        item: T,
        matchKeyValues: Array<Pair<String, Any?>>?,
    ): Uri?


    /**
     * Convert a [String] selection statement (with placeholder as '?') and a [Array] of values
     * into [Array] (of 1x [Pair]) where [Pair.first] is a key in [supportedFields]
     *
     * @param selection A single expression like "key = ?"
     * @param selectionArgs A single element like "1234" or 1234
     */
    @kotlin.jvm.Throws(RuntimeException::class)
    fun toMatchValues(
        selection: String?,
        selectionArgs: Array<out String>?
    ): Array<Pair<String, Any?>> {
        if (selection.isNullOrBlank()) return arrayOf()
        check((selection.split("AND", "OR", "(", ")").size) <= 1) { "selection only supports one condition" }
        check((selectionArgs?.size ?: 0) <= 1) { "selection only supports one condition" }
        val expression = selection.split("=").map { it.trim() }
        if (expression.size <= 1) return arrayOf()
        var (column, _expectedValue) = expression.getOrNull(0) to expression.getOrNull(1)
        require(column != null) { "column cannot be null" }
        require(modelHelper.isValidField(column)) { "$column not supported" }
        if (_expectedValue == "?") {
            _expectedValue = selectionArgs?.getOrNull(0)
        }

        val fieldType = modelHelper.getValueTypeByName(column)
        val expectedValue = when (fieldType) {
            Int::class.java -> _expectedValue?.toIntOrNull()
            Long::class.java -> _expectedValue?.toLongOrNull()
            Float::class.java -> _expectedValue?.toFloatOrNull()
            Boolean::class.java -> _expectedValue.toBoolean()
            else -> _expectedValue
        }
        return arrayOf(column to expectedValue)
    }
}

/**
 * A general exception when calling [ObjectContentSource] functions
 */
class SharedContentProviderException(msg: String) : Exception(msg)

