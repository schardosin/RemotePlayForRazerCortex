package com.razer.neuron.common

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatDelegate
import com.razer.neuron.common.BaseApplication.BaseActivityLifecycleCallbacks
import com.razer.neuron.extensions.hideNavigationBars
import com.razer.neuron.main.RnMainActivity
import com.razer.neuron.model.AppThemeType
import com.razer.neuron.oobe.RnOobeActivity
import com.razer.neuron.pref.RemotePlaySettingsPref
import com.razer.neuron.settings.RnSettingsActivity
import com.razer.neuron.utils.API_LEVEL28
import com.razer.neuron.utils.API_LEVEL29
import com.razer.neuron.utils.API_LEVEL30
import com.razer.neuron.utils.isAboveOrEqual
import com.razer.neuron.utils.isBelowOrEqual


object RnAppThemeHelper {
    /**
     * Call when [application] was created
     */
    fun onApplicationCreated(application: Application) {
        application.initActivityLifeCycleCallback()
        /**
         * [BaseActivityLifecycleCallbacks.onActivityPreCreated] will not be called for API 28
         * but API 28 doesn't have dynamic color anyway, which means it will use razer color
         * so we should just call [applyLightDarkMode]
         */
        if(isBelowOrEqual(API_LEVEL28)){
            AppThemeType.default().applyLightDarkMode()
        }
    }

    private fun appThemeType() = RemotePlaySettingsPref.appThemeType

    private fun Application.initActivityLifeCycleCallback() {
        registerActivityLifecycleCallbacks(object : BaseActivityLifecycleCallbacks() {
            /**
             * Note, only added on API 29+
             */
            override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
                if(isAboveOrEqual(API_LEVEL29)) {
                    appThemeType().apply(activity)
                }
            }

            override fun onActivityResumed(p0: Activity) {
                val appThemeType = appThemeType()
                appThemeType.applyLightDarkMode()
                p0.setSystemBarIconColor(white = appThemeType.isUseWhiteStatusIcons(p0))
                when (p0) {
                    is RnSettingsActivity -> {
                        p0.window.navigationBarColor = colorSurface
                    }
                    is RnMainActivity, is RnOobeActivity -> {
                        p0.hideNavigationBars()
                    }
                    else -> {
                        // rest of the activities just leave it as they are
                        Unit
                    }
                }
            }
        })
    }
}


/**
 * This only set the color of status bar an navigation bar icons
 *
 * For status bar color (should be transparent, it is set by style android:statusBarColor)
 * For navigation bar color (it should be set by [android.view.Window.setNavigationBarColor])
 */
fun Activity.setSystemBarIconColor(white : Boolean) {
    val window = window
    if(isAboveOrEqual(API_LEVEL30)) {
        val windowInsetsController = window.insetsController ?: return
        val flags = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        windowInsetsController.setSystemBarsAppearance(0,
            flags)
        if(!white) {
            windowInsetsController.setSystemBarsAppearance(flags, flags)
        }
    } else {
        // no way to do this in API 28 and 29 during runtime
    }
}

/**
 * This is to know if the OS has been set to use dark mode resources
 */
fun Context.isSystemInDarkMode(): Boolean {
    val currentNightMode = applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return currentNightMode == Configuration.UI_MODE_NIGHT_YES
}

/**
 * This is to know if the [Activity] has been set to use dark mode resources
 * e.g
 * was it customized with [AppCompatDelegate.setDefaultNightMode]
 */
fun Activity.isActivityInDarkMode(): Boolean {
    val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return currentNightMode == Configuration.UI_MODE_NIGHT_YES
}



