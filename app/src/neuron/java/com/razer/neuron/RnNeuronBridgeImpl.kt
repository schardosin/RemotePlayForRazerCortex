package com.razer.neuron

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.preference.PreferenceManager
import android.util.Size
import android.view.Window
import com.limelight.Game
import com.limelight.NeuronBridgeInterface
import com.limelight.R
import com.limelight.RemotePlayConfig
import com.limelight.binding.video.VideoStats
import com.limelight.nvstream.ConnectionContext
import com.limelight.nvstream.http.DisplayMode
import com.limelight.nvstream.http.toResolution
import com.limelight.preferences.PreferenceConfiguration
import com.razer.neuron.managers.AIDLManager
import com.razer.neuron.extensions.edit
import com.razer.neuron.extensions.getAllSupportedNativeFps
import com.razer.neuron.extensions.getPPI
import com.razer.neuron.extensions.getScreenResolution
import com.razer.neuron.extensions.getUserFacingMessage
import com.razer.neuron.managers.RumbleManager.isRumbleWithNexus
import com.razer.neuron.model.DisplayModeOption
import com.razer.neuron.model.ResolutionScale
import com.razer.neuron.model.SessionStats
import com.razer.neuron.model.isDesktop
import com.razer.neuron.pref.RemotePlaySettingsPref
import com.razer.neuron.shared.RazerRemotePlaySettingsKey
import com.razer.neuron.shared.SharedConstants
import com.razer.neuron.shared.SharedConstants.DEFAULT_LIST_FPS
import com.razer.neuron.shared.SharedConstants.DEFAULT_LIST_RESOLUTION
import com.razer.neuron.shared.SharedConstants.REDUCE_REFRESH_RATE_PREF_STRING
import com.razer.neuron.utils.calculateVirtualDisplayMode
import com.razer.neuron.utils.getDefaultDisplayRefreshRateHz
import com.razer.neuron.utils.getDeviceNickName
import kotlinx.coroutines.future.future
import timber.log.Timber
import java.util.concurrent.CompletableFuture
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * You can use com.razer.neuron.xxx classes here
 */
class RnNeuronBridgeImpl(val context: Context) : NeuronBridgeInterface {
    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    private val displayMode get() = prefs.getString(RazerRemotePlaySettingsKey.PREF_DISPLAY_MODE, null)?.let { DisplayModeOption.findByDisplayModeName(it) }

    override val isLaunchWithVirtualDisplay
        get() = displayMode?.isUsesVirtualDisplay == true

    val isLimitRefreshRate get() = prefs.getBoolean(REDUCE_REFRESH_RATE_PREF_STRING, false)

    override fun getUserFacingMessage(t: Throwable) = t.getUserFacingMessage()

    private var listResolution: String
        get() {
            return RemotePlaySettingsPref.sharedPreferences.getString(
                PreferenceConfiguration.RESOLUTION_PREF_STRING,
                SharedConstants.DEFAULT_LIST_RESOLUTION
            ) ?: SharedConstants.DEFAULT_LIST_RESOLUTION
        }
        set(value) {
            RemotePlaySettingsPref.sharedPreferences.edit {
                putString(PreferenceConfiguration.RESOLUTION_PREF_STRING, value)
            }
        }

    private var listFps: String
        get() {
            return RemotePlaySettingsPref.sharedPreferences.getString(
                PreferenceConfiguration.FPS_PREF_STRING,
                SharedConstants.DEFAULT_LIST_FPS
            ) ?: SharedConstants.DEFAULT_LIST_FPS
        }
        set(value) {
            RemotePlaySettingsPref.sharedPreferences.edit {
                putString(PreferenceConfiguration.FPS_PREF_STRING, value)
            }
        }




    override fun fallbackToDuplicateDisplay() {
        RemotePlaySettingsPref.isUseDefaultResolution = true
        prefs.edit {
            putString(RazerRemotePlaySettingsKey.PREF_DISPLAY_MODE, DisplayModeOption.DuplicateDisplay.displayModeName)
        }
    }

    override fun doVibrate(lowFreqMotor: Int, highFreqMotor: Int) = AIDLManager.doVibrate(lowFreqMotor, highFreqMotor)

    override fun isRumbleWithNexus(): CompletableFuture<Boolean> {
        return RnApp.globalScope.future {
            context.isRumbleWithNexus()
        }
    }

    /**
     * NEUR-59
     *
     *  Url encoding will be handled by the caller (this is because
     *  [okhttp3.HttpUrl.Builder.query] (used by [com.limelight.nvstream.http.NvHTTP])
     *  will canonicalize the query internally)
     */
    override fun getPairQueryParameters(): String {
        val deviceNickname = context.getDeviceNickName()
        val params = mutableListOf<Pair<String, Any?>>(
            "devicenickname" to deviceNickname
        )
        params.removeAll { it.second == null }

        // need to start and separate with '&'
        return params.joinToString("") { (k, v) ->
            "&$k=${v}"
        }
    }

