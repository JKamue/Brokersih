package de.jkamue.mqtt.encoder.connack.properties

import de.jkamue.mqtt.valueobject.QualityOfService
import de.jkamue.mqtt.encoder.Property
import java.nio.ByteBuffer

class MaximumQoSProperty(private val qos: QualityOfService) : Property {
    override fun encodedSize(): Int = 2 // 1 byte for identifier, 1 byte for value

    override fun encode(buffer: ByteBuffer) {
        buffer.put(0x24) // Property Identifier for Maximum QoS in MQTT 5.0
        buffer.put(qos.number.toByte())
    }

    companion object {
        fun create(qos: QualityOfService?): MaximumQoSProperty? {
            return qos?.let { MaximumQoSProperty(qos) }
        }
    }
}