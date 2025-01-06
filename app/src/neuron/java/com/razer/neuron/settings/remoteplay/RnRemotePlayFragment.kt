package com.razer.neuron.settings.remoteplay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.limelight.R
import com.limelight.databinding.RnFragmentRemotePlayBinding
import com.razer.neuron.extensions.getDisplayCutout
import com.razer.neuron.extensions.gone
import com.razer.neuron.extensions.isHdrSupported
import com.razer.neuron.extensions.setNumberDropDown
import com.razer.neuron.extensions.visible
import com.razer.neuron.extensions.visibleIf
import com.razer.neuron.model.DisplayModeOption
import com.razer.neuron.model.FramePacingOption
import com.razer.neuron.model.TouchScreenOption
import com.razer.neuron.pref.RemotePlaySettingsPref
import com.razer.neuron.utils.calculateVirtualDisplayMode
import com.razer.neuron.utils.getDefaultDisplayRefreshRateHz
import com.razer.neuron.utils.getStringExt
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.Locale
import kotlin.math.roundToInt


@AndroidEntryPoint
class RnRemotePlayFragment : Fragment() {

    private lateinit var binding: RnFragmentRemotePlayBinding

    private val viewModel: RnRemotePlayViewModel by activityViewModels()

    private val imageRadioButtonsMap
        get() = mapOf(
            DisplayModeOption.DuplicateDisplay to binding.irbDuplicateScreen,
            DisplayModeOption.SeparateDisplay to binding.irbSeparateScreen,
            DisplayModeOption.PhoneOnlyDisplay to binding.irbPhoneOnly
        )

    private val onDisplayModeChangedListener =
        CompoundButton.OnCheckedChangeListener { button, isChecked ->
            Timber.v("OnCheckedChangeListener: $button, isChecked=${isChecked}")
            if (isChecked) {
                val checkedDisplayOption =
                    imageRadioButtonsMap.entries.firstOrNull { (_, radioButton) -> radioButton.radioBtn == button }?.key
                Timber.v("OnCheckedChangeListener: ${checkedDisplayOption?.displayModeName}, isChecked=${isChecked}")
                if (checkedDisplayOption != null) {
                    viewModel.setDisplayMode(checkedDisplayOption)
                }
            }
        }

    private val framePacingOptionsMap
        get() = mapOf(
            FramePacingOption.LowLatency to binding.lowLatencyOption,
            FramePacingOption.Balanced to binding.balancedOption,
            FramePacingOption.SmoothestVideo to binding.smoothestOption
        )

    private val onFramePacingChangedListener =
        CompoundButton.OnCheckedChangeListener { button, isChecked ->
            Timber.v("OnCheckedChangeListener: $button, isChecked=${isChecked}")
            if (isChecked) {
                val framePacingOption =
                    framePacingOptionsMap.entries.firstOrNull { (_, radioButton) -> radioButton.radioBtn == button }?.key
                Timber.v("OnCheckedChangeListener: ${framePacingOption?.key}, isChecked=${isChecked}")
                if (framePacingOption != null) {
                    viewModel.setFramePacing(framePacingOption)
                }
            }
        }

    private val touchScreenOptionsMap
        get() = mapOf(
            TouchScreenOption.DirectTouch to binding.directTouchOption,
            TouchScreenOption.VirtualTrackpad to binding.virtualTrackpadOption
        )

