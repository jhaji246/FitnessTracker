package com.avi.run.presentation.di

import com.avi.run.domain.RunningTracker
import com.avi.run.presentation.active_run.ActiveRunViewModel
import com.avi.run.presentation.run_overview.RunOverviewViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val runPresentationModule = module {
    singleOf(::RunningTracker)
    single {
        get<RunningTracker>().elapsedTime
    }

    viewModelOf(::RunOverviewViewModel)
    viewModelOf(::ActiveRunViewModel)
}