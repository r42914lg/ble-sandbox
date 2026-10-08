package com.r42914lg.blesandbox.blewrapper.di

import com.r42914lg.blesandbox.blewrapper.DeviceConnection
import com.r42914lg.blesandbox.blewrapper.DeviceConnectionImpl
import org.koin.dsl.module

class BleInitializer {
    val module = module {
        single<DeviceConnection> { DeviceConnectionImpl(context = get()) }
    }
}