    private fun DisplayModeOption.toQueryParamInt() = when(this) {
        DisplayModeOption.DuplicateDisplay -> 0
        DisplayModeOption.SeparateDisplay -> 1
        DisplayModeOption.PhoneOnlyDisplay -> 2
    }




    /**
     * See [NeuronBridgeInterface.updatePreferenceConfiguration]
     */
    override fun updatePreferenceConfiguration(window: Window, prefConfig: PreferenceConfiguration, intent : Intent) {
        val activeDisplayMode = intent.getStringExtra(Game.EXTRA_HOST_ACTIVE_DISPLAY_MODE)?.let { DisplayMode.createDisplayMode(it) }
        val displayMode = displayMode ?: return
        val tag = "updateResolutionAndRefreshRate"

        Timber.v("$tag: activeDisplayMode=$activeDisplayMode, displayMode=$displayMode")

        when (displayMode) {
            DisplayModeOption.PhoneOnlyDisplay,
            DisplayModeOption.SeparateDisplay -> {
                val virtualDisplayMode = calculateVirtualDisplayMode(context).getOrNull()
                Timber.v("$tag: virtualDisplayMode=$virtualDisplayMode")
                if (virtualDisplayMode != null) {
                    prefConfig.width = virtualDisplayMode.widthInt ?: prefConfig.width
                    prefConfig.height = virtualDisplayMode.heightInt ?: prefConfig.height
                    prefConfig.fps = virtualDisplayMode.refreshRateInt ?: prefConfig.fps
                }
            }
            DisplayModeOption.DuplicateDisplay -> {
                val defaultResolution = DEFAULT_LIST_RESOLUTION.toResolution()
                val defaultFps = DEFAULT_LIST_FPS.toIntOrNull()
                // for cases when we had to fallback to use default resolution
                if(RemotePlaySettingsPref.isUseDefaultResolution && defaultFps != null && defaultResolution != null) {
                    Timber.v("$tag: defaultResolution=$defaultResolution defaultFps=$defaultFps")
                    prefConfig.width = defaultResolution.width
                    prefConfig.height = defaultResolution.height
                    prefConfig.fps = defaultFps
                } else if(activeDisplayMode != null) {
                    // NEUR-103
                    val nativeRefreshRates = getAllSupportedNativeFps(window)
                    val closestMatchingRefreshRate = nativeRefreshRates.minByOrNull { abs(it - (activeDisplayMode.refreshRateInt ?: 0)) } ?: context.getDefaultDisplayRefreshRateHz(true)
                    val (w, h) = activeDisplayMode.widthInt to activeDisplayMode.heightInt
                    Timber.v("$tag: w=$w, h=$h nativeRefreshRates=$nativeRefreshRates, closestMatchingRefreshRate=$closestMatchingRefreshRate")
                    if (w != null && h != null) {
                        prefConfig.width = w
                        prefConfig.height = h
                        prefConfig.fps = closestMatchingRefreshRate
                    }
                }
            }
        }
        Timber.v("$tag: prefConfig.widthxheight=${prefConfig.width}x${prefConfig.height} fps=${prefConfig.fps}")
    }

    /**
     * See [NeuronBridgeInterface.updateStreamConfiguration]
     */
    override fun updateStreamConfiguration(connectionContext: ConnectionContext) = Unit

    /**
     * See [NeuronBridgeInterface.getLaunchUrlQueryParameters]
     * See NEUR-4
     * - Add virtualDisplay param (1 or 0)
     * - Add "clientNativeResolution" param (wxh)
     *
     *  Url encoding will be handled by the caller (this is because
     *  [okhttp3.HttpUrl.Builder.query] (used by [com.limelight.nvstream.http.NvHTTP])
     *  will canonicalize the query internally)
     */
    override fun getLaunchUrlQueryParameters(connectionContext: ConnectionContext): String {
        val computerDetails = connectionContext.computerDetails
        val tag = "getLaunchUrlQueryParameters"
        val nvApp = connectionContext.streamConfig.app
        val deviceNickname = context.getDeviceNickName()
        val virtualDisplayModeFormated = calculateVirtualDisplayMode(context).getOrNull()?.format()
        val ppi = context.getPPI().roundToInt()
        val screenSize = context.getScreenResolution()
        val _displayMode = displayMode ?: DisplayModeOption.DuplicateDisplay
        val getUIScale = ResolutionScale.getUIScale(context)

        Timber.v("$tag: _displayMode=$_displayMode, computerDetails.activeDisplayMode=${computerDetails?.activeDisplayMode}, $deviceNickname, $virtualDisplayModeFormated")
        val params = mutableListOf<Pair<String, Any?>>(
            "virtualDisplay" to _displayMode.toQueryParamInt(),
            "virtualDisplayMode" to if(_displayMode.isUsesVirtualDisplay) virtualDisplayModeFormated else null,
            "devicenickname" to deviceNickname,
            "ppi" to ppi,
            "screen_resolution" to "${screenSize.x}x${screenSize.y}",
            "timeToTerminateApp" to if(nvApp?.isDesktop() == true) 0 else RemotePlaySettingsPref.autoCloseGameCountDown,
            "UIScale" to getUIScale.toString()
        )
        params.removeAll { it.second == null }

        // need to start and separate with '&'
        return params.joinToString("") { (k, v) ->
            "&$k=${v}"
        }
    }

