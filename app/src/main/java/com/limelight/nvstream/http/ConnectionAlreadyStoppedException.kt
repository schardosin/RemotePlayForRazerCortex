package com.limelight.nvstream.http;

import java.io.IOException

open class ConnectionAlreadyStoppedException(val msg: String, cause: Throwable? = null) : IOException(msg, cause)