    private val onTouchScreenChangedListener =
        CompoundButton.OnCheckedChangeListener { button, isChecked ->
            Timber.v("OnCheckedChangeListener: $button, isChecked=${isChecked}")
            if (isChecked) {
                val touchScreenOption =
                    touchScreenOptionsMap.entries.firstOrNull { (_, radioButton) -> radioButton.radioBtn == button }?.key
                Timber.v("OnCheckedChangeListener: ${touchScreenOption}, isChecked=${isChecked}")
                if (touchScreenOption != null) {
                    viewModel.setTouchScreen(touchScreenOption)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = RnFragmentRemotePlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDisplayMode()
        setupCropToSafeArea()
        setupLimitRefreshRate()
        setupVideoBitrate()
        setupFramePacing()
        setupHDR()
        setupMuteHostPc()
        setupTouchScreen()
        setupAutoCloseGameCountDown()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }


    private fun DisplayModeOption.getDisplaySubtitleText() : String {
        val virtualDisplayMode = calculateVirtualDisplayMode(requireContext(), isLimitRefreshRate = RemotePlaySettingsPref.isLimitRefreshRate).getOrNull()
        val virtualResolution = virtualDisplayMode?.let { "${it.width}x${it.height}" } ?: ""
        val virtualRefreshRate = virtualDisplayMode?.refreshRate ?: ""
        return when(this) {
            DisplayModeOption.DuplicateDisplay -> getString(R.string.rn_settings_display_mode_duplicate_desc)
            DisplayModeOption.PhoneOnlyDisplay -> getString(R.string.rn_settings_display_mode_phone_only_desc_x_resolution_x_refresh_rate, virtualResolution, virtualRefreshRate)
            DisplayModeOption.SeparateDisplay -> getString(R.string.rn_settings_display_mode_separate_desc_x_resolution_x_refresh_rate, virtualResolution, virtualRefreshRate)
        }
    }

    private fun setupDisplayMode() {
        viewModel.displayModeLiveData.observe(viewLifecycleOwner) { displayMode ->
            binding.irbDuplicateScreen.update(
                title = getStringExt(R.string.rn_settings_display_mode_duplicate),
                subtitle = null,
                iconRes = R.drawable.ic_display_mode_duplicate,
                isChecked = displayMode == DisplayModeOption.DuplicateDisplay
            )
            binding.irbPhoneOnly.update(
                title = getStringExt(R.string.rn_settings_display_mode_phone_only_2_lines),
                subtitle = null,
                iconRes = R.drawable.ic_display_mode_phone_only,
                isChecked = displayMode == DisplayModeOption.PhoneOnlyDisplay
            )
            binding.irbSeparateScreen.update(
                title = getStringExt(R.string.rn_settings_display_mode_separate),
                subtitle = null,
                iconRes = R.drawable.ic_display_mode_separate,
                isChecked = displayMode == DisplayModeOption.SeparateDisplay
            )
            updateHDRSwitch(displayMode)
            updateMuteSpeakersSwitch(displayMode)
            updateCropToSafeArea(displayMode)
            binding.tvDisplayModeDescription.text = displayMode.getDisplaySubtitleText()

        }
        imageRadioButtonsMap.values.forEach { imageRadioButtons ->
            imageRadioButtons.onCheckedChangeListener = onDisplayModeChangedListener
        }
        binding.irbSeparateScreen.visibleIf {
            RemotePlaySettingsPref.isSeparateScreenDisplayEnabled
        }
    }


    private fun updateCropToSafeArea(selected : DisplayModeOption?) {
        val context = context ?: return
        val window = activity?.window ?: return
        binding.rowLimitToSafeArea.root.visibleIf { context.getDisplayCutout(window).isNotEmpty() && selected == DisplayModeOption.PhoneOnlyDisplay }
        binding.rowStretchToFullScreen.root.visibleIf { context.getDisplayCutout(window).isNotEmpty() && selected == DisplayModeOption.DuplicateDisplay }
    }

    private fun setupCropToSafeArea() {
        with(binding.rowLimitToSafeArea) {
            tvTitle.text = getString(R.string.rn_setting_neuron_limit_to_safe_area)
            tvSubtitle.text = getString(R.string.rn_setting_neuron_limit_to_safe_area_desc)
            switchLayout.visible()
            ivActionIcon.gone()
            root.setOnClickListener {
                switchAction.toggle()
                viewModel.setCropDisplaySafeArea(switchAction.isChecked)
            }
            switchAction.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setCropDisplaySafeArea(isChecked)
            }
        }

        with(binding.rowStretchToFullScreen) {
            tvTitle.text = getString(R.string.rn_setting_neuron_stretch_to_full_screen)
            tvSubtitle.text = getString(R.string.rn_setting_neuron_stretch_to_full_screen_desc)
            switchLayout.visible()
            ivActionIcon.gone()
            // we use ! here because the function setCropDisplaySafeArea is opposite
            // of the switch's text
            root.setOnClickListener {
                switchAction.toggle()
                viewModel.setCropDisplaySafeArea(!switchAction.isChecked)
            }
            switchAction.setOnCheckedChangeListener { _, isChecked ->
                // we use ! here because the function setCropDisplaySafeArea is opposite
                // of the switch's text
                viewModel.setCropDisplaySafeArea(!isChecked)
            }
        }

        viewModel.cropDisplaySafeAreaLiveData.observe(viewLifecycleOwner) { isChecked ->
            with(binding.rowLimitToSafeArea) {
                switchAction.isChecked = isChecked
            }
            with(binding.rowStretchToFullScreen) {
                // we use ! here because the function setCropDisplaySafeArea is opposite
                // of the switch's text
                switchAction.isChecked = !isChecked
            }
        }
    }

    private fun setupLimitRefreshRate() {
        //BAA-2345
        binding.rowLimitRefreshRate.root.visibleIf { (context?.getDefaultDisplayRefreshRateHz() ?: 0) > 60f }
        with(binding.rowLimitRefreshRate) {
            tvTitle.text = getStringExt(R.string.rn_setting_neuron_limit_refresh_title)
            tvSubtitle.gone()
            switchLayout.visible()
            ivActionIcon.gone()
            root.setOnClickListener {
                switchAction.toggle()
                viewModel.setLimitRefreshRate(switchAction.isChecked)
            }
            switchAction.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setLimitRefreshRate(isChecked)
            }
        }

        viewModel.limitRefreshRateLiveData.observe(viewLifecycleOwner) { isChecked ->
            with(binding.rowLimitRefreshRate) {
                switchAction.isChecked = isChecked
            }
        }
    }

