package de.jkamue.mqtt.parser.connect

import de.jkamue.mqtt.MalformedPacketMqttException
import de.jkamue.mqtt.packet.ConnectPacket
import de.jkamue.mqtt.parser.MQTTByteBuffer
import de.jkamue.mqtt.parser.connect.properties.ConnectPropertiesParser
import de.jkamue.mqtt.parser.connect.will.WillParser
import de.jkamue.mqtt.valueobject.ClientId
import de.jkamue.mqtt.valueobject.Interval
import de.jkamue.mqtt.valueobject.Password
import de.jkamue.mqtt.valueobject.Username

internal object ConnectPacketParser {
    fun parseConnectPacket(buffer: MQTTByteBuffer): ConnectPacket {
        // ------ Variable header ------
        val protocolName = buffer.getEncodedString()
        val protocolVersion = buffer.getUnsignedByte()

        val rawFlags = buffer.getUnsignedByte()
        val flags = ConnectFlags.fromByte(rawFlags)

        val keepAlive = Interval(buffer.getTwoByteInt())

        val connectPropertyLength = buffer.getVariableByteInteger()
        val connectPropertiesBuffer = buffer.getNextBytesAsBuffer(connectPropertyLength)
        val connectProperties = ConnectPropertiesParser.parseConnectProperties(connectPropertiesBuffer)

        // ------ Payload ------
        val clientId = ClientId(buffer.getEncodedString())

        val will = WillParser.parse(buffer, flags)

        val username = if (flags.userName) Username(buffer.getEncodedString()) else null
        val password = if (flags.password) Password(buffer.getEncodedString()) else null

        if (buffer.remaining() > 0)
            throw MalformedPacketMqttException("${buffer.remaining()} bytes left over after CONNECT payload")

        return ConnectPacket(
            protocolName = protocolName,
            protocolVersion = protocolVersion,
            username = username,
            password = password,
            cleanStart = flags.cleanStart,
            keepAlive = keepAlive,
            clientId = clientId,
            properties = connectProperties,
            will = will
        )
    }
}