package org.openvoipalliance.voiplibexample.ui.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.text.InputType
import android.util.Log
import androidx.preference.*
import org.openvoipalliance.voiplib.VoIPLib
import org.openvoipalliance.voiplib.config.AdvancedVoIPSettings
import org.openvoipalliance.voiplib.config.Auth
import org.openvoipalliance.voiplib.model.Codec
import org.openvoipalliance.voiplib.model.RegistrationState
import org.openvoipalliance.voiplibexample.R
import org.openvoipalliance.voiplibexample.VoIPLibExampleApplication
import org.openvoipalliance.voiplibexample.getCodecs
import java.util.*

class SettingsFragment : PreferenceFragmentCompat() {

    private val prefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(activity)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        loadFromFile()
        initializeAdvancedDefaults()
        setPreferencesFromResource(R.xml.settings, rootKey)

        findPreference<EditTextPreference>("username")?.summaryProvider = Preference.SummaryProvider<EditTextPreference> {
            prefs.getString("username", "")
        }

        findPreference<EditTextPreference>("password")?.apply {
            setOnBindEditTextListener {
                it.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            summaryProvider = Preference.SummaryProvider<EditTextPreference> {
                prefs.getString(
                    "password",
                    ""
                )
            }
        }

        findPreference<EditTextPreference>("domain")?.apply {
            setOnBindEditTextListener {
                it.inputType = InputType.TYPE_TEXT_VARIATION_URI
            }
            summaryProvider = Preference.SummaryProvider<EditTextPreference> {
                prefs.getString(
                    "domain",
                    ""
                )
            }
        }

        findPreference<EditTextPreference>("port")?.apply {
            setOnBindEditTextListener {
                it.inputType = InputType.TYPE_CLASS_NUMBER
            }
            summaryProvider = Preference.SummaryProvider<EditTextPreference> {
                prefs.getString(
                    "port",
                    ""
                )
            }
        }

        findPreference<MultiSelectListPreference>("codecs")?.apply {
            summaryProvider = Preference.SummaryProvider<MultiSelectListPreference> {
                "${prefs.getCodecs().size} selected"
            }
        }

        arrayOf("username", "password", "domain", "port", "encryption", "jitter_compensation", "echo_cancellation",
                "adaptive_rate_control", "mtu", "adaptive_rate_algorithm", "media_multiplexing", "codecs").forEach {
            findPreference<Preference>(it)?.setOnPreferenceChangeListener { _, _ ->
                Handler().postDelayed(
                    {
                        activity?.runOnUiThread { updateAuthenticationStatus() }
                    },
                    1000
                )
                true
            }
        }

        findPreference<Preference>("status")?.setOnPreferenceClickListener {
            updateAuthenticationStatus()
            true
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun initializeAdvancedDefaults() {
        val defaults = AdvancedVoIPSettings()

        val keyToDefault = mapOf<String, Any>(
                "jitter_compensation" to defaults.jitterCompensation,
                "echo_cancellation" to defaults.echoCancellation,
                "adaptive_rate_control" to defaults.adaptiveRateControl,
                "mtu" to defaults.mtu,
                "adaptive_rate_algorithm" to defaults.adaptiveRateAlgorithm.name,
                "media_multiplexing" to defaults.mediaMultiplexing
        )

        keyToDefault.forEach {
            if (!prefs.contains(it.key)) {
                when (it.value) {
                    is Boolean -> prefs.edit().putBoolean(it.key, it.value as Boolean).commit()
                    is String -> {
                        val value = (it.value as String).toLowerCase(Locale.getDefault())
                        prefs.edit().putString(it.key, value.capitalize(Locale.getDefault())).commit()
                    }
                    is Int -> prefs.edit().putString(it.key, (it.value as Int).toString()).commit()
                }
            }
        }

        if (!prefs.contains("codecs")) {
            prefs.edit().putStringSet("codecs", mutableSetOf(Codec.OPUS.name)).commit()
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun loadFromFile() {
        val keyToDefault = mapOf(
            "username" to getString(R.string.default_sip_user),
            "password" to getString(R.string.default_sip_password),
            "domain" to getString(R.string.default_sip_domain),
            "port" to getString(R.string.default_sip_port)
        )

        keyToDefault.forEach {
            if (prefs.getString(it.key, "")!!.isEmpty()) {
                prefs.edit().putString(it.key, it.value).commit()
            }
        }
    }

    /**
     * Updates the authentication status field.
     *
     */
    private fun updateAuthenticationStatus() {
        findPreference<Preference>("status")?.summaryProvider = Preference.SummaryProvider<Preference> {
            "Trying..."
        }

        val voIPLib = VoIPLib.getInstance(requireContext())

        voIPLib.refreshConfig(
                voIPLib.currentConfig.copy(
                        auth = VoIPLibExampleApplication.authFromSharedPrefs(requireContext()),
                        advancedVoIPSettings = VoIPLibExampleApplication.advancedSettingsFromPrefs(requireContext()),
                        encryption = prefs.getBoolean("encryption", true),
                        codecs = prefs.getCodecs()
                )
        )

        voIPLib.register {
            if (it == RegistrationState.REGISTERED || it == RegistrationState.FAILED) {
                val status = if (it == RegistrationState.REGISTERED) "Authenticated" else "Authentication failed"

                activity?.runOnUiThread {
                    findPreference<Preference>("status")?.summaryProvider = Preference.SummaryProvider<Preference> {
                        status
                    }
                }
            }


        }
    }

    override fun onResume() {
        super.onResume()
        updateAuthenticationStatus()
    }
}
