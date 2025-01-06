package com.razer.neuron.model


import com.google.gson.annotations.SerializedName
import com.limelight.nvstream.http.ComputerDetails
import com.limelight.nvstream.http.ComputerDetails.AddressTuple

/**
 * See Neuron
 */
data class RnAddressTuple(
    @SerializedName("address")
    val address: String,
    @SerializedName("port")
    val port: Int
) {
    constructor(addressTuple: AddressTuple) : this(addressTuple.address, addressTuple.port)
    fun toAddressTuple() = AddressTuple(address, port)
}

/**
 * LOCAL = "local";
 * REMOTE = "remote";
 * MANUAL = "manual";
 * IPv6 = "ipv6";
 * ADDRESS = "address";
 * PORT = "port";
 */
data class Addresses(
    @SerializedName("local")
    val local: RnAddressTuple? = null,
    @SerializedName("remote")
    val remote: RnAddressTuple? = null,
    @SerializedName("manual")
    val manual: RnAddressTuple? = null,
    @SerializedName("ipv6")
    val ipv6: RnAddressTuple? = null
)