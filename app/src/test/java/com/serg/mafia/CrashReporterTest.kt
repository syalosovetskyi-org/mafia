package com.serg.mafia

import com.serg.mafia.crash.CrashReporter
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReporterTest {

    @Test
    fun `CrashReporter успешно принимает исключения и логи без падений`() {
        var completed = false
        try {
            CrashReporter.log("Test log message")
            CrashReporter.recordException(IllegalStateException("Unit test sample exception"))
            completed = true
        } catch (t: Throwable) {
            completed = false
        }
        assertTrue("CrashReporter должен безопасно перехватывать и логировать вызовы", completed)
    }

    @Test
    fun `вызовы recordException с различными типами исключений безопасны`() {
        CrashReporter.recordException(NullPointerException("NPE test"))
        CrashReporter.recordException(RuntimeException("Runtime test"))
        CrashReporter.recordException(Exception("Checked exception test"))
    }
}
