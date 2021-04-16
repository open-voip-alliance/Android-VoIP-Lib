package org.openvoipalliance.voiplibexample

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.openvoipalliance.voiplib.VoIPLib
import org.openvoipalliance.voiplib.config.AdaptiveRateAlgorithm
import org.openvoipalliance.voiplib.config.AdvancedVoIPSettings
import org.openvoipalliance.voiplib.config.Auth
import org.openvoipalliance.voiplib.config.Config
import org.openvoipalliance.voiplib.model.Codec
import org.openvoipalliance.voiplib.model.Codec.OPUS
import org.openvoipalliance.voiplib.repository.initialise.LogLevel
import org.openvoipalliance.voiplib.repository.initialise.LogLevel.*
import org.openvoipalliance.voiplib.repository.initialise.LogListener
import org.openvoipalliance.voiplibexample.logging.LogDao
import org.openvoipalliance.voiplibexample.logging.LogEntry
import org.openvoipalliance.voiplibexample.logging.RoomSingleton
import org.openvoipalliance.voiplibexample.ui.CallManager
import java.time.LocalDateTime
import java.util.*

class VoIPLibExampleApplication: Application(), LogListener {

    private val logDatabase: LogDao by lazy {
        RoomSingleton.getInstance(this).logDao()
    }

    override fun onCreate() {
        super.onCreate()

        callManager = CallManager(this)

        VoIPLib.getInstance(this).initialise(
                Config(
                        auth = authFromSharedPrefs(this),
                        callListener = callManager,
                        logListener = this,
                        advancedVoIPSettings = advancedSettingsFromPrefs(this),
                        encryption = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("encryption", true),
                        codecs = PreferenceManager.getDefaultSharedPreferences(this).getCodecs()
                )
        )
    }

    companion object {

        @SuppressLint("StaticFieldLeak")
        lateinit var callManager: CallManager

        fun authFromSharedPrefs(context: Context): Auth {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)

            return Auth(
                    prefs.getString("username", "")!!,
                    prefs.getString("password", "")!!,
                    prefs.getString("domain", "")!!,
                    prefs.getString("port", "0")!!.toInt(),
            )
        }

        fun advancedSettingsFromPrefs(context: Context): AdvancedVoIPSettings {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val defaults = AdvancedVoIPSettings()

            return AdvancedVoIPSettings(
                    echoCancellation = prefs.getBoolean("echo_cancellation", defaults.echoCancellation),
                    adaptiveRateControl = prefs.getBoolean("adaptive_rate_control", defaults.adaptiveRateControl),
                    jitterCompensation = prefs.getBoolean("jitter_compensation", defaults.jitterCompensation),
                    mtu = prefs.getString("mtu", defaults.mtu.toString())!!.toInt(),
                    adaptiveRateAlgorithm = AdaptiveRateAlgorithm.valueOf(prefs.getString("adaptive_rate_algorithm",
                            defaults.adaptiveRateAlgorithm.name.toLowerCase(Locale.getDefault()).capitalize(Locale.getDefault()))!!.toUpperCase(Locale.getDefault())
                    ),
                    mediaMultiplexing = prefs.getBoolean("media_multiplexing", defaults.mediaMultiplexing)
            )
        }
    }

    override fun onLogMessageWritten(lev: LogLevel, message: String) {
        val tag = "VoIPLibExampleApp"

        when(lev) {
            DEBUG -> Log.d(tag, message)
            TRACE -> Log.d(tag, message)
            MESSAGE -> Log.i(tag, message)
            WARNING -> Log.w(tag, message)
            ERROR -> Log.e(tag, message)
            FATAL -> Log.e(tag, message)
        }

        val toDisplay = arrayOf(MESSAGE, ERROR, FATAL, WARNING)
        val blacklist = arrayOf("wake_lock")

        if (!toDisplay.contains(lev)) return

        if (blacklist.any { message.contains(it, ignoreCase = true) }) return

        GlobalScope.launch(Dispatchers.IO) {
            logDatabase.save(LogEntry(
                    id = null,
                    datetime = LocalDateTime.now().toLocalTime().toString(),
                    message = message
            ))
        }
    }

}

fun SharedPreferences.getCodecs(): Array<Codec> {
    val codecs: List<Codec> = getStringSet("codecs", mutableSetOf(OPUS.name))!!.map {
        Codec.valueOf(it)
    }

    return codecs.toTypedArray()
}