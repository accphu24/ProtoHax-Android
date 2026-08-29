package dev.sora.protohax.relay

import dev.sora.protohax.MyApplication
import dev.sora.protohax.util.ContextUtils.readStringOrDefault
import dev.sora.protohax.util.ContextUtils.writeString
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Configuration for "manual relay" mode: a plain RakNet server bound to a real
 * UDP port (no VpnService/TUN involved). The user manually points their
 * Minecraft client at this device's LAN IP + relayPort, and the relay
 * forwards everything to targetHost:targetPort.
 */
object ManualRelayConfig {

	private const val KEY_TARGET_HOST = "MANUAL_RELAY_TARGET_HOST"
	private const val KEY_TARGET_PORT = "MANUAL_RELAY_TARGET_PORT"
	private const val KEY_RELAY_PORT = "MANUAL_RELAY_LOCAL_PORT"

	const val DEFAULT_RELAY_PORT = 19132

	var targetHost: String
		get() = MyApplication.instance.readStringOrDefault(KEY_TARGET_HOST, "")
		set(value) = MyApplication.instance.writeString(KEY_TARGET_HOST, value)

	var targetPort: Int
		get() = MyApplication.instance.readStringOrDefault(KEY_TARGET_PORT, "19132").toIntOrNull() ?: 19132
		set(value) = MyApplication.instance.writeString(KEY_TARGET_PORT, value.toString())

	var relayPort: Int
		get() = MyApplication.instance.readStringOrDefault(KEY_RELAY_PORT, DEFAULT_RELAY_PORT.toString()).toIntOrNull() ?: DEFAULT_RELAY_PORT
		set(value) = MyApplication.instance.writeString(KEY_RELAY_PORT, value.toString())

	val isConfigured: Boolean
		get() = targetHost.isNotBlank()

	/**
	 * Best-effort local LAN IPv4 address, so the user knows what to type
	 * into Minecraft's "Add Server" screen. Falls back to null if nothing
	 * usable was found (e.g. no WiFi connection) - in that case the user
	 * should use 127.0.0.1 if Minecraft runs on the same device, or find
	 * their IP manually in the WiFi settings.
	 */
	fun findLocalIpAddress(): String? {
		return try {
			NetworkInterface.getNetworkInterfaces().asSequence()
				.filter { !it.isLoopback && it.isUp }
				.flatMap { it.inetAddresses.asSequence() }
				.filterIsInstance<Inet4Address>()
				.firstOrNull()
				?.hostAddress
		} catch (t: Throwable) {
			null
		}
	}
}
