package com.razer.neuron.extensions

import android.content.SharedPreferences
import hu.autsoft.krate.Krate
import hu.autsoft.krate.base.KeyedKrateProperty
import hu.autsoft.krate.base.KeyedKratePropertyProvider
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * [enumPref2] saves the [Enum.name] rather than [Enum.ordinal]
 * [defaultValue] is the default value if:
 * - Saved value is not an [Enum.name]
 * - Saved value is null
 */
fun <K : Enum<*>> enumPref2(
    key: String? = null,
    enumClass: KClass<K>,
    defaultValue: K,
    useEnumName : Boolean = true,
): KeyedKratePropertyProvider<K> {
    return EnumDelegateProvider2(key, enumClass, defaultValue, useEnumName)
}

internal class EnumDelegateProvider2<T : Enum<*>>(
    val key: String?,
    private val enumClass: KClass<T>,
    private val defaultValue : T,
    private val useEnumName : Boolean,
) : KeyedKratePropertyProvider<T> {
    override fun provideDelegate(thisRef: Krate, property: KProperty<*>): EnumDelegate2<T> {
        return EnumDelegate2(key ?: property.name, enumClass, defaultValue, useEnumName)
    }
}

internal class EnumDelegate2<T : Enum<*>>(
    override val key: String,
    enumClass: KClass<T>,
    private val defaultValue : T,
    /**
     * Save the value with [Enum.name]
     */
    private val useEnumName : Boolean,
) : KeyedKrateProperty<T> {
    private val enumConstants = enumClass.java.enumConstants

    override operator fun getValue(thisRef: Krate, property: KProperty<*>): T {
        return if (!thisRef.sharedPreferences.contains(key)) {
            defaultValue
        } else {
            if(useEnumName) {
                val stringValue = try { thisRef.sharedPreferences.getString(key, null) } catch (t : Throwable) { null }
                if(stringValue == null) {
                    defaultValue
                } else {
                    enumConstants?.firstOrNull { it.name == stringValue } ?: defaultValue
                }
            } else {
                val value = thisRef.sharedPreferences.getInt(key, 0)
                enumConstants?.firstOrNull { it.ordinal == value } ?: defaultValue
            }
        }
    }

    override operator fun setValue(thisRef: Krate, property: KProperty<*>, value: T) {
        if(useEnumName) {
            thisRef.sharedPreferences.edit { putString(key, value.name) }
        } else {
            thisRef.sharedPreferences.edit { putInt(key, value.ordinal) }
        }
    }
}

inline fun SharedPreferences.edit(edits: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.edits()
    editor.apply()
}



@Throws(RuntimeException::class)
fun SharedPreferences.put(key: String, any: Any?) = edit { putAny(key, any) }

@Throws(RuntimeException::class)
fun SharedPreferences.Editor.putAny(key: String, any: Any?) = runCatching {
    if (any == null) {
        remove(key)
        return@runCatching
    }
    when (any) {
        is Int -> putInt(key, any)
        is Long -> putLong(key, any)
        is Float -> putFloat(key, any)
        is Boolean -> putBoolean(key, any)
        is String -> putString(key, any)
        else -> error("Type ${any::class.simpleName} not supported")
    }
}
