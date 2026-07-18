package de.jkamue.mqtt.encoder.connack

import de.jkamue.mqtt.packet.ConnackPacket
import de.jkamue.mqtt.encoder.MQTTPropertyEncoder
import de.jkamue.mqtt.encoder.MqttEncoderHelpers
import de.jkamue.mqtt.encoder.Property
import de.jkamue.mqtt.encoder.connack.properties.MaximumQoSProperty
import java.nio.ByteBuffer

object ConnackEncoder {
    fun encodeScatter(packet: ConnackPacket): Array<ByteBuffer> {
        val fixedHeaderByte0: Byte = (2 shl 4).toByte()
        val connAckFlags: Byte = if (packet.sessionPresent) 0x01 else 0x00
        val reason: Byte = packet.connectReasonCode.value.toByte()

        // encode the properties
        val properties = listOfNotNull<Property>(
            MaximumQoSProperty.create(packet.maximumQualityOfService)
        )
        val propertyBuffer = MQTTPropertyEncoder.encodeProperties(properties)
        val propertiesLength = propertyBuffer.remaining()

        // For the minimal example we have no properties.
        val variableHeaderSize = 1 + 1 + propertiesLength // flags + reason + property length
        val remainingLength = variableHeaderSize
        val remainingLenEncSize = MqttEncoderHelpers.variableByteIntSize(remainingLength)

        // Build small header part (fixed header + remaining length bytes + first part of var header)
        val headerBuf =
            ByteBuffer.allocate(1 + remainingLenEncSize + 1 + 1) // header + remaining length + connAckFlags + reason
        headerBuf.put(fixedHeaderByte0)
        MqttEncoderHelpers.encodeVariableByteIntegerToBuffer(remainingLength, headerBuf)
        headerBuf.put(connAckFlags)
        headerBuf.put(reason)

        headerBuf.flip()

        return arrayOf(headerBuf, propertyBuffer)
    }
}