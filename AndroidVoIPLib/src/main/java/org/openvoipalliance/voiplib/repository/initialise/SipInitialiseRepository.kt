package org.openvoipalliance.voiplib.repository.initialise

import org.openvoipalliance.voiplib.config.Config

internal interface SipInitialiseRepository {
    fun initialise(config: Config)
    fun swapConfig(config: Config)
    fun destroy()
    fun refreshRegisters(): Boolean
    fun currentConfig(): Config
    fun isInitialised(): Boolean
    fun wake()
    fun version(): String
}