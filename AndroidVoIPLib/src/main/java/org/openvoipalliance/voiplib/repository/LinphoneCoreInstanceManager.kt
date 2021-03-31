package org.openvoipalliance.voiplib.repository

import android.content.Context
import android.os.Handler
import android.os.Looper
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
import java.io.File
import java.io.IOException
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
            copyAssetsFromPackage()
            linphoneCore = Factory.instance().createCoreWithConfig(
                    Factory.instance().createConfig(pathConfigurations.linphoneConfigFile),
                    context).apply {
                addListener(this@LinphoneCoreInstanceManager)
                enableDnsSrv(false)
                enableDnsSearch(false)
                isAutoIterateEnabled = true
                start()
            }

            initLibLinphone()

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
            remoteRingbackTone = pathConfigurations.ringSound
            ring = userConfig.ring
            playFile = pathConfigurations.pauseSound
            rootCa = pathConfigurations.linphoneRootCaFile
            remoteRingbackTone = pathConfigurations.ringSound
            isNetworkReachable = true
            config?.setInt("audio", "codec_bitrate_limit", BITRATE_LIMIT)
            downloadBandwidth = DOWNLOAD_BANDWIDTH
            uploadBandwidth = UPLOAD_BANDWIDTH
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

        setCodecMime(config.codecs.toSet())
        destroyed = false
    }

    private fun setCodecMime(audioCodecs: Set<Codec>) {
        linphoneCore?.let {
            for (payloadType in it.audioPayloadTypes) {
                payloadType.enable(audioCodecs.contains(Codec.valueOf(payloadType.mimeType.toUpperCase(Locale.ROOT))))
            }
        }
    }

    @Throws(IOException::class)
    private fun copyAssetsFromPackage() {
        copyIfNotExist(mServiceContext, R.raw.oldphone_mono, pathConfigurations.ringSound)
        copyIfNotExist(mServiceContext, R.raw.ringback, pathConfigurations.ringBackSound)
        copyIfNotExist(mServiceContext, R.raw.toy_mono, pathConfigurations.pauseSound)
        copyIfNotExist(mServiceContext, R.raw.linphonerc_default, pathConfigurations.linphoneConfigFile)
        copyIfNotExist(mServiceContext, R.raw.linphonerc_factory, File(pathConfigurations.linphoneFactoryConfigFile).name)
        copyIfNotExist(mServiceContext, R.raw.lpconfig, pathConfigurations.linphoneConfigXsp)
        copyIfNotExist(mServiceContext, R.raw.rootca, pathConfigurations.linphoneRootCaFile)
    }

    @Throws(IOException::class)
    fun copyIfNotExist(context: Context, resourceId: Int, target: String?) {
        val fileToCopy = File(target ?: "")
        if (!fileToCopy.exists()) {
            copyFromPackage(context, resourceId, fileToCopy.name)
        }
    }

    @Throws(IOException::class)
    fun copyFromPackage(context: Context, resourceId: Int, target: String?) {
        val outputStream = context.openFileOutput(target, 0)
        val inputStream = context.resources.openRawResource(resourceId)
        var readByte: Int
        val buff = ByteArray(8048)
        while (inputStream.read(buff).also { readByte = it } != -1) {
            outputStream.write(buff, 0, readByte)
        }
        outputStream.flush()
        outputStream.close()
        inputStream.close()
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