package com.tk.quicksearch.shared.util

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

object CrashLogManager {
    private const val LOG_DIR = "crash_logs"
    private const val LOG_FILE = "logs.txt"
    private const val CRASH_FILE = "crashes.txt"
    private const val MAX_CRASH_SIZE_BYTES = 256 * 1024 // 256 KB
    private const val OOM_STACK_OMITTED = "[stack trace omitted: low memory]\n"
    @Volatile
    private var isInstalled = false
    private val isHandlingCrash = AtomicBoolean(false)

    @Synchronized
    fun install(context: Context) {
        if (isInstalled) return

        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (isHandlingCrash.compareAndSet(false, true)) {
                try {
                    writeCrashEntry(appContext, thread, throwable)
                } catch (_: Throwable) {
                    // Never let the crash handler itself crash, especially while handling OOM.
                }
            }

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(10)
            }
        }
        isInstalled = true
    }

    /**
     * Captures current logcat output for this app's process and writes it to the log file,
     * appending any previously recorded crash entries at the end.
     * Always returns a non-null file suitable for attaching to a feedback email.
     */
    fun getOrCreateLogFile(context: Context): File {
        val dir = File(context.filesDir, LOG_DIR).also { it.mkdirs() }
        val file = File(dir, LOG_FILE)

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val androidVersion = Build.VERSION.RELEASE
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val pid = android.os.Process.myPid()

        file.bufferedWriter().use { out ->
            out.write("=== Quick Search Logs ===\n")
            out.write("Captured: $timestamp\n")
            out.write("Android: $androidVersion  Device: $deviceModel\n")
            out.write("PID: $pid\n\n")

            // Dump logcat for this process only
            try {
                val args = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    arrayOf("logcat", "-d", "--pid=$pid", "-v", "time")
                } else {
                    // --pid not supported before API 24; fall back to full buffer
                    arrayOf("logcat", "-d", "-v", "time")
                }
                val process = Runtime.getRuntime().exec(args)
                process.inputStream.bufferedReader().use { reader ->
                    reader.lineSequence().forEach { line ->
                        out.write(line)
                        out.newLine()
                    }
                }
                process.waitFor()
            } catch (_: Exception) {
                out.write("[logcat unavailable]\n")
            }

            // Append any recorded crash stack traces
            val crashFile = File(dir, CRASH_FILE)
            if (crashFile.exists() && crashFile.length() > 0) {
                out.write("\n=== Recorded Crashes ===\n")
                crashFile.bufferedReader().use { it.copyTo(out) }
            }
        }

        return file
    }

    private fun writeCrashEntry(context: Context, thread: Thread, throwable: Throwable) {
        val dir = File(context.filesDir, LOG_DIR).also { it.mkdirs() }
        val file = File(dir, CRASH_FILE)

        if (file.exists() && file.length() > MAX_CRASH_SIZE_BYTES) {
            file.delete()
        }

        FileOutputStream(file, true).use { stream ->
            if (throwable.hasOutOfMemoryCause()) {
                writeLowMemoryCrashEntry(stream, thread, throwable)
            } else {
                OutputStreamWriter(stream).use { out ->
                    out.append("--- Crash ")
                        .append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                        .append(" ---\n")
                    out.append("Thread: ").append(thread.name).append('\n')
                    out.append("Exception: ")
                        .append(throwable::class.java.name)
                        .append(": ")
                        .append(throwable.message.orEmpty())
                        .append('\n')
                    throwable.printStackTrace(PrintWriter(out))
                    out.append('\n')
                }
            }
        }
    }

    private fun Throwable.hasOutOfMemoryCause(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is OutOfMemoryError) return true
            current = current.cause
        }
        return false
    }

    private fun writeLowMemoryCrashEntry(
        stream: FileOutputStream,
        thread: Thread,
        throwable: Throwable,
    ) {
        stream.writeAscii("--- Crash epochMillis=")
        stream.writeAscii(System.currentTimeMillis().toString())
        stream.writeAscii(" ---\n")
        stream.writeAscii("Thread: ")
        stream.writeAscii(thread.name)
        stream.writeAscii("\n")
        stream.writeThrowableSummary("Exception", throwable)

        var cause = throwable.cause
        var causeIndex = 1
        while (cause != null && causeIndex <= 4) {
            stream.writeThrowableSummary("Cause $causeIndex", cause)
            cause = cause.cause
            causeIndex++
        }

        stream.writeAscii(OOM_STACK_OMITTED)
        stream.writeAscii("\n")
    }

    private fun FileOutputStream.writeThrowableSummary(label: String, throwable: Throwable) {
        writeAscii(label)
        writeAscii(": ")
        writeAscii(throwable::class.java.name)
        val message = throwable.message
        if (!message.isNullOrEmpty()) {
            writeAscii(": ")
            writeAscii(message)
        }
        writeAscii("\n")
    }

    private fun FileOutputStream.writeAscii(value: String) {
        for (index in value.indices) {
            write(value[index].code)
        }
    }
}