    /**
     * BAA-2249
     */
    override fun updateVideoStats(
        randomId: String,
        decoder: String,
        initialSize: Size,
        avgNetLatency: Int,
        avgNetLatencyVarianceMs: Int,
        globalVideoStats: VideoStats?,
        lastWindowVideoStats: VideoStats?,
        activeWindowVideoStats: VideoStats?
    ) {
        val lastSession = SessionStats.lastSession?.takeIf { it.randomId == randomId }
            ?: (SessionStats(randomId = randomId).also { SessionStats.lastSession = it })
        lastSession.update(
            decoder,
            initialSize,
            avgNetLatency,
            avgNetLatencyVarianceMs,
            globalVideoStats,
            lastWindowVideoStats,
            activeWindowVideoStats
        )
    }


    override fun stringifyPortFlags(portFlags: Int) = stringifyPortFlags0(portFlags)

    /**
     * @param portFlags ignored in this implementation, it just lists all the ports that needs to be opened
     */
    private fun stringifyPortFlags0(portFlags: Int): String {
        fun Int.getPortFromPortFlagIndex() = RemotePlayConfig.default.portsMap[this] ?: 0
        val lines = mutableListOf<String>()
        lines += "TCP ${RemotePlayConfig.ML_PORT_INDEX_TCP_47984.getPortFromPortFlagIndex()}"
        lines += "TCP ${RemotePlayConfig.ML_PORT_INDEX_TCP_47989.getPortFromPortFlagIndex()}"
        lines += "TCP ${RemotePlayConfig.ML_PORT_INDEX_TCP_48010.getPortFromPortFlagIndex()}"
        lines += "UDP ${RemotePlayConfig.ML_PORT_INDEX_UDP_47998.getPortFromPortFlagIndex()} ~ ${RemotePlayConfig.ML_PORT_INDEX_UDP_48010.getPortFromPortFlagIndex()}"
        return (lines.joinToString("\n").trim())
    }

    /**
     *  @param portFlags (e.g. 1280 should return "UDP 51346\nUDP 51348")
     */
    private fun stringifyPortFlags1(portFlags: Int): String? {
        fun Int.getPortFromPortFlagIndex() = RemotePlayConfig.default.portsMap[this] ?: 0
        fun Int.getProtocolFromPortFlagIndex() = if(this >= 8) "UDP" else "TCP"
        val lines = mutableListOf<String>()
        Timber.v("stringifyPortFlags: portFlags${portFlags} (${portFlags.toString(2)})")
        for(i in 0 until 32) {
            val shifted = (portFlags and (1 shl i))
            if(shifted != 0) {
                lines += "${i.getProtocolFromPortFlagIndex()} ${i.getPortFromPortFlagIndex()}"
            }
        }
        return (lines.joinToString("\n").trim())
    }


    override fun logAndRecordException(t: Throwable) {
        com.razer.neuron.common.logAndRecordException(t)
    }



    override fun invertMotors(freqArray: IntArray) {
        require(freqArray.size == 2) { "Array size must be 2" }
        val temp = freqArray[0]
        freqArray[0] = freqArray[1]
        freqArray[1] = temp

    }


    override fun onStartNeuronGame() {
        AIDLManager.onStartNeuronGame()
    }

    override fun onStopNeuronGame() {
        AIDLManager.onStopNeuronGame()
    }

    override fun getLocalizedStageName(stage: String): String {
        return with(context) {
            when (stage) {
                "none" -> getString(R.string.conn_stage_none)
                "platform initialization" -> getString(R.string.conn_stage_platform_initialization)
                "name resolution" -> getString(R.string.conn_stage_name_resolution)
                "audio stream initialization" -> getString(R.string.conn_stage_audio_stream_initialization)
                "RTSP handshake" -> getString(R.string.conn_stage_RTSP_handshake)
                "control stream initialization" -> getString(R.string.conn_stage_control_stream_initialization)
                "video stream initialization" -> getString(R.string.conn_stage_video_stream_initialization)
                "input stream initialization" -> getString(R.string.conn_stage_input_stream_initialization)
                "control stream establishment" -> getString(R.string.conn_stage_control_stream_establishment)
                "video stream establishment" -> getString(R.string.conn_stage_video_stream_establishment)
                "audio stream establishment" -> getString(R.string.conn_stage_audio_stream_establishment)
                "input stream establishment" -> getString(R.string.conn_stage_input_stream_establishment)
                else -> stage
            }
        }
    }

    override fun getLocalizedStringFromErrorCode(errorCode: Int) = when (errorCode) {
        5031 -> context.getString(R.string.conn_error_code_5031)
        5032 -> context.getString(R.string.conn_error_code_5032)
        5033 -> context.getString(R.string.conn_error_code_5033)
        5034 -> context.getString(R.string.conn_error_code_5034)
        else -> "(error $this)"
    }
}



