package com.r42914lg.blesandbox

import android.app.Application
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.r42914lg.blesandbox.blewrapper.di.BleInitializer
import com.r42914lg.blesandbox.coredata.di.CoreDataInitializer
import com.r42914lg.blesandbox.tempgraph.di.TempGraphInitializer
import com.r42914lg.blesandbox.worker.CleanupTemperatureWorker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.loadKoinModules
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit

interface CoreApp {
    fun getApplicationContext(): Context
}

class BleSandboxApp : Application(), CoreApp {
    override fun onCreate() {
        super.onCreate()
        buildGraph()
        scheduleCleanupWorker()
    }

    private fun buildGraph() {
        startKoin {
            androidContext(getApplicationContext())
            loadKoinModules(
                listOf(
                    AppInitializer().module,
                    BleInitializer().module,
                    CoreDataInitializer().module,
                    TempGraphInitializer().module
                )
            )
        }
    }

    private fun scheduleCleanupWorker() {
        val cleanupWorkRequest = PeriodicWorkRequestBuilder<CleanupTemperatureWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "cleanup_old_temperatures_work",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupWorkRequest
        )
    }
}
