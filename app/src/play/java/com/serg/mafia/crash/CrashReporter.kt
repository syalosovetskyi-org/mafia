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
            FirebaseCrashlytics.getInstance().recordException(scrub(throwable))
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to report exception to Firebase Crashlytics", e)
        }
    }

    /**
     * В Crashlytics уходят только класс исключения и стек: сообщения исключений
     * (пути файлов, имена игроков, ответы плееров) остаются в локальном логе.
     */
    internal fun scrub(failure: Throwable, depth: Int = 0): Throwable {
        val scrubbed = ScrubbedException(failure.javaClass.name)
        scrubbed.stackTrace = failure.stackTrace
        val cause = failure.cause
        if (cause != null && cause !== failure && depth < 8) scrubbed.initCause(scrub(cause, depth + 1))
        return scrubbed
    }

    internal class ScrubbedException(originalClass: String) : Exception(originalClass)

    fun log(message: String) {
        Log.i(TAG, message)
        try {
            FirebaseCrashlytics.getInstance().log(message)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to send log to Firebase Crashlytics", e)
        }
    }
}
