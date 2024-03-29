package org.openvoipalliance.voiplib.repository.registration

import android.util.Log
import org.linphone.core.*
import org.openvoipalliance.voiplib.repository.LinphoneCoreInstanceManager
import org.openvoipalliance.voiplib.repository.SimpleCoreListener

internal class LinphoneSipRegisterRepository(private val linphoneCoreInstanceManager: LinphoneCoreInstanceManager) : SipRegisterRepository {

    private val config
        get() = linphoneCoreInstanceManager.voipLibConfig

    private val registrationListener = RegistrationListener()

    private var callback: RegistrationCallback? = null

    @Throws(CoreException::class)
    override fun register(callback: RegistrationCallback) {
        val core = linphoneCoreInstanceManager.safeLinphoneCore ?: return

        core.apply {
            removeListener(registrationListener)
            addListener(registrationListener)
        }

        this.callback = callback

        if (core.proxyConfigList.isNotEmpty()) {
            linphoneCoreInstanceManager.log("Proxy config found, re-registering.")
            core.refreshRegisters()
            return
        }

        linphoneCoreInstanceManager.log("No proxy config found, registering for the first time.")

        if (config.auth.port < 1 || config.auth.port > 65535) {
            throw IllegalArgumentException("Unable to register with a server when port is invalid: ${config.auth.port}")
        }

        val proxyConfig = createProxyConfig(core, config.auth.name, config.auth.domain, config.auth.port.toString())

        if (core.addProxyConfig(proxyConfig) == -1) {
            callback.invoke(org.openvoipalliance.voiplib.model.RegistrationState.FAILED)
            return
        }

        core.apply {
            addAuthInfo(createAuthInfo())
            defaultProxyConfig = core.proxyConfigList.first()
        }
    }

    private fun createAuthInfo() = Factory.instance().createAuthInfo(config.auth.name, config.auth.name, config.auth.password,
        null, null, "${config.auth.domain}:${config.auth.port}").apply {
        algorithm = null
    }

    private fun createProxyConfig(core: Core, name: String, domain: String, port: String): ProxyConfig {
        val identify = "sip:$name@$domain:$port"
        val proxy = "sip:$domain:$port"
        val identifyAddress = Factory.instance().createAddress(identify)

        return core.createProxyConfig().apply {
            enableRegister(true)
            enableQualityReporting(false)
            qualityReportingCollector = null
            qualityReportingInterval = 0
            identityAddress = identifyAddress
            isPushNotificationAllowed = false
            avpfMode = AVPFMode.Default
            serverAddr = proxy
            natPolicy = null
            done()
        }
    }

    override fun unregister() {
        val core = linphoneCoreInstanceManager.safeLinphoneCore ?: return

        core.proxyConfigList.forEach {
            it.edit()
            it.enableRegister(false)
            it.done()
            core.removeProxyConfig(it)
        }

        core.authInfoList.forEach {
            core.removeAuthInfo(it)
        }
    }

    override fun isRegistered() = linphoneCoreInstanceManager.state.isRegistered

    private inner class RegistrationListener : SimpleCoreListener {
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState?,
            message: String
        ) {

            linphoneCoreInstanceManager.log("Received registration state change: ${state?.name}")

            if (state == RegistrationState.Failed || state == RegistrationState.Ok) {
                linphoneCoreInstanceManager.state.isRegistered = state == RegistrationState.Ok
                callback?.invoke(if (state == RegistrationState.Ok) org.openvoipalliance.voiplib.model.RegistrationState.REGISTERED else org.openvoipalliance.voiplib.model.RegistrationState.FAILED)
                callback = null
            }
        }
    }
}