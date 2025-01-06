package com.razer.neuron.utils

import android.app.Activity
import android.content.Intent
import android.os.Bundle


fun Activity.launch(c: Class<*>, bundle: Bundle? = null) {
    val intent = Intent(this, c)
    bundle?.let { intent.putExtras(it) }
    startActivity(intent)
}