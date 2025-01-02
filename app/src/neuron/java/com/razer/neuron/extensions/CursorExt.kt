package com.razer.neuron.extensions

import android.database.Cursor
import android.media.session.PlaybackState.CustomAction
import androidx.core.database.getBlobOrNull
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getFloatOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull

typealias CursorRow = List<Pair<String, Any?>>

/**
 * Convert [Cursor] from first to last to a [List] of [CursorRow]
 */
fun Cursor.toRows(): List<CursorRow> {
    val result = mutableListOf<CursorRow>()
    this.use { cursor ->
        val colIndexes = mutableMapOf<Int, String>()
        if (!cursor.moveToFirst()) return listOf()
        while (!cursor.isAfterLast) {
            result.add(cursor.toRow(colIndexes))
            cursor.moveToNext()
        }
    }
    return result.toList()
}

/**
 * Convert current [Cursor] to [CursorRow] (i.e. without moving to next)
 */
fun Cursor.toRow(columnNamesCache: MutableMap<Int, String>): CursorRow {
    val result = mutableListOf<Pair<String, Any?>>()
    (0 until this.columnCount).map { colIndex ->
        val columnName = columnNamesCache[colIndex]
            ?: getColumnName(colIndex).also { columnNamesCache[colIndex] = it }
        val value = when (getType(colIndex)) {
            Cursor.FIELD_TYPE_NULL -> null
            Cursor.FIELD_TYPE_INTEGER -> getLongOrNull(colIndex) ?: getIntOrNull(colIndex)
            Cursor.FIELD_TYPE_FLOAT -> getFloatOrNull(colIndex) ?: getDoubleOrNull(colIndex)
            Cursor.FIELD_TYPE_STRING -> getStringOrNull(colIndex)
            Cursor.FIELD_TYPE_BLOB -> getBlobOrNull(colIndex)
            else -> null
        }
        result.add(columnName to value)
    }
    return result.toList()
}




/**
 * [Cursor] to [List] of [K]
 */
fun <K> Cursor.toList(transform: (Cursor) -> K?) = mutableListOf<K>().apply {
    if(isBeforeFirst) moveToFirst()
    while (!isAfterLast) {
        transform(this@toList)?.let { add(it) }
        moveToNext()
    }
}.toList()

fun Cursor.getBlobByColumnName(name: String) =
    getColumnIndex(name).takeIf { it > -1 }?.let { getBlobOrNull(it) }

fun Cursor.getStringByColumnName(name: String) =
    getColumnIndex(name).takeIf { it > -1 }?.let { getStringOrNull(it) }

fun Cursor.getAnyByColumnName(name: String) : Pair<Any?, Class<*>>? {
    val index = getColumnIndex(name)
    val type = index.takeIf { it > -1 }?.let { getType(it) } ?: Cursor.FIELD_TYPE_STRING
    return when(type) {
        Cursor.FIELD_TYPE_FLOAT -> getFloatOrNull(index) to Float::class.java
        Cursor.FIELD_TYPE_INTEGER -> getIntOrNull(index) to Int::class.java
        Cursor.FIELD_TYPE_STRING -> {
            // boolean type will be treated as string
            val value = getStringOrNull(index)
            val boolValue = value?.toBooleanStrictOrNull()
            if (boolValue != null)
                boolValue to Boolean::class.java
            else
                value to String::class.java
        }
        else -> null
    }
}



fun Cursor.getIntByColumnName(name: String) =
    getColumnIndex(name).takeIf { it > -1 }?.let { getIntOrNull(it) }

/**
 * Column value can be:
 * - If [Int]: 1 is true, else false
 * - else [String.toBoolean] will be used (i.e "true" is true else false)
 */
fun Cursor.getBooleanByColumnName(name: String) =
    getStringByColumnName(name)?.let {
            stringValue ->
        stringValue.toIntOrNull()?.let { it == 1 } ?: stringValue.toBoolean()
    }

fun Cursor.getLongByColumnName(name: String) =
    getColumnIndex(name).takeIf { it > -1 }?.let { getLongOrNull(it) }

fun Cursor.getFloatByColumnName(name: String) =
    getColumnIndex(name).takeIf { it > -1 }?.let { getFloatOrNull(it) }

fun Cursor.contains(name: String): Boolean =
    getColumnIndex(name) > -1

