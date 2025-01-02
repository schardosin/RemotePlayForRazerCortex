package com.razer.neuron.model

import android.util.Size
import com.google.gson.FieldNamingPolicy
import com.limelight.NeuronBridgeInterface
import com.limelight.binding.video.VideoStats
import com.limelight.binding.video.VideoStatsFps
import timber.log.Timber

import com.razer.neuron.extensions.defaultJson
import com.razer.neuron.settings.devoptions.DevOptionsHelper
import com.razer.neuron.utils.now


/**
 * BAA-2249
 *
 * A class to encapsulate various [VideoStats] from [NeuronBridgeInterface.updateVideoStats]
 *
 * [SessionStats] can be exported via [DevOptionsHelper.export]
 */
data class SessionStats(
    val startedAt: Long = now(),
    val randomId : String,
) {
    companion object {
        var lastSession: SessionStats? = null

        fun String.fromJson() = runCatching {
            defaultJson
                .newBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
                .fromJson(this, SessionStats::class.java)
        }.apply {
            exceptionOrNull()?.takeIf { isFailure }?.let {
                Timber.e(it)
            }
        }.getOrNull()
    }

    fun toJson(prettyPrinting : Boolean = false) = runCatching {
        defaultJson.newBuilder()
            .apply { if(prettyPrinting) setPrettyPrinting() else Unit }
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
            .toJson(this)
    }.apply {
        exceptionOrNull()?.takeIf { isFailure }?.let {
            Timber.e(it)
        }
    }.getOrNull()

    var decoderName : String? = null
    var initialWidth : Int? = null
    var initialHeight : Int? = null
    var avgFps : VideoStatsFps? = null
    var netDropsPercent : Float? = null
    var avgNetLatencyMs : Int? = null
    var avgNetLatencyVarianceMs : Int? = null
    var minHostProcessingLatencyMs : Float? = null
    var maxHostProcessingLatencyMs : Float? = null
    var avgHostProcessingLatencyMs : Float? = null
    var decodeTimeMs : Float? = null
    var lastUpdatedAt : Long? = null



    var globalStats: VideoStats? = null
    var lastStats: VideoStats? = null
    var activeStats: VideoStats? = null

    val age get() = now() - startedAt

    val elapsedTimeMs get() = lastUpdatedAt?.let { it - startedAt }

    fun update(decoder : String,
               initialSize: Size,
               avgNetLatency: Int,
               avgNetLatencyVarianceMs: Int,
               globalVideoStats : VideoStats?,
               lastWindowVideoStats : VideoStats?,
               activeWindowVideoStats : VideoStats?) {
        this.decoderName = decoder
        this.globalStats = globalVideoStats
        this.lastStats = lastWindowVideoStats
        this.activeStats = activeWindowVideoStats
        val lastTwo = VideoStats()
        lastTwo.add(lastWindowVideoStats)
        lastTwo.add(activeWindowVideoStats)
        val fps = lastTwo.getFps()
        this.avgFps = fps
        this.decodeTimeMs = lastTwo.decoderTimeMs.toFloat() / lastTwo.totalFramesReceived
        this.initialWidth = initialSize.width
        this.initialHeight = initialSize.height
        this.netDropsPercent = lastTwo.framesLost.toFloat() / lastTwo.totalFrames * 100
        this.avgNetLatencyMs = avgNetLatency
        this.avgNetLatencyVarianceMs = avgNetLatencyVarianceMs
        this.lastUpdatedAt = now()
        if(lastTwo.framesWithHostProcessingLatency > 0) {
            this.minHostProcessingLatencyMs = lastTwo.minHostProcessingLatency.code.toFloat() / 10f
            this.maxHostProcessingLatencyMs = lastTwo.maxHostProcessingLatency.code.toFloat() / 10f
            this.avgHostProcessingLatencyMs = lastTwo.totalHostProcessingLatency.toFloat() / 10f / lastTwo.framesWithHostProcessingLatency
        }
    }

}