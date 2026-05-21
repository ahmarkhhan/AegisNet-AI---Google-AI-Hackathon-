package com.aegisnet.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [IncidentEntity::class], version = 2, exportSchema = false)
abstract class AegisDatabase : RoomDatabase() {
    abstract val incidentDao: IncidentDao
}
