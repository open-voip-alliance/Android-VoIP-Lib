package nl.spindle.phonelib.repository.call.controls

import nl.spindle.phonelib.model.Session
import nl.spindle.phonelib.model.AttendedTransferSession

interface SipActiveCallControlsRepository {
    fun setMicrophone(on: Boolean)

    fun setHold(session: Session, on: Boolean)

    fun isMicrophoneMuted(): Boolean

    fun transferUnattended(session: Session, to: String)

    fun finishAttendedTransfer(attendedTransferSession: AttendedTransferSession)

    fun switchSession(from: Session, to: Session)

    fun pauseSession(session: Session)

    fun resumeSession(session: Session)
}