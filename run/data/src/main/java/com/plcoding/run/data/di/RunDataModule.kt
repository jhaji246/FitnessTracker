package com.avi.run.data.di

import com.avi.core.domain.run.SyncRunScheduler
import com.avi.run.data.CreateRunWorker
import com.avi.run.data.DeleteRunWorker
import com.avi.run.data.FetchRunsWorker
import com.avi.run.data.SyncRunWorkerScheduler
import com.avi.run.data.connectivity.PhoneToWatchConnector
import com.avi.run.domain.WatchConnector
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val runDataModule = module {
    workerOf(::CreateRunWorker)
    workerOf(::FetchRunsWorker)
    workerOf(::DeleteRunWorker)

    singleOf(::SyncRunWorkerScheduler).bind<SyncRunScheduler>()
    singleOf(::PhoneToWatchConnector).bind<WatchConnector>()
}