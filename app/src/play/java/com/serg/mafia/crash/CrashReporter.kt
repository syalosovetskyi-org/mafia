package com.serg.mafia.crash

import android.content.Context
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.serg.mafia.BuildConfig

/**
 * Реализация для flavor play.
 * Отправляет ошибки в Firebase Crashlytics только в release-сборке.
 */
object CrashReporter {
    private const val TAG = "CrashReporter"

    fun init(context: Context) {
        try {
            // Включен сбор только в release: setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to initialize Crashlytics collection state", e)
        }
    }

    fun recordException(throwable: Throwable) {
        Log.w(TAG, "Caught exception: ${throwable.message}", throwable)
        try {
            FirebaseCrashlytics.getInstance().recordException(throwable)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to report exception to Firebase Crashlytics", e)
        }
    }

    fun log(message: String) {
        Log.i(TAG, message)
        try {
            FirebaseCrashlytics.getInstance().log(message)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to send log to Firebase Crashlytics", e)
        }
    }
}
