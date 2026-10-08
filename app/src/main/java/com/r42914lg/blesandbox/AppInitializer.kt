package com.r42914lg.blesandbox

import com.r42914lg.blesandbox.mvi.MainViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class AppInitializer {
    val module = module {
        single<CoreApp> { BleSandboxApp() }
        viewModel { MainViewModel(get(), get()) }
    }
}
