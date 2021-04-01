package org.openvoipalliance.voiplibexample.ui

import android.content.Context
import android.content.Intent
import org.openvoipalliance.voiplib.model.AttendedTransferSession
import org.openvoipalliance.voiplib.model.Call
import org.openvoipalliance.voiplib.repository.initialise.CallListener
import org.openvoipalliance.voiplibexample.ui.call.CallActivity
import org.openvoipalliance.voiplibexample.ui.call.IncomingCallActivity

class CallManager(private val context: Context) : CallListener {

    private var internalActiveCall: Call? = null

    var transfer: AttendedTransferSession? = null

    val activeCall: Call?
        get() {
            transfer?.let {
                return it.to
            }

            return internalActiveCall
        }

    val inactiveCall: Call?
        get() = transfer?.from


    override fun incomingCallReceived(call: Call) {
        if (activeCall == null) {
            this.internalActiveCall = call
            context.startActivity(Intent(context, IncomingCallActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        }
        broadcast()
    }

    override fun outgoingCallCreated(call: Call) {
        if (activeCall == null) {
            this.internalActiveCall = call
            context.startActivity(Intent(context, CallActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        }
        broadcast()
    }

    override fun callConnected(call: Call) {
        context.startActivity(Intent(context, CallActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        broadcast()
    }

    override fun callEnded(call: Call) {
        if (transfer == null) {
            internalActiveCall = null
        }
        transfer = null
        broadcast()
    }

    override fun callUpdated(call: Call) {
        broadcast()
    }

    private fun broadcast() {
        context.sendBroadcast(Intent().apply {
            action = "org.openvoipalliance.voiplibexample.CALL_UPDATE"
        })
    }
}