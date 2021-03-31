package org.openvoipalliance.voiplib.repository.registration

import android.R.attr
import org.linphone.core.*
import org.openvoipalliance.voiplib.repository.LinphoneCoreInstanceManager
import org.openvoipalliance.voiplib.repository.SimpleCoreListener

internal class LinphoneSipRegisterRepository(private val linphoneCoreInstanceManager: LinphoneCoreInstanceManager) : SipRegisterRepository {

    private val config
        get() = linphoneCoreInstanceManager.config

    @Throws(CoreException::class)
    override fun register(callback: RegistrationCallback) {
        val core = linphoneCoreInstanceManager.safeLinphoneCore ?: return

        core.addListener(object : SimpleCoreListener {
            override fun onRegistrationStateChanged(lc: Core, cfg: ProxyConfig, cstate: RegistrationState, message: String) {
                callback.invoke(when (cstate) {
                    RegistrationState.None -> {
                        org.openvoipalliance.voiplib.model.RegistrationState.NONE
                    }
                    RegistrationState.Progress -> {
                        org.openvoipalliance.voiplib.model.RegistrationState.PROGRESS
                    }
                    RegistrationState.Ok -> {
                        linphoneCoreInstanceManager.isRegistered = true
                        org.openvoipalliance.voiplib.model.RegistrationState.REGISTERED
                    }
                    RegistrationState.Cleared -> {
                        org.openvoipalliance.voiplib.model.RegistrationState.CLEARED
                    }
                    RegistrationState.Failed -> {
                        linphoneCoreInstanceManager.isRegistered = false
                        org.openvoipalliance.voiplib.model.RegistrationState.FAILED
                    }
                    else -> {
                        org.openvoipalliance.voiplib.model.RegistrationState.UNKNOWN
                    }
                })

                if (cstate == RegistrationState.Failed || cstate == RegistrationState.Ok) {
                    core.removeListener(this)
                }
            }
        })

        core.transports = core.transports.apply {
            udpPort = if (config.encryption) attr.port else RANDOM_PORT
            tcpPort = if (config.encryption) attr.port else RANDOM_PORT
            tlsPort = RANDOM_PORT
        }

        core.mediaEncryption = if (config.encryption) MediaEncryption.SRTP else MediaEncryption.None
        core.isMediaEncryptionMandatory = config.encryption

        config.stun?.let {
            core.stunServer = it
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
            useRfc2833ForDtmf = true
            enableIpv6(false)
            isPushNotificationEnabled = false
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
            avpfRrInterval = 0
            avpfMode = AVPFMode.Disabled
            serverAddr = proxy
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

    override fun isRegistered() = linphoneCoreInstanceManager.isRegistered

    companion object {
        const val RANDOM_PORT = -1
    }
}