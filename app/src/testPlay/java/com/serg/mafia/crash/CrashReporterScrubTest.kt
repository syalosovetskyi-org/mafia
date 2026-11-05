package com.serg.mafia.crash

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CrashReporterScrubTest {

    @Test
    fun `scrub убирает сообщения по всей цепочке cause и сохраняет стек`() {
        val root = IllegalArgumentException("игрок Вася, файл /sdcard/secret.mp3")
        val top = IllegalStateException("обёртка с данными", root)

        val scrubbed = CrashReporter.scrub(top)

        assertEquals(IllegalStateException::class.java.name, scrubbed.message)
        assertArrayEquals(top.stackTrace, scrubbed.stackTrace)
        val cause = scrubbed.cause!!
        assertEquals(IllegalArgumentException::class.java.name, cause.message)
        assertArrayEquals(root.stackTrace, cause.stackTrace)
        assertNull(cause.cause)
        assertFalse(scrubbed.toString().contains("Вася"))
        assertFalse(cause.toString().contains("secret"))
    }

    @Test
    fun `scrub не уходит в бесконечность на длинной цепочке`() {
        var e: Throwable = RuntimeException("0")
        repeat(50) { e = RuntimeException("$it", e) }

        var depth = 0
        var cur: Throwable? = CrashReporter.scrub(e)
        while (cur != null) { depth++; cur = cur.cause }

        assertEquals(9, depth)
    }
}
