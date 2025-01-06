package com.razer.neuron.settings

import com.google.android.material.internal.NavigationMenuItemView
import com.google.android.material.navigation.NavigationView

/**
 * See BAA-2220
 */
enum class NavigationBehavior {
    /**
     * Call [RnSettingsViewModel.navigationTo] when [NavigationMenuItemView] is focused
     */
    NavigateOnFocus,

    /**
     * Call [RnSettingsViewModel.navigationTo] when
     * [NavigationView.OnNavigationItemSelectedListener.onNavigationItemSelected] is called
     */
    NavigateOnSelect;

    companion object {
        val default = NavigateOnFocus
    }
}