package com.serg.mafia.model

/**
 * Чистая логика обратного отсчёта речи с возможностью инъекции системных часов для тестирования.
 */
class CountdownTimer(
    val totalSeconds: Int,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val onFinished: () -> Unit = {},
) {
    var remainingSeconds: Int = totalSeconds
        private set
    var isRunning: Boolean = false
        private set
    var isFinished: Boolean = false
        private set

    private var lastTickMs: Long = 0L

    fun start() {
        if (remainingSeconds <= 0 || isRunning) return
        isRunning = true
        lastTickMs = clock()
    }

    fun pause() {
        isRunning = false
    }

    fun toggle() {
        if (isRunning) pause() else start()
    }

    fun reset() {
        isRunning = false
        remainingSeconds = totalSeconds
        isFinished = false
        lastTickMs = 0L
    }

    /**
     * Вызывается при тике часов (или периодическом цикле). Возвращает true, если секунда изменилась.
     */
    fun tick(): Boolean {
        if (!isRunning || remainingSeconds <= 0) return false
        val now = clock()
        val elapsedSec = (now - lastTickMs) / 1000
        if (elapsedSec >= 1) {
            val step = elapsedSec.toInt().coerceAtMost(remainingSeconds)
            remainingSeconds -= step
            lastTickMs += step * 1000L
            if (remainingSeconds == 0) {
                isRunning = false
                isFinished = true
                onFinished()
            }
            return true
        }
        return false
    }

    fun step(seconds: Int = 1) {
        if (!isRunning || remainingSeconds <= 0) return
        remainingSeconds = (remainingSeconds - seconds).coerceAtLeast(0)
        if (remainingSeconds == 0) {
            isRunning = false
            isFinished = true
            onFinished()
        }
    }
}
