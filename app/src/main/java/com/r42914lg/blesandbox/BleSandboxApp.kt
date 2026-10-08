package com.r42914lg.blesandbox

import android.app.Application
import android.content.Context
import com.r42914lg.blesandbox.blewrapper.di.BleInitializer
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.loadKoinModules
import org.koin.core.context.startKoin

interface CoreApp {
    fun getApplicationContext(): Context
}

class BleSandboxApp : Application(), CoreApp {
    override fun onCreate() {
        super.onCreate()
        buildGraph()
    }

    private fun buildGraph() {
        startKoin {
            androidContext(getApplicationContext())
            loadKoinModules(
                listOf(
                    AppInitializer().module,
                    BleInitializer().module
                )
            )
        }
    }
}

