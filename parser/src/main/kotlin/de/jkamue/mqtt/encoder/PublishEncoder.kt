package de.jkamue.mqtt.encoder

import de.jkamue.mqtt.packet.PublishPacket
import de.jkamue.mqtt.packet.PublishProperties
import de.jkamue.mqtt.parser.MQTTByteBuffer
import java.nio.ByteBuffer

object PublishEncoder {

    fun encodeScatter(packet: PublishPacket): Array<ByteBuffer> {
        val topicName = packet.topic.value.duplicate().rewind().asReadOnlyBuffer()
        val payload = packet.payload.duplicate().rewind().asReadOnlyBuffer()

        val propertiesBuffer = encodeProperties(packet.properties)
        val propertiesLength = propertiesBuffer.remaining()

        val contentLength = 2 + // topic name length
                topicName.remaining() + // topic name
                // Qos 0 -> no identifier 2 + // packet identifier
                propertiesLength + // length of the properties
                payload.remaining() // payload length
        val lengthAfterHeader = MqttEncoderHelpers.variableByteIntegerLength(contentLength)

        val buffer =
            ByteBuffer.allocate(1 + lengthAfterHeader + 2) // 1 header + lengthAfterHeader + 2 topic length name
        buffer.put(0b00110000.toByte()) // control packet type + fixed dup / qos / retain
        MqttEncoderHelpers.encodeVariableByteIntegerToBuffer(contentLength, buffer)
        MqttEncoderHelpers.encodeTwoByteInt(topicName.remaining(), buffer)

        buffer.flip()

        MQTTByteBuffer.wrap(topicName).getString()

        return arrayOf(
            buffer,
            topicName,
            // Qos 0 -> no identifier but in the future MqttEncoderHelpers.encodeTwoByteInt(packet.packetIdentifier, buffer)
            propertiesBuffer,
            payload
        )
    }

    private fun encodeProperties(properties: PublishProperties): ByteBuffer {
        var propertyContentLength = 0

        properties.subscriptionIdentifier?.let { id ->
            propertyContentLength += 1 // property identifier
            propertyContentLength += MqttEncoderHelpers.variableByteIntegerLength(id)
        }

        // --- Allocate exact buffer size: property length field + properties ---
        val propertyLengthFieldSize = MqttEncoderHelpers.variableByteIntegerLength(propertyContentLength)
        val buffer = ByteBuffer.allocate(propertyLengthFieldSize + propertyContentLength)

        // --- Encode property length field ---
        MqttEncoderHelpers.encodeVariableByteIntegerToBuffer(propertyContentLength, buffer)

        // --- Encode property content ---
        properties.subscriptionIdentifier?.let { id ->
            buffer.put(0x0B.toByte())
            MqttEncoderHelpers.encodeVariableByteIntegerToBuffer(id, buffer)
        }

        buffer.flip()
        return buffer.asReadOnlyBuffer()
    }
}