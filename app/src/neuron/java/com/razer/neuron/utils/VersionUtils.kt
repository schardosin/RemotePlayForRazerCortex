package com.razer.neuron.utils

import android.os.Build
import androidx.annotation.IntDef


/**
 * https://apilevels.com/
 *
 * To help with version naming and checking
 * e.g. [API_LEVEL34] has the same value as [ANDROID_14]
 * In API doc, it is either "Android 14" or "API level 34" the term "TIRAMISU" is not used a lot
 *
 * So calling [isAboveOrEqual] [API_LEVEL34] or [isAboveOrEqual] [ANDROID14] are the same thing
 *
 * This replaces the old "isAboveAndroidXX" where "XX" was the API level. It was also implying "above or equals to"
 *
 */
fun isAboveOrEqual(@AndroidVersion target: Int) = Build.VERSION.SDK_INT >= target
fun isBelowOrEqual(@AndroidVersion target: Int) = Build.VERSION.SDK_INT <= target

const val API_LEVEL34 = 34
const val API_LEVEL33 = Build.VERSION_CODES.TIRAMISU
const val API_LEVEL32 = Build.VERSION_CODES.S_V2
const val API_LEVEL31 = Build.VERSION_CODES.S
const val API_LEVEL30 = Build.VERSION_CODES.R
const val API_LEVEL29 = Build.VERSION_CODES.Q
const val API_LEVEL28 = Build.VERSION_CODES.P

const val ANDROID_14 = API_LEVEL34
const val ANDROID_13 = API_LEVEL33
const val ANDROID_12L = API_LEVEL32
const val ANDROID_12 = API_LEVEL31
const val ANDROID_11 = API_LEVEL30
const val ANDROID_10 = API_LEVEL29
const val ANDROID_9 = API_LEVEL28


@Retention(AnnotationRetention.SOURCE)
@IntDef(
    API_LEVEL34,
    API_LEVEL33,
    API_LEVEL32,
    API_LEVEL31,
    API_LEVEL30,
    API_LEVEL29,
    API_LEVEL28,
    ANDROID_14,
    ANDROID_13,
    ANDROID_12L,
    ANDROID_12,
    ANDROID_11,
    ANDROID_10,
    ANDROID_9,
)
annotation class AndroidVersion