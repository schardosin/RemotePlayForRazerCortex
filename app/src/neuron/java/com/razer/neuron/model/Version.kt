package com.razer.neuron.model

import androidx.annotation.StringDef
import com.limelight.BuildConfig
import com.limelight.nvstream.http.ComputerDetails


/**
 * For versioning
 */
data class Version(val major: String?, val minor: String?, val patch: String?, val build: String? = null, val versionCode : Int? = null) {
    val majorInt get() = major?.toIntOrNull()
    val minorInt get() = minor?.toIntOrNull()
    val patchInt get() = patch?.toIntOrNull()
    val buildInt get() = build?.toIntOrNull()

    fun string() = listOf(majorInt ?: 0, minorInt ?: 0, patchInt ?: 0, buildInt ?: 0).joinToString(".")

    companion object {
        fun parse(fullString: String): Version {
            return fullString.split(".")
                .map { it.trim() }
                .run {
                    Version(
                        major = getOrNull(0),
                        minor = getOrNull(1),
                        patch = getOrNull(2),
                        build = getOrNull(3),
                    )
                }
        }

        val appVersion get() = parse(BuildConfig.VERSION_NAME).copy(versionCode = BuildConfig.VERSION_CODE)
    }
}



const val CORTEX_PC = "CORTEX_PC"
const val SUNSHINE = "SUNSHINE"
const val SUNSHINE_RAZER_MOD = "SUNSHINE_RAZER_MOD"
@Retention(AnnotationRetention.SOURCE)
@StringDef(CORTEX_PC, SUNSHINE, SUNSHINE_RAZER_MOD)
annotation class HostVariant

/**
 * For debug display only
 */
@HostVariant
fun Version.getHostVariant() = when (build) {
    "-1" -> SUNSHINE
    "-3" -> SUNSHINE_RAZER_MOD
    "-4" -> CORTEX_PC
    else -> build?.let { "BUILD_${build}" }
}

/**
 * For debug display only
 */
fun ComputerDetails.getHostVariant() = serverVersion?.let { Version.parse(it) }?.getHostVariant()

