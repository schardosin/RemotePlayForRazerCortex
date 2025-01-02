package com.razer.neuron.game.helpers

import android.content.res.Resources
import com.limelight.Game
import com.limelight.NeuronBridge
import com.limelight.R
import com.razer.neuron.game.RnGame
import com.limelight.nvstream.jni.MoonBridge
import com.limelight.utils.ServerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A collection of functions for [RnGame]
 */

data class ConnectionError(val title: String? = null, val message: String? = null)

/**
 * This was translated from [Game.stageFailed]
 */
suspend fun Resources.getConnectionErrors(
    stage: String,
    portFlags: Int,
    errorCode: Int
): List<ConnectionError> {

    // Perform a connection test if the failure could be due to a blocked port
    // This does network I/O, so don't do it on the main thread.
    val portTestResult = withContext(Dispatchers.IO) {
        MoonBridge.testClientConnectivity(ServerHelper.CONNECTION_TEST_SERVER, 443, portFlags)
    }

    val errors = mutableListOf<ConnectionError>()
    // If video initialization failed and the surface is still valid, display extra information for the user
    if (stage.contains("video")) {
        errors.add(ConnectionError(message = getString(R.string.video_decoder_init_failed)))
    }

    var dialogText = "${getString(R.string.conn_error_msg)} $stage (error $errorCode)"
    if (portFlags != 0) {
        dialogText += "\n\n${getString(R.string.check_ports_msg)}\n${
            NeuronBridge.stringifyPortFlags(portFlags) ?: MoonBridge.stringifyPortFlags(
                portFlags,
                "\n"
            )
        }"
    }

    if (portTestResult != MoonBridge.ML_TEST_RESULT_INCONCLUSIVE && portTestResult != 0) {
        dialogText += "\n\n${getString(R.string.nettest_text_blocked)}"
    }

    errors.add(ConnectionError(title = getString(R.string.conn_error_title), message = dialogText))
    return errors
}