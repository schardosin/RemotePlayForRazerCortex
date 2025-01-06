package com.razer.neuron.provider

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri


/**
 * This interface will be used by [com.razer.neuron.provider.NeuronContentProvider] to support different types of [Uri]
 */

interface NeuronContentSource {

    /**
     * This is the [android.net.Uri.getLastPathSegment]
     */
    val path: String

    /**
     * See [android.content.ContentProvider.query]
     */
    fun query(context : Context, uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor?

    /**
     * See [android.content.ContentProvider.insert]
     */
    fun insert(context : Context, uri: Uri, values: ContentValues?): Uri?

    /**
     * See [android.content.ContentProvider.delete]
     */
    fun delete(context : Context, uri: Uri, whereClause: String?, whereArgs: Array<out String>?): Int

    /**
     * See [android.content.ContentProvider.update]
     */
    fun update(context : Context, uri: Uri, values: ContentValues?, whereClause: String?, whereArgs: Array<out String>?): Int
}