package com.decisionpro.app.util.crash

import android.util.Log

interface CrashReporter {
    fun recordException(e: Throwable, tag: String = "general")
    fun log(message: String)
    fun setTag(key: String, value: String)
}

class DebugCrashReporter : CrashReporter {
    override fun recordException(e: Throwable, tag: String) {
        Log.e("DecisionPro/$tag", "Recorded exception", e)
    }
    override fun log(message: String) = Log.d("DecisionPro", message)
    override fun setTag(key: String, value: String) =
        Log.d("DecisionPro", "customKey: $key=$value")
}

/** Forward to Firebase Crashlytics (or your SDK of choice) in release. */
class ReleaseCrashReporter : CrashReporter {
    override fun recordException(e: Throwable, tag: String) { /* SDK hookup */ }
    override fun log(message: String) { /* SDK hookup */ }
    override fun setTag(key: String, value: String) { /* SDK hookup */ }
}
