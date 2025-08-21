package com.avi.fitnesstracker

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.avi.auth.data.di.authDataModule
import com.avi.auth.presentation.di.authViewModelModule
import com.avi.core.connectivity.data.di.coreConnectivityDataModule
import com.avi.core.data.di.coreDataModule
import com.avi.core.database.di.databaseModule
import com.avi.run.data.di.runDataModule
import com.avi.run.location.di.locationModule
import com.avi.run.network.di.networkModule
import com.avi.run.presentation.di.runPresentationModule
import com.avi.fitnesstracker.di.appModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class FitnessTrackerApp: Application() {
    
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize WorkManager
        val workManagerConfiguration = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
        WorkManager.initialize(this, workManagerConfiguration)
        
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@FitnessTrackerApp)
            modules(
                appModule,
                authDataModule,
                authViewModelModule,
                coreDataModule,
                databaseModule,
                runDataModule,
                locationModule,
                networkModule,
                runPresentationModule,
                coreConnectivityDataModule
            )
        }
    }
}