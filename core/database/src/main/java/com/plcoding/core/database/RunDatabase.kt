package com.avi.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.avi.core.database.dao.AnalyticsDao
import com.avi.core.database.dao.RunDao
import com.avi.core.database.dao.RunPendingSyncDao
import com.avi.core.database.entity.DeletedRunSyncEntity
import com.avi.core.database.entity.RunEntity
import com.avi.core.database.entity.RunPendingSyncEntity

@Database(
    entities = [
        RunEntity::class,
        RunPendingSyncEntity::class,
        DeletedRunSyncEntity::class
    ],
    version = 1
)
abstract class RunDatabase : RoomDatabase() {

    abstract val runDao: RunDao
    abstract val runPendingSyncDao: RunPendingSyncDao
    abstract val analyticsDao: AnalyticsDao
}