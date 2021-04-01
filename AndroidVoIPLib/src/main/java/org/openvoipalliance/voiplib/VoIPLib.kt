package org.openvoipalliance.voiplib

import android.Manifest.permission.RECORD_AUDIO
import android.content.Context
import androidx.annotation.RequiresPermission
import org.koin.core.component.inject
import org.openvoipalliance.voiplib.config.Config
import org.openvoipalliance.voiplib.di.Injection
import org.openvoipalliance.voiplib.model.Call
import org.openvoipalliance.voiplib.repository.call.controls.SipActiveCallControlsRepository
import org.openvoipalliance.voiplib.repository.call.session.SipSessionRepository
import org.openvoipalliance.voiplib.repository.initialise.SipInitialiseRepository
import org.openvoipalliance.voiplib.repository.registration.RegistrationCallback
import org.openvoipalliance.voiplib.repository.registration.SipRegisterRepository

class VoIPLib private constructor(private val context: Context) {
    private val sipInitialiseRepository: SipInitialiseRepository by injection.inject()
    private val sipRegisterRepository: SipRegisterRepository by injection.inject()

    private val sipCallControlsRepository: SipActiveCallControlsRepository by injection.inject()
    private val sipSessionRepository: SipSessionRepository by injection.inject()


    /**
     * This needs to be called whenever this library needs to initialise. Without it, no other calls
     * can be done.
     */
    fun initialise(config: Config): VoIPLib {
        sipInitialiseRepository.initialise(config)
        return this
    }

    /**
     * Check to see if the phonelib is initialised and ready to make calls.
     *
     */
    val isInitialised: Boolean
        get() = sipInitialiseRepository.isInitialised()

    val isRegistered: Boolean
        get() = sipRegisterRepository.isRegistered()

    val isReady: Boolean
        get() = isInitialised && isRegistered

    /**
     * This registers your user on SIP. You need this before placing a call.
     *
     */
    fun register(callback: RegistrationCallback): VoIPLib {
        sipRegisterRepository.register(callback)
        return this
    }

    /**
     * Refreshes the configuration by destroying and then re-initialising the library.
     *
     */
    fun refreshConfig(config: Config) {
        destroy()
        initialise(config)
    }

    /**
     * Get the currently used config.
     *
     */
    val currentConfig by lazy { sipInitialiseRepository.currentConfig() }

    /**
     * This performs a direct swap of the current config without any restarts, not all
     * changes may take affect.
     *
     * It is recommended you use the copy function after getting the currentConfig.
     */
    fun swapConfig(config: Config) = sipInitialiseRepository.swapConfig(config)

    /**
     * Destroy this library completely.
     *
     */
    fun destroy() {
        unregister()
        sipInitialiseRepository.destroy()
    }

    /**
     * This unregisters your user on SIP.
     */
    fun unregister() = sipRegisterRepository.unregister()

    /**
     * This method audio calls a phone number
     * @param number the number dialed to
     * @return returns true when call succeeds, false when the number is an empty string or the
     * phone service isn't ready.
     */
    @RequiresPermission(RECORD_AUDIO)
    fun callTo(number: String) = sipSessionRepository.callTo(number)

    /**
     * A method that should be called when the application has received a push message and
     * is waking from the background.
     *
     */
    fun wake() = sipInitialiseRepository.wake()

    /**
     * Whether or not the microphone is currently muted.
     *
     * Set to TRUE to mute the microphone and prevent voice transmission.
     */
    var microphoneMuted
            get() = sipCallControlsRepository.isMicrophoneMuted()
            set(muted) = sipCallControlsRepository.setMicrophone(!muted)

    /**
     * Perform actions on the given call.
     *
     */
    fun actions(call: Call) = Actions(context, call)

    /**
     * Return the current version of the underlying voip library.
     *
     */
    fun version() = sipInitialiseRepository.version()

    companion object {
        private var instance: VoIPLib? = null
        internal lateinit var injection: Injection

        @JvmStatic
        fun getInstance(context: Context): VoIPLib {
            if (instance != null) return instance as VoIPLib

            injection = Injection(context)
            val voipLib = VoIPLib(context.applicationContext)
            instance = voipLib
            return voipLib
        }
    }
}