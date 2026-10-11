package com.decisionpro.app

import android.app.Application
import android.os.StrictMode
import com.decisionpro.app.util.crash.CrashReporter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DecisionProApp : Application() {

    @Inject lateinit var crashReporter: CrashReporter

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) enableStrictMode()

        // Report fatal crashes, then DELEGATE to the platform handler —
        // never swallow fatal errors.
        val platformHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                crashReporter.recordException(throwable, tag = "fatal.${thread.name}")
            }
            platformHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads().detectDiskWrites().detectNetwork()
                .detectCustomSlowCalls()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .build()
        )
    }
}
