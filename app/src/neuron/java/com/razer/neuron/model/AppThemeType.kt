package com.razer.neuron.model

import android.app.Activity
import android.os.Build
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.NightMode
import com.google.android.material.color.DynamicColors
import com.limelight.R
import timber.log.Timber
import com.razer.neuron.common.isActivityInDarkMode
import com.razer.neuron.game.RnGame

import com.razer.neuron.utils.API_LEVEL31
import com.razer.neuron.utils.isAboveOrEqual

enum class AppThemeType(
    @StringRes
    val title: Int,
    /**
     * true if this [AppThemeType] is meant to use dynamic color (from system)
     */
    val isUseDynamicColors: Boolean,
    /**
     * For status bar icon color
     */
    @NightMode
    val mode: Int
) {
    RazerColor(R.string.rn_theme_razer, false, AppCompatDelegate.MODE_NIGHT_YES),
    System(R.string.rn_theme_system, true, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    Light(R.string.rn_theme_light, true, AppCompatDelegate.MODE_NIGHT_NO),
    Dark(R.string.rn_theme_dark, true, AppCompatDelegate.MODE_NIGHT_YES);

    companion object {

        /**
         * Get a [AppThemeType] where [apply] will work
         * NEUR-106
         */
        fun default() = RazerColor

        /**
         * Use this instead of [DynamicColors.isDynamicColorAvailable]
         */
        fun isDynamicColorAvailable2() =
            DynamicColors.isDynamicColorAvailable() || isDynamicColorAvailableUnofficially()

        /**
         * See BAA-2200, add more if needed
         */
        private val supportedBrands = setOf("Razer").map { it.lowercase() }

        /**
         * See BAA-2200, add more if needed
         */
        private val supportedManufacturers = setOf("Razer").map { it.lowercase() }

        /**
         * See BAA-2200
         *
         * Check OS version and check [supportedBrands] and [supportedManufacturers]
         */
        private fun isDynamicColorAvailableUnofficially() = isAboveOrEqual(API_LEVEL31) && (
                supportedBrands.contains(Build.BRAND.lowercase())
                        || supportedManufacturers.contains(Build.MANUFACTURER.lowercase()))

    }

    /**
     * true if this [AppThemeType] should use white status icon
     */
    fun isUseWhiteStatusIcons(activity : Activity) =
        (mode == AppCompatDelegate.MODE_NIGHT_YES) || (this == System && activity.isActivityInDarkMode())

    /**
     * As per Greg's requirement, if [DynamicColors] was not applied (and we
     * are forced to use Razer color) then we force it to night mode
     */
    fun apply(activity: Activity) {
        val finalTheme = if (isUseDynamicColors) {
            if (DynamicColors.isDynamicColorAvailable()) {
                DynamicColors.applyToActivityIfAvailable(activity)
                this
            } else if (isDynamicColorAvailableUnofficially()) {
                /**
                 * See BAA-2200 on why [isDynamicColorAvailableUnofficially] is needed
                 *
                 * e.g.
                 * Razer edge has partial support. Even though [DynamicColors.isDynamicColorAvailable]
                 * is false, it can still support it if we force it.
                 */
                runCatching {
                    with(activity) {
                        setTheme(
                            when (this) {
                                /**
                                 * [RnGame] needs a special theme with black background
                                 * see [R.style.RnTheme_Game_Base], so if we are overriding
                                 * the theme, we need to make sure we use one that has black
                                 * bg also.
                                 */
                                is RnGame -> R.style.AppTheme_DynamicColors_WindowBackground_Black
                                else -> R.style.AppTheme_DynamicColors
                            }
                        )
                    }
                }
                this
            } else {
                // cannot apply dynamic, just use default
                default()
            }
        } else {
            this
        }
        finalTheme.applyLightDarkMode()
        Timber.v("$name.apply(${activity.javaClass.simpleName}): finalTheme=${finalTheme.name} MODEL=${Build.MODEL},MANUFACTURER=${Build.MANUFACTURER},BRAND=${Build.BRAND}  DynamicColors.isDynamicColorAvailable=${DynamicColors.isDynamicColorAvailable()}")
    }


    fun applyLightDarkMode() {
        AppCompatDelegate.setDefaultNightMode(mode)
        Timber.v("$name.applyLightDarkMode")
    }
}



