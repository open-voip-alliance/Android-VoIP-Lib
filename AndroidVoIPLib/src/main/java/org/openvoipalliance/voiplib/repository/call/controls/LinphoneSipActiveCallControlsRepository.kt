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
            GsonBuilder()
                    .disableHtmlEscaping()
                    .setPrettyPrinting()
                    .create()
                    .toJson(buildCallInfo(call.linphoneCall))

    private fun buildCallInfo(call: org.linphone.core.Call): Map<String, Any> = mapOf(
            "audio" to mapOf(
                    "codec" to call.currentParams.usedAudioPayloadType?.description,
                    "codecMime" to call.currentParams.usedAudioPayloadType?.mimeType,
                    "codecRecvFmtp" to call.currentParams.usedAudioPayloadType?.recvFmtp,
                    "codecSendFmtp" to call.currentParams.usedAudioPayloadType?.sendFmtp,
                    "codecChannels" to call.currentParams.usedAudioPayloadType?.channels,
                    "downloadBandwidth" to call.getStats(StreamType.Audio)?.downloadBandwidth,
                    "estimatedDownloadBandwidth" to call.getStats(StreamType.Audio)?.estimatedDownloadBandwidth,
                    "jitterBufferSizeMs" to call.getStats(StreamType.Audio)?.jitterBufferSizeMs,
                    "localLateRate" to call.getStats(StreamType.Audio)?.localLateRate,
                    "localLossRate" to call.getStats(StreamType.Audio)?.localLossRate,
                    "receiverInterarrivalJitter" to call.getStats(StreamType.Audio)?.receiverInterarrivalJitter,
                    "receiverLossRate" to call.getStats(StreamType.Audio)?.receiverLossRate,
                    "roundTripDelay" to call.getStats(StreamType.Audio)?.roundTripDelay,
                    "rtcpDownloadBandwidth" to call.getStats(StreamType.Audio)?.rtcpDownloadBandwidth,
                    "rtcpUploadBandwidth" to call.getStats(StreamType.Audio)?.rtcpUploadBandwidth,
                    "senderInterarrivalJitter" to call.getStats(StreamType.Audio)?.senderInterarrivalJitter,
                    "senderLossRate" to call.getStats(StreamType.Audio)?.senderLossRate,
                    "iceState" to call.getStats(StreamType.Audio)?.iceState?.name,
                    "uploadBandwidth" to call.getStats(StreamType.Audio)?.uploadBandwidth,
            ),
            "advanced-settings" to mapOf(
                    "mtu" to call.core.mtu,
                    "echoCancellationEnabled" to call.core.echoCancellationEnabled(),
                    "adaptiveRateControlEnabled" to call.core.adaptiveRateControlEnabled(),
                    "audioAdaptiveJittcompEnabled" to call.core.audioAdaptiveJittcompEnabled(),
                    "rtpBundleEnabled" to call.core.rtpBundleEnabled(),
                    "adaptiveRateAlgorithm" to call.core.adaptiveRateAlgorithm,
            ),
            "to-address" to mapOf(
                    "transport" to call.toAddress.transport.name,
                    "domain" to call.toAddress.domain,
            ),
            "remote-params" to mapOf(
                    "encryption" to call.remoteParams?.mediaEncryption?.name,
                    "sessionName" to call.remoteParams?.sessionName,
                    "remotePartyId" to call.remoteParams?.getCustomHeader("Remote-Party-ID"),
                    "pAssertedIdentity" to call.remoteParams?.getCustomHeader("P-Asserted-Identity"),
            ),
            "params" to mapOf(
                    "encryption" to call.params.mediaEncryption.name,
                    "sessionName" to call.params.sessionName
            ),
            "call" to mapOf(
                    "callId" to call.callLog.callId,
                    "refKey" to call.callLog.refKey,
                    "status" to call.callLog.status,
                    "direction" to call.callLog.dir.name,
                    "quality" to call.callLog.quality,
                    "startDate" to call.callLog.startDate,
                    "reason" to call.reason.name,
                    "duration" to call.duration
            ),
            "network" to mapOf(
                    "stunEnabled" to call.core.natPolicy?.stunEnabled(),
                    "stunServer" to call.core.natPolicy?.stunServer,
                    "upnpEnabled" to call.core.natPolicy?.upnpEnabled(),
                    "ipv6Enabled" to call.core.ipv6Enabled(),
            ),
            "error" to mapOf(
                    "phrase" to call.errorInfo.phrase,
                    "protocol" to call.errorInfo.protocol,
                    "reason" to call.errorInfo.reason,
                    "protocolCode" to call.errorInfo.protocolCode
            ),
    )

    override fun switchCall(from: Call, to: Call) {
        from.linphoneCall.pause()
        to.linphoneCall.resume()
    }
}