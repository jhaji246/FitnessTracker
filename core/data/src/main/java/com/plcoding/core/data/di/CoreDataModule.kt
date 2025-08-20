package com.avi.core.data.di

import android.content.SharedPreferences
import com.avi.core.data.auth.EncryptedSessionStorage
import com.avi.core.data.networking.HttpClientFactory
import com.avi.core.data.run.OfflineFirstRunRepository
import com.avi.core.domain.SessionStorage
import com.avi.core.domain.run.RunRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreDataModule = module {
    single {
        HttpClientFactory(get()).build()
    }
    singleOf(::EncryptedSessionStorage).bind<SessionStorage>()

    singleOf(::OfflineFirstRunRepository).bind<RunRepository>()
}