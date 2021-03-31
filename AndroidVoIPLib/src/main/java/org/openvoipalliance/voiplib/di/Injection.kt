package org.openvoipalliance.voiplib.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.dsl.koinApplication

internal class Injection(private val context: Context) : VoIPLibKoinComponent {
    init {
        voipLibKoin = koinApplication {
            androidContext(context)
            modules(listOf(repositoryModule))
        }
    }

    companion object {
        var voipLibKoin: KoinApplication? = null
    }
}

interface VoIPLibKoinComponent : KoinComponent {
    @KoinApiExtension
    override fun getKoin(): Koin {
        return Injection.voipLibKoin?.koin!!
    }
}