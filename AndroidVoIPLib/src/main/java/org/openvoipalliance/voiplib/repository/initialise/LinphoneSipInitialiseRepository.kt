package org.openvoipalliance.voiplib.repository.initialise

import android.content.Context
import org.linphone.core.Factory
import org.openvoipalliance.voiplib.repository.LinphoneCoreInstanceManager
import org.openvoipalliance.voiplib.config.Config as VoIPLibConfig

internal class LinphoneSipInitialiseRepository(private val linphoneCoreInstanceManager: LinphoneCoreInstanceManager, private val context: Context) : SipInitialiseRepository {

    override fun initialise(config: VoIPLibConfig) {
        Factory.instance()
        linphoneCoreInstanceManager.initialiseLinphone(context, config)
    }

    override fun destroy() {
        linphoneCoreInstanceManager.destroy()
    }

    override fun swapConfig(config: VoIPLibConfig) {
        linphoneCoreInstanceManager.config = config
    }

    override fun refreshRegisters(): Boolean {
        linphoneCoreInstanceManager.safeLinphoneCore?.let {
            it.refreshRegisters()
            return true
        }
        return false
    }

    override fun currentConfig(): org.openvoipalliance.voiplib.config.Config = linphoneCoreInstanceManager.config

    override fun isInitialised(): Boolean = linphoneCoreInstanceManager.initialised

    override fun wake() {
        linphoneCoreInstanceManager.safeLinphoneCore?.ensureRegistered()
    }

    override fun version(): String = linphoneCoreInstanceManager.safeLinphoneCore?.version ?: ""
}