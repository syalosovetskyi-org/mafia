package com.serg.mafia.crash

import android.content.Context
import android.util.Log

/**
 * Реализация для flavor personal.
 * Не содержит зависимостей Firebase и не отправляет данные на внешние серверы.
 */
object CrashReporter {
    private const val TAG = "CrashReporter"

    fun init(context: Context) {
        // В personal Firebase не инициализируется вообще
    }

    fun recordException(throwable: Throwable) {
        Log.w(TAG, "Caught exception: ${throwable.message}", throwable)
    }

    fun log(message: String) {
        Log.i(TAG, message)
    }
}
