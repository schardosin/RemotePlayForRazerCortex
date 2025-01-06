package com.razer.neuron.provider.sources

import android.content.Context
import com.limelight.computers.ComputerDatabaseManager
import com.razer.neuron.provider.ComputerSharedModelHelper
import com.limelight.nvstream.http.ComputerDetails
import com.razer.neuron.provider.DaoContentProvider
import com.razer.neuron.shared.SharedConstants

/**
 * [AppContentProvider] for [ComputerDetails]
 */
class NeuronComputerDetailsSource(val manager: ComputerDatabaseManager) : DaoContentProvider<ComputerDetails, ComputerSharedModelHelper>(keyColName = ComputerSharedModelHelper.COL_UUID) {

    override val path = SharedConstants.COMPUTER_DETAILS

    override val modelHelper = ComputerSharedModelHelper

    override suspend fun getByKey(context : Context,  key: String): ComputerDetails = manager.getComputerByUUID(key)

    override suspend fun getAll(context : Context): List<ComputerDetails> {
        val allComputers = manager.allComputers
        return allComputers
    }

    override suspend fun deleteByKey(context : Context, key: String): Boolean {
        return manager.getComputerByUUID(key)?.let {
            manager.deleteComputer(it)
            true
        } ?: false
    }

    override suspend fun insertOrReplace(context : Context, objects: List<ComputerDetails>) {
        objects.forEach {
            manager.updateComputer(it)
        }
    }
}

