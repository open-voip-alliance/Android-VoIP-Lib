package org.openvoipalliance.voiplib.repository.call.session

import org.linphone.core.Address
import org.linphone.core.CoreException
import org.openvoipalliance.voiplib.model.Call
import org.openvoipalliance.voiplib.model.Reason
import org.openvoipalliance.voiplib.repository.LinphoneCoreInstanceManager
import org.linphone.core.Call as LinphoneCall

internal class LinphoneSipSessionRepository(private val linphoneCoreInstanceManager: LinphoneCoreInstanceManager) : SipSessionRepository {

    override fun acceptIncoming(call: Call) {
        try {
            call.linphoneCall.accept()
        } catch (e: CoreException) {
            e.printStackTrace()
        }
    }

    override fun declineIncoming(call: Call, reason: Reason) {
        try {
            call.linphoneCall.decline(org.linphone.core.Reason.fromInt(reason.value))
        } catch (e: CoreException) {
            e.printStackTrace()
        }
    }

    override fun callTo(number: String): Call {
        if (!linphoneCoreInstanceManager.state.initialised) {
            throw Exception("Linphone is not ready")
        }

        if (number.isEmpty()) {
            throw IllegalArgumentException("Phone number is not valid")
        }

        val connectionParameters = ConnectionParameters(number, linphoneCoreInstanceManager.voipLibConfig.auth.domain)

        return Call(callTo(connectionParameters) ?: throw Exception("Call failed"))
    }

    private fun callTo(connectionParameters: ConnectionParameters) : LinphoneCall? {
        val core = linphoneCoreInstanceManager.safeLinphoneCore ?: return null

        val address: Address = try {
            core.interpretUrl(connectionParameters.asUrl())!!
        } catch (e: CoreException) {
            e.printStackTrace()
            return null
        }

        val params = core.createCallParams(null)?.apply {
            enableVideo(false)
        } ?: return null

        return core.inviteAddressWithParams(address, params)
    }

    override fun end(call: Call) {
        call.linphoneCall.terminate()
        if (linphoneCoreInstanceManager.safeLinphoneCore?.isInConference == true) {
            linphoneCoreInstanceManager.safeLinphoneCore?.terminateConference()
        }
    }

    data class ConnectionParameters(
            val username: String,
            val host: String
    ) {
        fun asUrl() = "$username@$host"
    }
}