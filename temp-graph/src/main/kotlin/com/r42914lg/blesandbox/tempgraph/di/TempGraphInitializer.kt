package com.r42914lg.blesandbox.tempgraph.di

import com.r42914lg.blesandbox.tempgraph.TempGraphViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class TempGraphInitializer {
    val module = module {
        viewModel { TempGraphViewModel(get()) }
    }
}
