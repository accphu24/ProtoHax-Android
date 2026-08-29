package dev.sora.protohax.relay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import dev.sora.protohax.MyApplication
import dev.sora.protohax.R
import dev.sora.protohax.relay.ManualRelayConfig
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.ui.activities.MainActivity
import dev.sora.relay.utils.logError
import dev.sora.relay.utils.logInfo

/**
 * Runs the relay in "manual" mode: a plain RakNet server bound to a real UDP
 * port, no VpnService/TUN involved. The user manually enters this device's
 * LAN IP + [ManualRelayConfig.relayPort] as the server address inside
 * Minecraft, and the relay forwards traffic to the configured target server.
 */
class ManualRelayService : Service() {

	override fun onBind(intent: Intent?) = null

	override fun onCreate() {
		val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
		if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
			notificationManager.createNotificationChannel(
				NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW)
			)
		}
		MyApplication.overlayManager.currentContext = this
	}

	override fun onDestroy() {
		logInfo("manual relay service destroyed")
		stopManualRelay()
		MyApplication.overlayManager.currentContext = null
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		intent ?: return START_NOT_STICKY
		try {
			when (intent.action) {
				ACTION_START -> {
					startForeground(1, createNotification())
					try {
						MinecraftRelay.announceManualRelayUp()
						isActive = true
						serviceListeners.forEach { it.onServiceStarted() }
					} catch (t: Throwable) {
						// bind failed (port in use, permission denied, etc) - do NOT
						// leave a "connected" notification lying to the user
						logError("manual relay failed to bind", t)
						stopManualRelay()
						stopForeground(STOP_FOREGROUND_REMOVE)
						stopSelf()
					}
				}
				else -> {
					stopManualRelay()
					stopForeground(STOP_FOREGROUND_REMOVE)
					stopSelf()
				}
			}
		} catch (t: Throwable) {
			logError("manual relay command", t)
		}
		return super.onStartCommand(intent, flags, startId)
	}

	private fun stopManualRelay() {
		isActive = false
		MinecraftRelay.stopManualRelay()
		serviceListeners.forEach { it.onServiceStopped() }
	}

	private fun createNotification(): Notification {
		val flag = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
		val intent = Intent(this, MainActivity::class.java)
		intent.addCategory(Intent.CATEGORY_LAUNCHER)
		intent.action = Intent.ACTION_MAIN
		val pendingIntent = PendingIntent.getActivity(this, 0, intent, flag)

		val stopIntent = Intent(ACTION_STOP)
		stopIntent.setPackage(packageName)
		val pendingIntentStop = PendingIntent.getForegroundService(this, 1, stopIntent, flag)

		return NotificationCompat.Builder(this, CHANNEL_ID)
			.setContentTitle(getString(R.string.app_name))
			.setContentText(getString(R.string.manual_relay_notification, ManualRelayConfig.relayPort))
			.setSmallIcon(R.drawable.notification_icon)
			.setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
			.setOngoing(true)
			.setContentIntent(pendingIntent)
			.addAction(R.drawable.notification_icon, getString(R.string.dashboard_fab_disconnect), pendingIntentStop)
			.build()
	}

	companion object {
		const val ACTION_START = "dev.sora.protohax.manualrelay.start"
		const val ACTION_STOP = "dev.sora.protohax.manualrelay.stop"
		const val CHANNEL_ID = "dev.sora.protohax.MANUAL_RELAY_CHANNEL_ID"

		var isActive = false
		private val serviceListeners = mutableSetOf<ServiceListener>()

		fun addListener(listener: ServiceListener) {
			serviceListeners.add(listener)
		}

		fun removeListener(listener: ServiceListener) {
			serviceListeners.remove(listener)
		}
	}
}
