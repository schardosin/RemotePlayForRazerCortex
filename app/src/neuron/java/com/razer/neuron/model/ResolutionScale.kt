package com.razer.neuron.model

import android.content.Context
import com.razer.neuron.extensions.getFullScreenSize
import kotlin.math.abs
import kotlin.math.min

enum class ResolutionScale(
    val swPixel: Int,
    val scaleValues: List<Float>
) {
    sw720p(720, listOf(1f)),
    sw1080(1080, listOf(1f, 1.2f, 1.5f, 1.75f)),
    sw1440p(1440, listOf(1f, 1.2f, 1.5f, 1.75f, 2f, 2.25f));

    companion object {
        
        // Microsoft’s own minimum screen size
        private const val MAGIC_PX = 720

        private fun getResolutionScale(context: Context): ResolutionScale {
            val swPixel = context.getFullScreenSize().run {
                min(width, height)
            }
            return entries.toTypedArray().minByOrNull { abs(swPixel - it.swPixel) } ?: sw720p
        }

        fun getUIScale(context: Context): Int {
            val scale = getResolutionScale(context)
            val density = context.resources.displayMetrics.density
            val closestScaleIndex = scale.scaleValues.indexOf(scale.scaleValues.minByOrNull { abs(it - density) })
            for (index in closestScaleIndex downTo 0) {
                if (scale.swPixel / scale.scaleValues[index] >= MAGIC_PX) {
                    return (scale.scaleValues[index] * 100).toInt()
                }
            }
            return (scale.scaleValues[0] * 100).toInt()
        }
    }
}
