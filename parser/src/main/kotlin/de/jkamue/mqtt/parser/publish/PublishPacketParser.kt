package de.jkamue.mqtt.parser.publish

import de.jkamue.mqtt.packet.PublishPacket
import de.jkamue.mqtt.valueobject.QualityOfService
import de.jkamue.mqtt.valueobject.Topic
import de.jkamue.mqtt.parser.MQTTByteBuffer
import de.jkamue.mqtt.parser.publish.properties.PublishPropertiesParser

internal object PublishPacketParser {
    fun parsePublishPacket(buffer: MQTTByteBuffer): PublishPacket {
        // TODO: Get dup, qos and retain :/
        // ------ Variable header ------
        val topicName = Topic(buffer.getBinaryData())
        val packetIdentifier = 0
        // TODO: Use QoS that conditionally sets this
        //  val packetIdentifier = buffer.getTwoByteInt()

        // ------ Properties ------
        val publishPropertyLength = buffer.getVariableByteInteger()
        val publishPropertiesBuffer = buffer.getNextBytesAsBuffer(publishPropertyLength)
        val publishProperties = PublishPropertiesParser.parsePublishProperties(publishPropertiesBuffer)

        // ------ Payload ------
        val payloadBuffer = buffer.getNextBytesAsBuffer(buffer.remaining())

        return PublishPacket(
            dup = false,
            qualityOfService = QualityOfService.AT_MOST_ONCE_DELIVERY,
            retain = false,
            topic = topicName,
            packetIdentifier = packetIdentifier,
            properties = publishProperties,
            payload = payloadBuffer.buffer
        )
    }
}