package de.jkamue.mqtt.parser.connect

import de.jkamue.mqtt.MalformedPacketMqttException
import de.jkamue.mqtt.valueobject.QualityOfService

internal data class ConnectFlags(
    val userName: Boolean,
    val password: Boolean,
    val willRetain: Boolean,
    val willQos: QualityOfService,
    val willFlag: Boolean,
    val cleanStart: Boolean
) {
    companion object {
        fun fromByte(connectFlags: Int): ConnectFlags {
            val userNameFlag = connectFlags and 0b10000000 != 0
            val passwordFlag = connectFlags and 0b01000000 != 0
            val willRetainFlag = connectFlags and 0b00100000 != 0
            val willQoS = QualityOfService.fromInt(connectFlags and 0b00011000 shr 3)
            val willFlag = connectFlags and 0b00000100 != 0
            val cleanStart = connectFlags and 0b00000010 != 0
            val reservedFlag = connectFlags and 0b00000001 != 0
            if (reservedFlag) throw MalformedPacketMqttException("Reserved Connect flag set MQTT-3.1.2-3")


            return ConnectFlags(
                userName = userNameFlag,
                password = passwordFlag,
                willRetain = willRetainFlag,
                willQos = willQoS,
                willFlag = willFlag,
                cleanStart = cleanStart
            )
        }
    }
}