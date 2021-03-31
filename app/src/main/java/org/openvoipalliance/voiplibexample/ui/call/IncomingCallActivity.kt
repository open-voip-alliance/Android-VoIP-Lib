package org.openvoipalliance.voiplibexample.ui.call

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_call.callSubtitle
import kotlinx.android.synthetic.main.activity_call.callTitle
import kotlinx.android.synthetic.main.activity_incoming_call.*
import org.openvoipalliance.voiplib.VoIPLib
import org.openvoipalliance.voiplib.model.CallState
import org.openvoipalliance.voiplib.model.Reason
import org.openvoipalliance.voiplibexample.R
import org.openvoipalliance.voiplibexample.VoIPLibExampleApplication
import org.openvoipalliance.voiplibexample.ui.CallManager

class IncomingCallActivity : AppCompatActivity() {

    private val lib: VoIPLib
        get() = VoIPLib.getInstance(this)

    private val calls: CallManager
        get() = VoIPLibExampleApplication.callManager

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            displayCall()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        answerCallButton.setOnClickListener {
            calls.activeCall?.let {
                lib.actions(it).accept()
                finish()
            }
        }

        declineCallButton.setOnClickListener {
            calls.activeCall?.let {
                lib.actions(it).decline(Reason.BUSY)
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, IntentFilter("org.openvoipalliance.voiplibexample.CALL_UPDATE"))
        displayCall()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    private fun displayCall() {
        if (calls.activeCall == null) {
            finish()
            return
        }

        val call = calls.activeCall ?: return

        callTitle.text = call.phoneNumber
        callSubtitle.text = call.displayName
    }
}
