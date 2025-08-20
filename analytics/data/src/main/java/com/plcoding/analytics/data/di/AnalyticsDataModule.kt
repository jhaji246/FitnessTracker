package com.avi.analytics.data.di

import com.avi.analytics.data.RoomAnalyticsRepository
import com.avi.analytics.domain.AnalyticsRepository
import com.avi.core.database.RunDatabase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val analyticsModule = module {
    singleOf(::RoomAnalyticsRepository).bind<AnalyticsRepository>()
    single {
        get<RunDatabase>().analyticsDao
    }
}