    private fun setupVideoBitrate() {
        val levels = 100f
        fun BitrateRateSettings.range() = max - min
        fun BitrateRateSettings.valueToBitrate(value: Float) = ((range() * (value / levels)) + min).roundToInt()
        fun BitrateRateSettings.valueToText(value: Float) = valueToBitrate(value).toBitrateString()
        fun BitrateRateSettings.bitrateToValue() = (((rate.toFloat()) - min)/range())*levels
        var currentSettings : BitrateRateSettings? = null
        viewModel.bitrateSettingsLiveData.observe(viewLifecycleOwner) { bitRateSettings ->
            currentSettings = bitRateSettings
            binding.bitrateSlider.valueFrom = 0f
            binding.bitrateSlider.valueTo = levels
            binding.bitrateSlider.value = bitRateSettings.bitrateToValue()
            binding.bitrateSlider.setLabelFormatter { value ->
                bitRateSettings.valueToText(value)
            }
            binding.tvBitrateValue.text = bitRateSettings.valueToText(bitRateSettings.bitrateToValue())
        }
        binding.bitrateSlider.addOnChangeListener {
                _ ,value, fromUser ->
            if (fromUser) {
                currentSettings?.let {
                    viewModel.setBitrateSettings(it.valueToBitrate(value))
                    binding.tvBitrateValue.text = it.valueToText(value)
                }
            }
        }
    }

    private fun setupFramePacing() {
        viewModel.framePacingLiveData.observe(viewLifecycleOwner) { framePacing ->
            binding.lowLatencyOption.update(
                title = getStringExt(R.string.rn_setting_neuron_low_latency),
                isChecked = framePacing == FramePacingOption.LowLatency
            )
            binding.balancedOption.update(
                title = getStringExt(R.string.rn_setting_neuron_balanced),
                isChecked = framePacing == FramePacingOption.Balanced
            )
            binding.smoothestOption.update(
                title = getStringExt(R.string.rn_setting_neuron_smoothest_video),
                isChecked = framePacing == FramePacingOption.SmoothestVideo
            )
        }
        framePacingOptionsMap.values.forEach { option ->
            option.onCheckedChangeListener = onFramePacingChangedListener
        }
    }

