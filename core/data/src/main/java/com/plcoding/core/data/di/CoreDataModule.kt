package com.avi.core.data.di

import com.avi.core.data.analytics.AnalyticsService
import com.avi.core.data.auth.EncryptedSessionStorage
import com.avi.core.data.ble.BleServiceImpl
import com.avi.core.data.ble.TelemetryDataProcessor
import com.avi.core.data.firebase.FirebaseService
import com.avi.core.data.jni.NativePerformanceBridge
import com.avi.core.data.networking.HttpClientFactory
import com.avi.core.data.run.OfflineFirstRunRepository
import com.avi.core.domain.SessionStorage
import com.avi.core.domain.ble.BleService
import com.avi.core.domain.run.RunRepository
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreDataModule = module {
    // Session Storage
    singleOf(::EncryptedSessionStorage)
    single<SessionStorage> { get<EncryptedSessionStorage>() }
    
    // Network Client
    singleOf(::HttpClientFactory)
    single<HttpClient> { get<HttpClientFactory>().build() }
    
    // Run Repository
    singleOf(::OfflineFirstRunRepository)
    single<RunRepository> { get<OfflineFirstRunRepository>() }
    
    // BLE Services
    singleOf(::BleServiceImpl)
    single<BleService> { get<BleServiceImpl>() }
    singleOf(::TelemetryDataProcessor)
    
    // Firebase and Analytics Services
    singleOf(::FirebaseService)
    singleOf(::AnalyticsService)
    
    // Performance and Native Bridge
    singleOf(::NativePerformanceBridge)
}