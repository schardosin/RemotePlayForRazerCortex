package com.razer.neuron.extensions

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.razer.neuron.utils.API_LEVEL33
import com.razer.neuron.utils.isAboveOrEqual


/**
 * No logging. It gets spammy
 */
fun PackageManager.getPackageInfoInternal(packageName: String): PackageInfo? {
    return try {
        this.getPackageInfoExt(packageName, 0)
    } catch (t: Throwable) {
        null
    }
}

fun PackageManager.queryIntentActivitiesExt(intent: Intent, flags: Int): List<ResolveInfo> {
    if (isAboveOrEqual(API_LEVEL33)) {
        return queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION") return  queryIntentActivities(intent, flags)
    }
}

fun PackageManager.getApplicationInfoExt(packageName: String, flags: Int): ApplicationInfo {
    if (isAboveOrEqual(API_LEVEL33)) {
        return getApplicationInfo(
            packageName,
            PackageManager.ApplicationInfoFlags.of(flags.toLong())
        )
    } else {
        @Suppress("DEPRECATION") return getApplicationInfo(packageName, flags)
    }
}

fun PackageManager.resolveActivityExt(intent: Intent, flags: Int): ResolveInfo? {
    if (isAboveOrEqual(API_LEVEL33)) {
        return resolveActivity(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION") return resolveActivity(intent, flags)
    }
}

fun PackageManager.getPackageInfoExt(packageName: String, flags: Int): PackageInfo? {
    if (isAboveOrEqual(API_LEVEL33)) {
        return getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION") return getPackageInfo(packageName, flags)
    }
}
