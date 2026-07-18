package de.jkamue.mqtt.server

import de.jkamue.mqtt.valueobject.Interval

object Settings {
    const val SERVER_MAX_PACKET_SIZE = 8192 // 8KB
    val SERVER_KEEP_ALIVE_INTERVAL = Interval(60) // 60s
}