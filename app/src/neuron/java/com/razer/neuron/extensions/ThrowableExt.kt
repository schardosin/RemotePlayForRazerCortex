package com.razer.neuron.extensions

import com.limelight.R
import com.limelight.nvstream.http.ConnectionAlreadyStoppedException
import com.limelight.nvstream.http.HostHttpResponseException
import java.io.FileNotFoundException
import java.io.IOException
import java.net.UnknownHostException

fun Throwable.getUserFacingMessage(defaultMessage : String = getStringExt(R.string.error_lost_connection)): String {
    return when (this) {
        is UnknownHostException -> getStringExt(R.string.error_unknown_host)
        is FileNotFoundException -> getStringExt(R.string.error_404)
        is ConnectionAlreadyStoppedException -> getStringExt(R.string.conn_terminated_title)
        is HostHttpResponseException -> getStringExt(R.string.conn_error_title)
        is IOException -> getStringExt(R.string.error_lost_connection)
        else -> this.message ?: defaultMessage
    }
}