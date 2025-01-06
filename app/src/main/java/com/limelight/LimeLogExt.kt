package com.limelight

import kotlin.math.min

/**
 * [LimeLog] cannot print more than certain characters per line (similar to Logcat)
 *
 * So we need to send it in chunks
 */
fun chunk(line : String?, maxLogLineLength: Int = 2000, maxChunks : Int = 10, log: (String) -> Unit = LimeLog::info) {
   line?.takeIf { it.isNotEmpty() } ?: return
    var offset = 0
    var i = 0
    while (offset < line.length && i < maxChunks) {
        val endPos = min(line.length, offset + maxLogLineLength)
        log(line.substring(offset, endPos))
        offset = endPos
        i++
    }
}