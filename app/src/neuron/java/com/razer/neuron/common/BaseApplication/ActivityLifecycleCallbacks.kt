package com.razer.neuron.common.BaseApplication

import android.app.Activity
import android.app.Application
import android.os.Bundle

open class BaseActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {

    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityCreated(p0: Activity, p1: Bundle?) = Unit

    override fun onActivityStarted(p0: Activity) = Unit

    override fun onActivityResumed(p0: Activity) = Unit

    override fun onActivityPaused(p0: Activity) = Unit

    override fun onActivityStopped(p0: Activity) = Unit

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) = Unit

    override fun onActivityDestroyed(p0: Activity) = Unit
}