package dev.sora.protohax

import android.annotation.SuppressLint
import android.app.Application
import dev.sora.protohax.relay.netty.log.NettyLoggerFactory
import dev.sora.protohax.relay.service.AppService
import dev.sora.protohax.ui.overlay.OverlayManager
import io.netty.util.internal.logging.InternalLoggerFactory
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

InternalLoggerFactory.setDefaultFactory(NettyLoggerFactory())

density = resources.displayMetrics.density

        instance = this

        setupCrashHandler()
        loadPreviousCrashIfAny()
    }

    private fun setupCrashHandler() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                crashLogFile().writeText(
                    "=== CRASH (${java.util.Date()}) on thread ${thread.name} ===\n$sw\n"
                )
            } catch (ignored: Throwable) {
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun loadPreviousCrashIfAny() {
        val file = crashLogFile()
        if (file.exists()) {
            val content = file.readText()
            if (content.isNotBlank()) {
                InternalLoggerFactory.getInstance("CrashHandler").error(
                    "phat hien crash tu lan chay truoc, chi tiet ben duoi:\n$content"
                )
            }
            file.delete()
        }
    }

    private fun crashLogFile() = File(filesDir, "last_crash.txt")

    companion object {
        lateinit var instance: MyApplication
            private set

var density: Float = 1f
private set

@SuppressLint("StaticFieldLeak")
val overlayManager = OverlayManager().also {
AppService.addListener(it)
}
    }
}
