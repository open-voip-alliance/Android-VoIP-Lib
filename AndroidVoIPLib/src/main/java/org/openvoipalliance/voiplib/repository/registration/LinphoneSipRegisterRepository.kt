package org.openvoipalliance.voiplib.repository.registration

import android.util.Log
import org.linphone.core.*
import org.openvoipalliance.voiplib.repository.LinphoneCoreInstanceManager
import org.openvoipalliance.voiplib.repository.SimpleCoreListener

internal class LinphoneSipRegisterRepository(private val linphoneCoreInstanceManager: LinphoneCoreInstanceManager) : SipRegisterRepository {

    private val config
        get() = linphoneCoreInstanceManager.voipLibConfig

    private var listener: SimpleCoreListener? = null

    @Throws(CoreException::class)
    override fun register(callback: RegistrationCallback) {
        val core = linphoneCoreInstanceManager.safeLinphoneCore ?: return

        // We do not want multiple registrations to occur, if we are already listening for one
        // we will not start anymore.
        if (listener != null) {
            return
        }

        listener = object : SimpleCoreListener {
            override fun onAccountRegistrationStateChanged(
                core: Core,
                account: Account,
                state: RegistrationState?,
                message: String
            ) {
                callback.invoke(when (state) {
                    RegistrationState.None -> {
                        org.openvoipalliance.voiplib.model.RegistrationState.NONE
                    }
                    RegistrationState.Progress -> {
                        org.openvoipalliance.voiplib.model.RegistrationState.PROGRESS
                    }
                    RegistrationState.Ok -> {
                        linphoneCoreInstanceManager.state.isRegistered = true
                        org.openvoipalliance.voiplib.model.RegistrationState.REGISTERED
                    }
                    RegistrationState.Cleared -> {
                        org.openvoipalliance.voiplib.model.RegistrationState.CLEARED
                    }
                    RegistrationState.Failed -> {
                        linphoneCoreInstanceManager.state.isRegistered = false
                        org.openvoipalliance.voiplib.model.RegistrationState.FAILED
                    }
                    else -> {
                        org.openvoipalliance.voiplib.model.RegistrationState.UNKNOWN
                    }
                })

                if (state == RegistrationState.Failed || state == RegistrationState.Ok) {
                    core.removeListener(this)
                    listener = null
                }
            }
        }

        core.addListener(listener)

        if (config.auth.port < 1 || config.auth.port > 65535) {
            throw IllegalArgumentException("Unable to register with a server when port is invalid: ${config.auth.port}")
        }

        if (config.encryption) {
            core.apply {
                transports = transports.apply {
                    udpPort = DISABLED
                    tcpPort = DISABLED
                    tlsPort = RANDOM_PORT
                }
                mediaEncryption = MediaEncryption.SRTP
                isMediaEncryptionMandatory = true
            }
        } else {
            core.apply {
                transports = transports.apply {
                    udpPort = RANDOM_PORT
                    tcpPort = DISABLED
                    tlsPort = DISABLED
                }
                mediaEncryption = MediaEncryption.None
                isMediaEncryptionMandatory = false
            }
        }

        val authInfo = Factory.instance().createAuthInfo(config.auth.name, config.auth.name, config.auth.password,
                null, null, "${config.auth.domain}:${config.auth.port}").apply {
            algorithm = null
        }

        core.clearProxyConfig()

        val proxyConfig = createProxyConfig(core, config.auth.name, config.auth.domain, config.auth.port.toString())

        if (core.addProxyConfig(proxyConfig) == -1) {
            callback.invoke(org.openvoipalliance.voiplib.model.RegistrationState.FAILED)
            return
        }

        core.apply {
            addAuthInfo(authInfo)
            defaultProxyConfig = core.proxyConfigList.first()
        }
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

    companion object {
        const val RANDOM_PORT = -1
        const val DISABLED = 0
    }
}