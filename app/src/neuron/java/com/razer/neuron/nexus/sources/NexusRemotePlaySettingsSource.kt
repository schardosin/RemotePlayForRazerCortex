package com.razer.neuron.nexus.sources

import android.content.Context
import android.database.Cursor
import android.preference.PreferenceManager
import com.razer.neuron.extensions.contains
import com.razer.neuron.extensions.contentValuesOf
import com.razer.neuron.extensions.getAnyByColumnName
import com.razer.neuron.extensions.getBooleanByColumnName
import com.razer.neuron.extensions.getFloatByColumnName
import com.razer.neuron.extensions.getIntByColumnName
import com.razer.neuron.extensions.getLongByColumnName
import com.razer.neuron.extensions.getStringByColumnName
import com.razer.neuron.extensions.toList
import com.razer.neuron.model.DisplayModeName
import com.razer.neuron.model.DisplayModeOption
import com.razer.neuron.shared.RazerRemotePlaySettingsKey
import com.razer.neuron.shared.SharedConstants
import com.razer.neuron.nexus.NexusContentSource
import com.razer.neuron.pref.RemotePlaySettingsPref.isReadOnlyKey
import com.razer.neuron.shared.SharedContentException
import kotlin.jvm.Throws

class NexusRemotePlaySettingsSource : NexusContentSource {
    override val path get() = SharedConstants.REMOTE_PLAY_SETTINGS
    private val uriPath get() = SharedConstants.baseNexusUri.buildUpon().appendPath(path).build()


    @Throws(SharedContentException::class)
    suspend fun getAll(context : Context): List<Triple<String, Any?, Class<*>?>> {
        return checkOrThrowPermission(context) {
            context.contentResolver.query(
                uriPath,
                null,
                null,
                null,
                null,
            ).use { c ->
                c?.toList {
                    val name = it.getStringByColumnName(SharedConstants.KRATE_ITEM_NAME) ?: ""
                    val valueAndClass = it.getAnyByColumnName(SharedConstants.KRATE_ITEM_VALUE)
                    Triple(name, valueAndClass?.first, valueAndClass?.second)
                } ?: emptyList()
            }
        }
    }

    /**
     * Should not save item where [isReadOnlyKey] is true
     */
    override suspend fun sync(context: Context) {
        logger("sync: getAll")
        val all = getAll(context)
        logger("sync: all=${all.size}")
        all.forEach { (key, value, clazz) ->
            if(!key.isReadOnlyKey()) {
                PreferenceManager.getDefaultSharedPreferences(context).edit().apply {
                    value?.let {
                        when (clazz) {
                            String::class.java -> putString(key, value as String)
                            Int::class.java -> putInt(key, value as Int)
                            Long::class.java -> putLong(key, value as Long)
                            Float::class.java -> putFloat(key, value as Float)
                            Boolean::class.java -> putBoolean(key, value as Boolean)
                        }
                        apply()
                    }
                }
                logger("sync: key=$key, value=$value (${clazz?.simpleName})")
            }
        }
    }


    fun getStringPref(context: Context, key: String): String? {
        getCursor(context, key)?.use {
            if (it.moveToFirst()) {
                return it.getStringByColumnName(SharedConstants.KRATE_ITEM_VALUE)
            }
        }
        return null
    }

    fun getIntPref(context: Context, key: String): Int? {
        getCursor(context, key)?.use {
            if (it.moveToFirst()) {
                return it.getIntByColumnName(SharedConstants.KRATE_ITEM_VALUE)
            }
        }
        return null
    }

    fun getBooleanPref(context: Context, key: String): Boolean? {
        getCursor(context, key)?.use {
            if (it.moveToFirst()) {
                return it.getBooleanByColumnName(SharedConstants.KRATE_ITEM_VALUE)
            }
        }
        return null
    }

    fun getLongPref(context: Context, key: String): Long? {
        getCursor(context, key)?.use {
            if (it.moveToFirst()) {
                return it.getLongByColumnName(SharedConstants.KRATE_ITEM_VALUE)
            }
        }
        return null
    }

    fun getFloatPref(context: Context, key: String): Float? {
        getCursor(context, key)?.use {
            if (it.moveToFirst()) {
                return it.getFloatByColumnName(SharedConstants.KRATE_ITEM_VALUE)
            }
        }
        return null
    }

    fun contains(context: Context, key: String): Boolean {
        return getCursor(context, key)?.contains(key) ?: false
    }

    private fun getCursor(context: Context, key: String): Cursor? {
        return context.contentResolver.query(
            uriPath,
            null,
            "${SharedConstants.KRATE_ITEM_NAME} = ?",
            arrayOf(key),
            null,
        )
    }


    @DisplayModeName
    private fun displayMode(context : Context) = runCatching { getStringPref(context, RazerRemotePlaySettingsKey.PREF_DISPLAY_MODE) }.getOrNull()

    fun isVirtualDisplayMode(context: Context) = (displayMode(context)?.let { DisplayModeOption.findByDisplayModeName(it) } ?: DisplayModeOption.default).isUsesVirtualDisplay

    fun isVirtualDisplayCropToSafeArea(context : Context) = isVirtualDisplayMode(context) && runCatching {  getBooleanPref(context, RazerRemotePlaySettingsKey.PREF_VIRTUAL_DISPLAY_CROP_TO_SAFE_AREA) }.getOrNull() == true


}