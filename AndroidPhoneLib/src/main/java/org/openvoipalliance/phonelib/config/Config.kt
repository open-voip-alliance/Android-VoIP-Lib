package org.openvoipalliance.phonelib.config

import org.openvoipalliance.phonelib.model.Codec
import org.openvoipalliance.phonelib.repository.initialise.LogListener

data class Config(
    val auth: Auth,
    val encryption: Boolean = true,
    val stun: String? = null,
    val ring: String? = null,
    val logListener: LogListener? = null,
    val codecs: Array<Codec> = arrayOf(Codec.G722, Codec.G729, Codec.GSM, Codec.ILBC, Codec.ISAC, Codec.L16, Codec.OPUS, Codec.PCMA, Codec.PCMU, Codec.SPEEX),
    val userAgent: String = "AndroidPhoneLib"
)