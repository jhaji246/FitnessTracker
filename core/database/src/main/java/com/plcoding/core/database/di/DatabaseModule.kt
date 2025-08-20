package com.avi.core.database.di

import androidx.room.Room
import com.avi.core.database.RoomLocalRunDataSource
import com.avi.core.database.RunDatabase
import com.avi.core.domain.run.LocalRunDataSource
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidApplication(),
            RunDatabase::class.java,
            "run.db"
        ).build()
    }
    single { get<RunDatabase>().runDao }
    single { get<RunDatabase>().runPendingSyncDao }

    singleOf(::RoomLocalRunDataSource).bind<LocalRunDataSource>()
}