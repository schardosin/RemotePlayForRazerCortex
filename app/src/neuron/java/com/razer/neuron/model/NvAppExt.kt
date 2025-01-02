package com.razer.neuron.model

import com.limelight.nvstream.http.NvApp

fun NvApp.isDesktop() = this.appName?.equals("Desktop") == true