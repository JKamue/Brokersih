package de.jkamue.mqtt.parser.connect.will

import de.jkamue.mqtt.MalformedPacketMqttException
import de.jkamue.mqtt.parser.MQTTByteBuffer
import de.jkamue.mqtt.parser.connect.ConnectFlags
import de.jkamue.mqtt.parser.createCopy
import de.jkamue.mqtt.valueobject.Payload
import de.jkamue.mqtt.valueobject.QualityOfService
import de.jkamue.mqtt.valueobject.Topic
import de.jkamue.mqtt.valueobject.Will

internal object WillParser {
    fun parse(buffer: MQTTByteBuffer, flags: ConnectFlags): Will? {
        if (!flags.willFlag) {
            ensureWillQosAtMostOnce(flags)
            ensureWillRetainIsSetToFalse(flags)
            return null
        }

        val willPropertiesLength = buffer.getVariableByteInteger()
        val willPropertiesBuffer = buffer.getNextBytesAsBuffer(willPropertiesLength)
        val willProperties = WillPropertyParser.parseConnectWillProperties(willPropertiesBuffer)

        // Create copy since will is kept in Memory
        val topicString = buffer.getBinaryData().createCopy()
        if (topicString.remaining() == 0) {
            throw MalformedPacketMqttException("MQTT-3.1.3-11 - The Will Topic MUST be a UTF-8 Encoded String.")
        }
        val willTopic = Topic(topicString)
        // Create copy since will is kept in Memory
        val willPayload = Payload(buffer.getBinaryData().createCopy())

        return Will(
            retain = flags.willRetain,
            qualityOfService = flags.willQos,
            topic = willTopic,
            payload = willPayload,
            properties = willProperties
        )
    }

    private fun ensureWillQosAtMostOnce(flags: ConnectFlags) {
        if (flags.willQos != QualityOfService.AT_MOST_ONCE_DELIVERY) {
            throw MalformedPacketMqttException("MQTT-3.1.2-11 - If the Will Flag is set to 0, then the Will QoS MUST be set to 0.")
        }
    }

    private fun ensureWillRetainIsSetToFalse(flags: ConnectFlags) {
        if (flags.willRetain) {
            throw MalformedPacketMqttException("MQTT-3.1.2-13 - If the Will Flag is set to 0, then Will Retain MUST be set to 0.")
        }
    }
}