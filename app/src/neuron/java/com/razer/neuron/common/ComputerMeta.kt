package com.razer.neuron.common;

import com.google.gson.annotations.SerializedName
import com.limelight.utils.defaultJson

data class ComputerMeta (
    @SerializedName("LastUsedTimestamp") val lastUsedTimestamp: Long?,
) {

    fun toJsonString() = runCatching { defaultJson.toJson(this, ComputerMeta::class.java) }.getOrNull()

    companion object {
        fun fromJson(json : String) = runCatching { defaultJson.fromJson(json, ComputerMeta::class.java) }.getOrNull()
    }

}