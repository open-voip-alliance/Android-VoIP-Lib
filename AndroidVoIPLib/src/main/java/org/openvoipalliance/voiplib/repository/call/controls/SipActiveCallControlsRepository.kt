package org.openvoipalliance.voiplib.repository.call.controls

import org.openvoipalliance.voiplib.model.AttendedTransferSession
import org.openvoipalliance.voiplib.model.Call

interface SipActiveCallControlsRepository {
    fun setMicrophone(on: Boolean)

    fun setHold(call: Call, on: Boolean)

    fun isMicrophoneMuted(): Boolean

    fun transferUnattended(call: Call, to: String)

    fun finishAttendedTransfer(attendedTransferSession: AttendedTransferSession)

    fun switchCall(from: Call, to: Call)

    fun pauseCall(call: Call)

    fun resumeCall(call: Call)

    fun sendDtmf(call: Call, dtmf: String)

    fun provideCallInfo(call: Call): String

    fun routeAudioToEarpiece(call: Call)

    fun routeAudioToSpeaker(call: Call)

    fun routeAudioToBluetooth(call: Call)

    fun routeAudioToHeadset(call: Call)
}