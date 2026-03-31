package com.tk.quicksearch.shared.util

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogManager {
    private const val LOG_DIR = "crash_logs"
    private const val LOG_FILE = "logs.txt"
    private const val CRASH_FILE = "crashes.txt"
    private const val MAX_CRASH_SIZE_BYTES = 256 * 1024 // 256 KB

    fun install(context: Context) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrashEntry(appContext, thread, throwable)
            } catch (_: Exception) {
                // Never let the crash handler itself crash
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
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

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()

        file.appendText(
            buildString {
                append("--- Crash $timestamp ---\n")
                append("Thread: ${thread.name}\n")
                append("Exception: ${throwable::class.java.name}: ${throwable.message}\n")
                append(stackTrace)
                append("\n")
            }
        )
    }
}
