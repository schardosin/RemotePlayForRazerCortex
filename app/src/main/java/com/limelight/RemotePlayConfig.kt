package com.limelight

import com.limelight.nvstream.jni.MoonBridge

/**
 * A
 */
data class RemotePlayConfig(
    val name: String,
    val serviceTypeNsd: String,
    val serviceTypeJmdns: String,
    val defaultHttpPort: Int,
    val defaultHttpsPort: Int,
    val x509ClientAlias: String,
    val x509RDNCommonName: String,
    val defaultUniqueId: String,
    val defaultDeviceName: String,
    val portsMap : Map<Int, Int>
) {

    companion object {

        /**
         * For [MoonBridge.stringifyPortFlags] and [NeuronBridge.stringifyPortFlags]
         */
        const val ML_PORT_INDEX_TCP_47984 = 0
        const val ML_PORT_INDEX_TCP_47989 = 1
        const val ML_PORT_INDEX_TCP_48010 = 2
        const val ML_PORT_INDEX_UDP_47998 = 8
        const val ML_PORT_INDEX_UDP_47999 = 9
        const val ML_PORT_INDEX_UDP_48000 = 10
        const val ML_PORT_INDEX_UDP_48010 = 11

        val limelightPorts = mapOf(
            ML_PORT_INDEX_TCP_47984 to 47984,
            ML_PORT_INDEX_TCP_47989 to 47989,
            ML_PORT_INDEX_TCP_48010 to 48010,
            ML_PORT_INDEX_UDP_47998 to 47998,
            ML_PORT_INDEX_UDP_47999 to 47999,
            ML_PORT_INDEX_UDP_48000 to 48000,
            ML_PORT_INDEX_UDP_48010 to 48010)

        val neuronPorts = mapOf(
            ML_PORT_INDEX_TCP_47984 to 51332,
            ML_PORT_INDEX_TCP_47989 to 51337,
            ML_PORT_INDEX_TCP_48010 to 51358,
            ML_PORT_INDEX_UDP_47998 to 51346,
            ML_PORT_INDEX_UDP_47999 to 51347,
            ML_PORT_INDEX_UDP_48000 to 51348,
            ML_PORT_INDEX_UDP_48010 to 51358)

        /**
         * TO BE REMOVED
         *
         * This was added because in cortex-PC v11.0.7.0, only the port was change
         * and serviceType was not
         */
        val cortexPcDev = RemotePlayConfig(
            name = "cortex-pc-dev",
            serviceTypeNsd = "_nvstream._tcp",
            serviceTypeJmdns = "_nvstream._tcp.local.",
            defaultHttpPort = 51337,
            defaultHttpsPort = 51332,
            x509ClientAlias = "RZR-RSA",
            x509RDNCommonName = "Nexus",
            defaultUniqueId = "0123456789ABCDEF",
            defaultDeviceName = "roth",
            portsMap = neuronPorts
        )


        @JvmStatic
        val razer = RemotePlayConfig(
            name = "razer",
            serviceTypeNsd = "_rzstream._tcp",
            serviceTypeJmdns = "_rzstream._tcp.local.",
            defaultHttpPort = 51337,
            defaultHttpsPort = 51332,
            x509ClientAlias = "RZR-RSA",
            x509RDNCommonName = "Nexus",
            defaultUniqueId = "0123456789ABCDEF",
            defaultDeviceName = "roth",
            portsMap = neuronPorts
        )

        @JvmStatic
        val limelight = RemotePlayConfig(
            name = "limelight",
            serviceTypeNsd = "_nvstream._tcp",
            serviceTypeJmdns = "_nvstream._tcp.local.",
            defaultHttpPort = 47989,
            defaultHttpsPort = 47984,
            x509ClientAlias = "Limelight-RSA",
            x509RDNCommonName = "Nexus",
            defaultUniqueId = "0123456789ABCDEF",
            defaultDeviceName = "roth",
            portsMap = limelightPorts
        )

        @JvmStatic
        val default by lazy {
            setOf(limelight, razer, cortexPcDev).firstOrNull { it.name == BuildConfig.DEFAULT_REMOTE_PLAY_CONFIG } ?: limelight
        }



    }
}


