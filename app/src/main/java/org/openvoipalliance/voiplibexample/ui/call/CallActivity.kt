package org.openvoipalliance.voiplibexample.ui.call

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_call.*
import org.openvoipalliance.androidphoneintegration.example.ui.TransferDialog
import org.openvoipalliance.voiplib.VoIPLib
import org.openvoipalliance.voiplibexample.R
import org.openvoipalliance.voiplibexample.VoIPLibExampleApplication
import org.openvoipalliance.voiplibexample.ui.CallManager
import java.util.*
import kotlin.concurrent.fixedRateTimer

class CallActivity : AppCompatActivity() {

    private val lib: VoIPLib
        get() = VoIPLib.getInstance(this)

    private val calls: CallManager
        get() = VoIPLibExampleApplication.callManager

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            render()
        }
    }

    private var timer: Timer? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        endCallButton.setOnClickListener {
            calls.activeCall?.let {
                lib.actions(it).end()
            }
        }

        holdButton.setOnClickListener {
            calls.activeCall?.let {
                lib.actions(it).hold(!it.isOnHold)
            }
        }

        muteButton.setOnClickListener {
            lib.microphoneMuted = !lib.microphoneMuted
        }

        transferButton.setOnClickListener {
            TransferDialog(this).apply {
                onTransferListener = TransferDialog.OnTransferListener { number ->
                    calls.activeCall?.let {
                        calls.transfer = lib.actions(it).beginAttendedTransfer(number)
                    }
                    dismiss()
                }
                show(supportFragmentManager, "")
            }
        }

        transferMergeButton.setOnClickListener {
            calls.transfer?.let {
                lib.actions(it.from).finishAttendedTransfer(it)
            }
        }

        dtmfButton.setOnClickListener {

            val editText = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
            }

            AlertDialog.Builder(this).apply {
                setView(editText)
                setTitle("Send DTMF to Remote Party")
                setPositiveButton("Send DTMF") { _, _ ->
                    calls.activeCall?.let {
                        lib.actions(it).sendDtmf(editText.text.toString())
                    }
                }
                setNegativeButton("Cancel") { _, _ ->
                }
            }.show()
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, IntentFilter("org.openvoipalliance.voiplibexample.CALL_UPDATE"))
        render()
        timer = fixedRateTimer("render", false, initialDelay = 500, period = 500) {
            runOnUiThread {
                render()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
        timer?.cancel()
        timer = null
    }


    private fun render() {
        if (VoIPLibExampleApplication.callManager.activeCall == null) {
            finish()
            return
        }

        val call = VoIPLibExampleApplication.callManager.activeCall ?: return
        val isInTransfer = calls.transfer != null

        if (isInTransfer) {
            transferCallInformation.text = calls.inactiveCall?.phoneNumber ?: ""
            transferContainer.visibility = View.VISIBLE
        } else {
            transferContainer.visibility = View.GONE
        }

        callTitle.text = call.phoneNumber
        callSubtitle.text = call.displayName
        callDuration.text = call.duration.toString()

        holdButton.text = if (call.isOnHold) "unhold" else "hold"
        muteButton.text = if (VoIPLib.getInstance(this).microphoneMuted) "unmute" else "mute"

        callStatus.text = call.state.name

        calls.activeCall?.let {
            callDetailsAdvanced.text = lib.actions(it).callInfo()
        }
    }
}
