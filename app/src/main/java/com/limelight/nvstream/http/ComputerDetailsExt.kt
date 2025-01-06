package com.limelight.nvstream.http

fun ComputerDetails.isPaired(): Boolean {
    return pairState == PairingManager.PairState.PAIRED || (serverCert != null && pairState != PairingManager.PairState.NOT_PAIRED)
}