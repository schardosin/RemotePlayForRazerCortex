package com.razer.neuron.utils

import android.os.SystemClock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


/**
 * Current time
 */
fun now() = System.currentTimeMillis()

/**
 * Duration since boot
 *
 * In unit test [SystemClock.elapsedRealtime] doesn't work
 */
fun elapsedRealtime() : Long {
    return SystemClock.elapsedRealtime()
}


const val ISO8601 = "yyyy-MM-dd'T'HH:mm:ssZ"
const val ISO8601_SSS = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

const val ISO8601_SHORT = "HH:mm:ssZ"
const val ISO8601_SHORT_SSS = "HH:mm:ss.SSSZ"


/**
 * Convert a [Long] duration to a readable [String] for developer or debugging
 */
@JvmOverloads
fun Long.toDebugTimeString(
    includeMilliseconds: Boolean = false,
    timeZone: TimeZone = TimeZone.getDefault()
) = this.toISO8601String(includeMilliseconds = includeMilliseconds, timeZone = timeZone)

fun Long.toDebugTimeShortString(
    includeMilliseconds: Boolean = false,
    timeZone: TimeZone = TimeZone.getDefault()
) = toDateTimeString(if (includeMilliseconds) ISO8601_SSS else ISO8601, timeZone = timeZone)

fun Long.toISO8601String(
    includeMilliseconds: Boolean = false,
    timeZone: TimeZone = TimeZone.getDefault(),
    short : Boolean = false
) = toDateTimeString(if (includeMilliseconds) (if(short) ISO8601_SSS else ISO8601_SHORT_SSS) else (if(short) ISO8601 else ISO8601_SHORT) , timeZone = timeZone)

fun Long.toDateTimeString(
    customFormat : String,
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    return SimpleDateFormat(
        customFormat,
        Locale.US
    ).apply { this.timeZone = timeZone }.format(Date(this))
}
