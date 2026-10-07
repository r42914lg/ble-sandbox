package com.r42914lg.blesandbox

import org.koin.dsl.module

class AppInitializer {
    val module = module {
        single<CoreApp> { BleSandboxApp() }
    }
}
