package com.vextreme.vexlife.android

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class VexLifeAndroidApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@VexLifeAndroidApplication)
            modules(vexLifeAndroidModule)
        }
    }
}

// [VXG RealForever]
