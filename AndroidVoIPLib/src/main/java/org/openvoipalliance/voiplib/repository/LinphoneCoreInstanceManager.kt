package org.openvoipalliance.voiplib.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.linphone.core.*
import org.linphone.core.GlobalState.Off
import org.linphone.core.GlobalState.On
import org.linphone.core.LogLevel.*
import org.openvoipalliance.voiplib.R
import org.openvoipalliance.voiplib.config.Config
import org.openvoipalliance.voiplib.model.Call
import org.openvoipalliance.voiplib.model.Codec
import org.openvoipalliance.voiplib.repository.initialise.LogLevel
import java.io.BufferedReader
import java.util.*
import org.linphone.core.Call as LinphoneCall

private const val LINPHONE_DEBUG_TAG = "SIMPLE_LINPHONE"

internal class LinphoneCoreInstanceManager(private val context: Context): SimpleCoreListener, LoggingServiceListener {

    internal val state = CoreState()

    lateinit var voipLibConfig: Config
        internal set

    private var linphoneCore: Core? = null

    val safeLinphoneCore: Core?
        get() {
            return if (state.initialised) {
                linphoneCore
            } else {
                Log.e(TAG, "Trying to get linphone core while not possible", Exception())
                null
            }
        }

    init {
        Factory.instance().setDebugMode(true, LINPHONE_DEBUG_TAG)
    }

    fun initialiseLinphone(config: Config) {
        this.voipLibConfig = config

        try {
            startLibLinphone()
        } catch (e: Exception) {
            config.logListener?.onLogMessageWritten(LogLevel.ERROR, "Failed to start Linphone: ${e.localizedMessage}")
            Log.e(TAG, "startLibLinphone: cannot start linphone")
        }
    }

    @Synchronized
    @Throws(Exception::class)
    private fun startLibLinphone() {
        voipLibConfig.logListener.let { Factory.instance().loggingService.addListener(this) }

        this.linphoneCore = createLinphoneCore(context).apply {
            addListener(this@LinphoneCoreInstanceManager)
            enableDnsSrv(false)
            enableDnsSearch(false)
            setDnsServers(arrayOf("8.8.8.8", "8.8.4.4"))
            setDnsServersApp(arrayOf("8.8.8.8", "8.8.4.4"))
            setUserAgent(voipLibConfig.userAgent, null)
            ring = voipLibConfig.ring
            configureCodecs(this)

            log("Applying ${voipLibConfig.advancedVoIPSettings}")

            enableEchoCancellation(voipLibConfig.advancedVoIPSettings.echoCancellation)
            enableAdaptiveRateControl(voipLibConfig.advancedVoIPSettings.adaptiveRateControl)
            mtu = voipLibConfig.advancedVoIPSettings.mtu
            adaptiveRateAlgorithm = voipLibConfig.advancedVoIPSettings.adaptiveRateAlgorithm.name.toLowerCase(Locale.ROOT)
            enableRtpBundle(voipLibConfig.advancedVoIPSettings.mediaMultiplexing)
            enableAudioAdaptiveJittcomp(voipLibConfig.advancedVoIPSettings.jitterCompensation)
        }.also {
            it.start()
            log("Started Linphone with config:\n ${it.config.dump()}")
        }

        state.destroyed = false
    }

    /**
     * Creates the Linphone core by reading in the linphone raw configuration file.
     *
     */
    private fun createLinphoneCore(context: Context) = Factory.instance().createCoreWithConfig(
            Factory.instance().createConfigFromString(
                    context.resources.openRawResource(R.raw.linphone_initial_config).bufferedReader().use(BufferedReader::readText)
            ), context)

    private fun log(message: String, level: LogLevel = LogLevel.DEBUG) {
        voipLibConfig.logListener?.onLogMessageWritten(message = message, lev = level)
    }

    private fun configureCodecs(core: Core) {
        val codecs = this.voipLibConfig.codecs

        core.videoPayloadTypes.forEach { it.enable(false) }

        core.audioPayloadTypes.forEach { it.enable(codecs.contains(Codec.valueOf(it.mimeType.toUpperCase(Locale.ROOT)))) }
    }

    override fun onCallStateChanged(lc: Core, linphoneCall: LinphoneCall, state: LinphoneCall.State, message: String) {
        Log.e(TAG, "callState: $state, Message: $message")

        val call = Call(linphoneCall)

        Log.e(TAG, "callState: $state, Message: $message - SENDING EVENT")

        when (state) {
            LinphoneCall.State.IncomingReceived -> voipLibConfig.callListener.incomingCallReceived(call)
            LinphoneCall.State.OutgoingInit -> voipLibConfig.callListener.outgoingCallCreated(call)
            LinphoneCall.State.Connected -> {
                safeLinphoneCore?.activateAudioSession(true)
                voipLibConfig.callListener.callConnected(call)
            }
            LinphoneCall.State.End -> voipLibConfig.callListener.callEnded(call)
            LinphoneCall.State.Error -> voipLibConfig.callListener.error(call)
            else -> voipLibConfig.callListener.callUpdated(call)
        }
    }

    override fun onGlobalStateChanged(lc: Core, gstate: GlobalState, message: String) {
        gstate.let {
            globalState = it

            when (it) {
                Off -> voipLibConfig.onDestroy()
                On -> voipLibConfig.onReady()
                else -> {}
            }
        }
    }

    override fun onLogMessageWritten(service: LoggingService, domain: String, lev: org.linphone.core.LogLevel, message: String) {
        GlobalScope.launch(Dispatchers.IO) {
            voipLibConfig.logListener?.onLogMessageWritten(when (lev) {
                Debug -> LogLevel.DEBUG
                Trace -> LogLevel.TRACE
                Message -> LogLevel.MESSAGE
                Warning -> LogLevel.WARNING
                Error -> LogLevel.ERROR
                Fatal -> LogLevel.FATAL
            }, message)
        }
    }

    @Synchronized
    fun destroy() {
        state.isRegistered = false
        Factory.instance().loggingService.removeListener(this@LinphoneCoreInstanceManager)
        linphoneCore?.isNetworkReachable = false
        linphoneCore?.stop()
        linphoneCore?.removeListener(this@LinphoneCoreInstanceManager)
        linphoneCore = null
    }

    companion object {
        const val TAG = "VOIPLIB-LINPHONE"
        var globalState: GlobalState = Off
    }

    inner class CoreState {
        var destroyed: Boolean = false
        var isRegistered: Boolean = false
        val initialised: Boolean get() = linphoneCore != null && !destroyed
    }
}