package com.decisionpro.app.di

import android.content.Context
import androidx.room.Room
import com.decisionpro.app.data.local.DecisionDao
import com.decisionpro.app.data.local.DecisionDatabase
import com.decisionpro.app.data.local.Migrations
import com.decisionpro.app.util.crash.CrashReporter
import com.decisionpro.app.util.crash.DebugCrashReporter
import com.decisionpro.app.util.crash.ReleaseCrashReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DecisionDatabase =
        Room.databaseBuilder(context, DecisionDatabase::class.java, DecisionDatabase.DB_NAME)
            .addMigrations(*Migrations.ALL)
            // Intentionally NO fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDecisionDao(db: DecisionDatabase): DecisionDao = db.decisionDao()

    @Provides
    @Singleton
    fun provideCrashReporter(): CrashReporter =
        if (com.decisionpro.app.BuildConfig.DEBUG) DebugCrashReporter()
        else ReleaseCrashReporter()

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