    private fun updateHDRSwitch(displayModeOption: DisplayModeOption? = null) {
        //BAA-2345
        binding.rowHdr.root.visibleIf { (context?.isHdrSupported() ?: false) && displayModeOption == DisplayModeOption.DuplicateDisplay }
    }

    private fun setupHDR() {
        with(binding.rowHdr) {
            tvTitle.text = getStringExt(R.string.rn_setting_neuron_hdr_title)
            tvSubtitle.text = getStringExt(R.string.rn_setting_neuron_hdr_subtitle)
            switchLayout.visible()
            ivActionIcon.gone()
            root.setOnClickListener {
                switchAction.toggle()
                viewModel.setEnableHdr(switchAction.isChecked)
            }
            switchAction.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setEnableHdr(isChecked)
            }
        }

        viewModel.hdrLiveData.observe(viewLifecycleOwner) { isChecked ->
            with(binding.rowHdr) {
                switchAction.isChecked = isChecked
            }
        }
    }

    private fun updateMuteSpeakersSwitch(displayModeOption: DisplayModeOption? = null) {
        // disable since BAA-2345
        binding.rowMuteHostPc.root.visibleIf { displayModeOption == DisplayModeOption.DuplicateDisplay }
    }

    private fun setupMuteHostPc() {
        with(binding.rowMuteHostPc) {
            tvTitle.text = getStringExt(R.string.rn_setting_neuron_mute_host_pc_title)
            tvSubtitle.gone()
            switchLayout.visible()
            ivActionIcon.gone()
            root.setOnClickListener {
                switchAction.toggle()
                viewModel.setMuteHostPc(switchAction.isChecked)
            }
            switchAction.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setMuteHostPc(isChecked)
            }
        }
        viewModel.muteHostPcLiveData.observe(viewLifecycleOwner) { isChecked ->
            with(binding.rowMuteHostPc) {
                switchAction.isChecked = isChecked
            }
        }
    }

    private fun setupTouchScreen() {
        viewModel.touchScreenOptionLiveData.observe(viewLifecycleOwner) { touchScreen ->
            binding.virtualTrackpadOption.update(
                title = getStringExt(R.string.rn_setting_neuron_virtual_trackpad),
                isChecked = touchScreen == TouchScreenOption.VirtualTrackpad
            )
            binding.directTouchOption.update(
                title = getStringExt(R.string.rn_setting_neuron_direct_touch),
                isChecked = touchScreen == TouchScreenOption.DirectTouch
            )
        }
        touchScreenOptionsMap.values.forEach { option ->
            option.onCheckedChangeListener = onTouchScreenChangedListener
        }
    }

    private fun Int.toBitrateString(): String {
        return "%.1f".format(toFloat() / 1000, Locale.ENGLISH) + " Mbps"
    }

    private fun setupAutoCloseGameCountDown() {
        with(binding.rowAutoCloseGameCountdownDropdown) {
            tvTitle.text = getString(R.string.rn_setting_neuron_autoclose_countdown_title)
            tvSubtitle.text = getString(R.string.rn_setting_neuron_autoclose_countdown_subtitle)
            ivActionIcon.gone()
            layoutDropdownMenu.visible()
            val actv = actvDropdownMenu as? MaterialAutoCompleteTextView ?: return@with
            val setter = actv.setNumberDropDown(
                names = resources.getStringArray(R.array.auto_close_games_names),
                values = resources.getIntArray(R.array.auto_close_games_values).toTypedArray(),
                initialValue = viewModel.autoCloseGameCountDownLiveData.value ?: error("Not init"),
            ) {
                value ->
                viewModel.setAutoCloseGameCountDown(value)
            }

            viewModel.autoCloseGameCountDownLiveData.observe(viewLifecycleOwner) {
                value ->
                setter(value)
            }
        }
    }

}
