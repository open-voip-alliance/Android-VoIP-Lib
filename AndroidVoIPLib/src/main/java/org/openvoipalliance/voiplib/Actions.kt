package org.openvoipalliance.voiplib

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import org.koin.core.component.inject
import org.openvoipalliance.voiplib.di.VoIPLibKoinComponent
import org.openvoipalliance.voiplib.model.AttendedTransferSession
import org.openvoipalliance.voiplib.model.Call
import org.openvoipalliance.voiplib.model.Reason
import org.openvoipalliance.voiplib.repository.call.controls.SipActiveCallControlsRepository
import org.openvoipalliance.voiplib.repository.call.session.SipSessionRepository

class Actions(context: Context, private val call: Call) : VoIPLibKoinComponent {

    private val sipCallControlsRepository: SipActiveCallControlsRepository by inject()
    private val sipSessionRepository: SipSessionRepository by inject()

    /** --Control an incoming call-- */
    /**
     * Accepts the current incoming session/call when it exists
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun accept() = sipSessionRepository.acceptIncoming(call)

    /**
     * Declines the current incoming session/call when it exists
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun decline(reason: Reason) = sipSessionRepository.declineIncoming(call, reason)

    /**
     * Hangs up the current active session/call when it exists
     */
    fun end() = sipSessionRepository.end(call)

    /**
     * Pause a session.
     * @param from The session you want to pause.
     */
    fun pause() = sipCallControlsRepository.pauseCall(call)

    /**
     * Resume a session.
     * @param call The session you want to resume.
     */
    fun resume() = sipCallControlsRepository.resumeCall(call)

    /**
     * Switch between sessions.
     * @param from The session you want to pause.
     * @param to The number you want to resume.
     */
    fun switchActiveCall(to: Call) = sipCallControlsRepository.switchCall(call, to)

    /**
     * Turns session on hold or off.
     * @param call The session you want to control.
     * @param on If true, hold will turn on or stay on. If false it will turn off or stay off.
     */
    fun hold(on: Boolean) = sipCallControlsRepository.setHold(call, on)

    /**
     * Transfer a session to a number unattended.
     * @param from The session you want to control.
     * @param to The number you want to call to.
     */
    fun transferUnattended(to: String) = sipCallControlsRepository.transferUnattended(call, to)

    /**
     * Begin an attended transfer, putting the current call on hold and placing a call to a new user.
     *
     * @param from The session you want to control.
     * @param to The number you want to transfer to.
     */
    @RequiresPermission(allOf = [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO])
    fun beginAttendedTransfer(to: String): AttendedTransferSession {
        pause()

        val targetCall = sipSessionRepository.callTo(to) ?: throw Exception("Unable to make call for target session")

        sipCallControlsRepository.resumeCall(targetCall)

        return AttendedTransferSession(call, targetCall)
    }

    /**
     * Complete a pending attended transfer, merging the two calls.
     *
     * @param attendedTransferSession The transfer session that should be merged.
     */
    fun finishAttendedTransfer(attendedTransferSession: AttendedTransferSession) = sipCallControlsRepository.finishAttendedTransfer(attendedTransferSession)

    /**
     * Send a dtmf string.
     *
     */
    fun sendDtmf(dtmf: String) = sipCallControlsRepository.sendDtmf(call, dtmf)

    fun callInfo(): String = sipCallControlsRepository.provideCallInfo(call)
}