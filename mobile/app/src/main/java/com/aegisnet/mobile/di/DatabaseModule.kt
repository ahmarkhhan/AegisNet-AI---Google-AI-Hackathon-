package com.aegisnet.mobile.di

import android.content.Context
import androidx.room.Room
import com.aegisnet.mobile.data.local.AegisDatabase
import com.aegisnet.mobile.data.local.IncidentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AegisDatabase {
        return Room.databaseBuilder(
            context,
            AegisDatabase::class.java,
            "aegis_crisis_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideIncidentDao(database: AegisDatabase): IncidentDao {
        return database.incidentDao
    }
}
