package com.avi.analytics.presentation.di

import com.avi.analytics.presentation.AnalyticsDashboardViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val analyticsPresentationModule = module {
    viewModelOf(::AnalyticsDashboardViewModel)
}