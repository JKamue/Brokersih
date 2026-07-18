package de.jkamue.mqtt.encoder

import java.nio.ByteBuffer

object MQTTPropertyEncoder {
    fun encodeProperties(properties: List<Property>): ByteBuffer {
        val propertySize = properties.sumOf(Property::encodedSize)
        val propLengthEncodedSize = MqttEncoderHelpers.variableByteIntSize(propertySize)
        val bufferSize = propertySize + propLengthEncodedSize

        val buffer = ByteBuffer.allocate(bufferSize)
        MqttEncoderHelpers.encodeVariableByteIntegerToBuffer(propertySize, buffer)
        properties.forEach { it.encode(buffer) }

        buffer.flip()
        return buffer
    }
}

interface Property {
    fun encodedSize(): Int
    fun encode(buffer: ByteBuffer)
}