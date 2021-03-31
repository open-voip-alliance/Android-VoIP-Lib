package org.openvoipalliance.voiplib.repository.call.controls

import com.google.gson.GsonBuilder
import org.linphone.core.StreamType
import org.openvoipalliance.voiplib.model.AttendedTransferSession
import org.openvoipalliance.voiplib.model.Call
import org.openvoipalliance.voiplib.repository.LinphoneCoreInstanceManager

internal class LinphoneSipActiveCallControlsRepository(private val linphoneCoreInstanceManager: LinphoneCoreInstanceManager) : SipActiveCallControlsRepository {

    override fun setMicrophone(on: Boolean) {
        linphoneCoreInstanceManager.safeLinphoneCore?.enableMic(on)
    }

    override fun setHold(call: Call, on: Boolean) {
        if (on) {
            call.linphoneCall.pause()
        } else {
            call.linphoneCall.resume()
        }
    }

    override fun isMicrophoneMuted(): Boolean {
        val core = linphoneCoreInstanceManager.safeLinphoneCore ?: return false

        return !core.micEnabled()
    }

    override fun transferUnattended(call: Call, to: String) {
        call.linphoneCall.transfer(to)
    }

    override fun finishAttendedTransfer(attendedTransferSession: AttendedTransferSession) {
        attendedTransferSession.from.linphoneCall.transferToAnother(attendedTransferSession.to.linphoneCall)
    }

    override fun pauseCall(call: Call) {
        call.linphoneCall.pause()
    }

    override fun resumeCall(call: Call) {
        call.linphoneCall.resume()
    }

    override fun sendDtmf(call: Call, dtmf: String) {
        if (dtmf.length == 1) {
            call.linphoneCall.sendDtmf(dtmf[0])
        } else {
            call.linphoneCall.sendDtmfs(dtmf)
        }
    }

    override fun provideCallInfo(call: Call): String =
            GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(
                    mapOf(
                            "audio" to mapOf(
                                    "codec" to call.linphoneCall.currentParams.usedAudioPayloadType?.description,
                                    "codecChannels" to call.linphoneCall.currentParams.usedAudioPayloadType?.channels,
                                    "downloadBandwidth" to call.linphoneCall.getStats(StreamType.Audio)?.downloadBandwidth,
                                    "estimatedDownloadBandwidth" to call.linphoneCall.getStats(StreamType.Audio)?.estimatedDownloadBandwidth,
                                    "jitterBufferSizeMs" to call.linphoneCall.getStats(StreamType.Audio)?.jitterBufferSizeMs,
                                    "localLateRate" to call.linphoneCall.getStats(StreamType.Audio)?.localLateRate,
                                    "localLossRate" to call.linphoneCall.getStats(StreamType.Audio)?.localLossRate,
                                    "receiverInterarrivalJitter" to call.linphoneCall.getStats(StreamType.Audio)?.receiverInterarrivalJitter,
                                    "receiverLossRate" to call.linphoneCall.getStats(StreamType.Audio)?.receiverLossRate,
                                    "roundTripDelay" to call.linphoneCall.getStats(StreamType.Audio)?.roundTripDelay,
                                    "rtcpDownloadBandwidth" to call.linphoneCall.getStats(StreamType.Audio)?.rtcpDownloadBandwidth,
                                    "rtcpUploadBandwidth" to call.linphoneCall.getStats(StreamType.Audio)?.rtcpUploadBandwidth,
                                    "senderInterarrivalJitter" to call.linphoneCall.getStats(StreamType.Audio)?.senderInterarrivalJitter,
                                    "senderLossRate" to call.linphoneCall.getStats(StreamType.Audio)?.senderLossRate,
                                    "iceState" to call.linphoneCall.getStats(StreamType.Audio)?.iceState?.name,
                                    "uploadBandwidth" to call.linphoneCall.getStats(StreamType.Audio)?.uploadBandwidth,
                            ),
                            "advanced-settings" to mapOf(
                                "mtu" to call.linphoneCall.core.mtu,
                                "echoCancellationEnabled" to call.linphoneCall.core.echoCancellationEnabled(),
                                "adaptiveRateControlEnabled" to call.linphoneCall.core.adaptiveRateControlEnabled(),
                                "audioAdaptiveJittcompEnabled" to call.linphoneCall.core.audioAdaptiveJittcompEnabled(),
                                "rtpBundleEnabled" to call.linphoneCall.core.rtpBundleEnabled(),
                                "adaptiveRateAlgorithm" to call.linphoneCall.core.adaptiveRateAlgorithm,
                            ),
                            "to-address" to mapOf(
                                    "transport" to call.linphoneCall.toAddress.transport.name,
                                    "domain" to call.linphoneCall.toAddress.domain,
                            ),
                            "remote-params" to mapOf(
                                    "encryption" to call.linphoneCall.remoteParams?.mediaEncryption?.name,
                                    "sessionName" to call.linphoneCall.remoteParams?.sessionName,
                                    "remotePartyId" to call.linphoneCall.remoteParams?.getCustomHeader("Remote-Party-ID"),
                                    "pAssertedIdentity" to call.linphoneCall.remoteParams?.getCustomHeader("P-Asserted-Identity"),
                            ),
                            "params" to mapOf(
                                    "encryption" to call.linphoneCall.params.mediaEncryption.name,
                                    "sessionName" to call.linphoneCall.params.sessionName
                            ),
                            "call" to mapOf(
                                    "callId" to call.linphoneCall.callLog.callId,
                                    "refKey" to call.linphoneCall.callLog.refKey,
                                    "status" to call.linphoneCall.callLog.status,
                                    "direction" to call.linphoneCall.callLog.dir.name,
                                    "quality" to call.linphoneCall.callLog.quality,
                                    "startDate" to call.linphoneCall.callLog.startDate,
                                    "reason" to call.linphoneCall.reason.name,
                                    "duration" to call.linphoneCall.duration
                            ),
                            "error" to mapOf(
                                    "phrase" to call.linphoneCall.errorInfo.phrase,
                                    "protocol" to call.linphoneCall.errorInfo.protocol,
                                    "reason" to call.linphoneCall.errorInfo.reason,
                                    "protocolCode" to call.linphoneCall.errorInfo.protocolCode
                            ),
                    )
            )

    override fun switchCall(from: Call, to: Call) {
        from.linphoneCall.pause()
        to.linphoneCall.resume()
    }
}