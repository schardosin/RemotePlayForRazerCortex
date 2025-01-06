package com.razer.neuron.model

import com.limelight.R
import com.razer.neuron.utils.getStringExt

enum class AppAction {
    Back, Accept, Skip, Download, Allow, Done, Continue, Pair, Unpair, StartPlay, Settings, ManuallyPair, Retry
}

fun AppAction.toButtonText() = when (this) {
    AppAction.Download -> getStringExt(R.string.rn_download)
    AppAction.Done -> getStringExt(R.string.rn_done)
    AppAction.Allow -> getStringExt(R.string.rn_allow)
    AppAction.Skip -> getStringExt(R.string.rn_skip)
    AppAction.Continue -> getStringExt(R.string.rn_btn_continue)
    AppAction.Back -> getStringExt(R.string.rn_back)
    AppAction.Accept -> getStringExt(R.string.rn_accept)
    AppAction.Pair -> getStringExt(R.string.rn_pair)
    AppAction.Unpair -> getStringExt(R.string.rn_unpair)
    AppAction.StartPlay -> getStringExt(R.string.rn_start_play)
    AppAction.Settings -> getStringExt(R.string.rn_settings)
    AppAction.ManuallyPair -> getStringExt(R.string.rn_manually_pair)
    AppAction.Retry -> getStringExt(R.string.rn_retry)
}