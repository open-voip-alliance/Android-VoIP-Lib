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
import org.openvoipalliance.voiplib.model.PathConfigurations
import org.openvoipalliance.voiplib.repository.initialise.LogLevel
import java.io.BufferedReader
import java.util.*
import org.linphone.core.Call as LinphoneCall

private const val LINPHONE_DEBUG_TAG = "SIMPLE_LINPHONE"

private const val BITRATE_LIMIT = 36
private const val DOWNLOAD_BANDWIDTH = 0
private const val UPLOAD_BANDWIDTH = 0

internal class LinphoneCoreInstanceManager(private val mServiceContext: Context): SimpleCoreListener, LoggingServiceListener {
    private var destroyed: Boolean = false
    private var pathConfigurations: PathConfigurations = PathConfigurations(mServiceContext.filesDir.absolutePath)
    lateinit var config: Config
        internal set
    private var linphoneCore: Core? = null

    var isRegistered: Boolean = false
    val initialised: Boolean get() = linphoneCore != null && !destroyed

    val safeLinphoneCore: Core?
        get() {
            return if (initialised) {
                linphoneCore
            } else {
                Log.e(TAG, "Trying to get linphone core while not possible", Exception())
                null
            }
        }

    init {
        Factory.instance().setDebugMode(true, LINPHONE_DEBUG_TAG)
        pathConfigurations = PathConfigurations(mServiceContext.filesDir.absolutePath)
    }

    fun initialiseLinphone(context: Context, config: Config) {
        this.config = config
        Factory.instance().setDebugMode(true, LINPHONE_DEBUG_TAG)
        config.logListener.let { Factory.instance().loggingService.addListener(this) }
        startLibLinphone(context)
    }

    @Synchronized
    private fun startLibLinphone(context: Context) {
        try {
            linphoneCore = Factory.instance().createCoreWithConfig(
                    Factory.instance().createConfigFromString(
                            context.resources.openRawResource(R.raw.linphonerc_factory).bufferedReader().use(BufferedReader::readText)
                    ),
                    context).apply {
                addListener(this@LinphoneCoreInstanceManager)
                enableDnsSrv(false)
                enableDnsSearch(false)
                start()
            }

            initLibLinphone()

            Log.e("TEST123", "CONFIG:" + linphoneCore?.config?.dump() + "")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "startLibLinphone: cannot start linphone")
        }
    }

    @Synchronized
    @Throws(CoreException::class)
    private fun initLibLinphone() {
        val userConfig = this.config

        linphoneCore?.apply {
            setUserAgent(userConfig.userAgent, null)
            ring = userConfig.ring
            isNetworkReachable = true
        }

        config.logListener?.onLogMessageWritten(LogLevel.MESSAGE, "Applying ${userConfig.advancedVoIPSettings}")

        linphoneCore?.apply {
            enableEchoCancellation(userConfig.advancedVoIPSettings.echoCancellation)
            enableAdaptiveRateControl(userConfig.advancedVoIPSettings.adaptiveRateControl)
            mtu = userConfig.advancedVoIPSettings.mtu
            adaptiveRateAlgorithm = userConfig.advancedVoIPSettings.adaptiveRateAlgorithm.name.toLowerCase(Locale.ROOT)
            enableRtpBundle(userConfig.advancedVoIPSettings.mediaMultiplexing)
            enableAudioAdaptiveJittcomp(userConfig.advancedVoIPSettings.jitterCompensation)
        }

        configureCodecs(config.codecs.toSet())
        destroyed = false
    }

    private fun configureCodecs(audioCodecs: Set<Codec>) {
        val linphoneCore = linphoneCore ?: return

        linphoneCore.videoPayloadTypes.forEach { payloadType -> payloadType.enable(false) }

        linphoneCore.audioPayloadTypes.forEach { it.enable(false) }
        for (payloadType in linphoneCore.audioPayloadTypes) {
            payloadType.enable(audioCodecs.contains(Codec.valueOf(payloadType.mimeType.toUpperCase(Locale.ROOT))))
        }
    }

    override fun onCallStateChanged(lc: Core, linphoneCall: LinphoneCall, state: LinphoneCall.State, message: String) {
        Log.e(TAG, "callState: $state, Message: $message")
Log.e("TEST123", "Remotparty:" + linphoneCall.remoteParams?.getCustomHeader("Remote-Party-ID"))

        val call = Call(linphoneCall ?: return)

        Log.e(TAG, "callState: $state, Message: $message - SENDING EVENT")

        when (state) {
            LinphoneCall.State.IncomingReceived -> config.callListener.incomingCallReceived(call)
            LinphoneCall.State.OutgoingInit -> config.callListener.outgoingCallCreated(call)
            LinphoneCall.State.Connected -> {
                safeLinphoneCore?.activateAudioSession(true)
                config.callListener.callConnected(call)
            }
            LinphoneCall.State.End -> config.callListener.callEnded(call)
            LinphoneCall.State.Error -> config.callListener.error(call)
            else -> config.callListener.callUpdated(call)
        }
    }

    override fun onGlobalStateChanged(lc: Core, gstate: GlobalState, message: String) {
        gstate?.let {
            globalState = it

            when (it) {
                Off -> config.onDestroy()
                On -> config.onReady()
                else -> {}
            }
        }
    }

    override fun onLogMessageWritten(service: LoggingService, domain: String, lev: org.linphone.core.LogLevel, message: String) {
        GlobalScope.launch(Dispatchers.IO) {
            config.logListener?.onLogMessageWritten(when (lev) {
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
        isRegistered = false
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
}