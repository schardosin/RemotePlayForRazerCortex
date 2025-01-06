package com.razer.neuron.provider.sources

import com.razer.neuron.pref.RemotePlaySettingsPref
import com.razer.neuron.pref.RemotePlaySettingsPref.isReadOnlyKey
import com.razer.neuron.provider.KrateContentSource

/**
 * [com.razer.neuron.provider.NeuronContentProvider] for [RemotePlaySettingsPref]
 */
class RemotePlaySettingsProviderSource : KrateContentSource() {
    override val krate = RemotePlaySettingsPref
    override val path: String
        get() = RemotePlaySettingsPref.REMOTE_PLAY_SETTINGS_NAME

    override fun isReadOnly(name: String) = name.isReadOnlyKey()
}
