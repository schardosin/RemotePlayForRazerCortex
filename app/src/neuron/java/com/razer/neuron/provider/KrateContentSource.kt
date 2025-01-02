package com.razer.neuron.provider

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import com.razer.neuron.shared.SharedConstants
import hu.autsoft.krate.Krate

/**
 * A wrapper for accessing [Krate] implementation
 */
abstract class KrateContentSource : ObjectContentSource<KrateItem, KrateItem.Companion> {
    override val modelHelper = KrateItem

    abstract val krate: Krate

    /**
     * If true, the [KrateItem.name] with this [name] will not be:
     * - [delete]
     * - [insertOrReplace]
     */
    open fun isReadOnly(name : String) = false

    override fun query(context : Context, uri: Uri, matchKeyValues: Array<Pair<String, Any?>>?): List<KrateItem> {
        val match : Pair<String, Any?>? = matchKeyValues?.firstOrNull { it.first == KrateItem.FIELD_NAME }
        val result = mutableListOf<KrateItem>()
        val all = krate.sharedPreferences.all
        if(match != null) {
            val (_, prefName) = match
            if (prefName is String && krate.sharedPreferences.contains(prefName)) {
                result += KrateItem(prefName, all[prefName])
            } else {
                result.addAll(all.map { (key, any) -> KrateItem(key, any) })
            }
        }  else {
            result.addAll(all.map { (key, any) -> KrateItem(key, any) })
        }
        return result.filter { it.isValidValueType }
    }

    override fun delete(context : Context, uri: Uri, matchKeyValues: Array<Pair<String, Any?>>?): Int {
        val (_, prefName) = requireNotNull(matchKeyValues?.firstOrNull { it.first == KrateItem.FIELD_NAME }) {
            "${KrateItem.FIELD_NAME} must be specified for query"
        }
        require(prefName is String) { "${prefName} must be string" }
        var result = 0
        with(krate.sharedPreferences) {
            if (contains((prefName)) && !isReadOnly(prefName)) {
                edit {
                    remove(prefName)
                }
                result++
            }
        }
        return result
    }

    override fun insertOrReplace(context : Context, uri: Uri, item: KrateItem, matchKeyValues: Array<Pair<String, Any?>>?): Uri? {
        val (_, prefName) = requireNotNull(matchKeyValues?.firstOrNull { it.first == KrateItem.FIELD_NAME }) {
            "${KrateItem.FIELD_NAME} must be specified for query"
        }
        require(prefName is String) { "${prefName} must be string" }
        if(isReadOnly(prefName))
            return uri
        with(krate.sharedPreferences) {
            edit {
                put(prefName, item.value)
            }
        }
        return uri
    }


    @Throws(RuntimeException::class)
    private fun SharedPreferences.put(key: String, any: Any?) {
        edit {
            if (any == null) {
                remove(key)
                return@edit
            }
            when (any) {
                is Int -> putInt(key, any)
                is Long -> putLong(key, any)
                is Float -> putFloat(key, any)
                is Boolean -> putBoolean(key, any)
                else -> putString(key, any.toString())
            }
        }
    }
}

/**
 * Container for objects read/write to [com.razer.neuron.provider.KrateContentSource]
 */
data class KrateItem(
    val name: String,
    val value: Any? = null
) {
    val isValidValueType get() = when(value) {
        is Boolean, is Int, is Long, is Float, is String -> true
        else -> false
    }

    companion object : NeuronProviderModelHelper<KrateItem> {
        const val FIELD_NAME = SharedConstants.KRATE_ITEM_NAME
        const val FIELD_VALUE = SharedConstants.KRATE_ITEM_VALUE

        override val allFields = listOf(FIELD_NAME to String::class.java, FIELD_VALUE to Any::class.java).toMap()

        override fun create(values: ContentValues): KrateItem {
            return KrateItem(
                name = (values[FIELD_NAME] as? String) ?: throw IllegalArgumentException("$FIELD_NAME not specified"),
                value = values[FIELD_VALUE]
            )
        }

        override fun toKeyValuePairs(obj: KrateItem) = arrayOf(FIELD_NAME to obj.name, FIELD_VALUE to obj.value)

    }
}