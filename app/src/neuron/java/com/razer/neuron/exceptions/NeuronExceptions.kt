package com.razer.neuron.exceptions

open class RnException(val msg: String, cause: Throwable? = null) : Exception(msg, cause)

open class RnAddHostException(msg: String, cause: Throwable? = null) : RnException(msg, cause)

open class NeuronPairingException(msg: String, cause: Throwable? = null) : RnException(msg, cause)

open class NeuronStreamException(msg: String, cause: Throwable? = null) : RnException(msg, cause)