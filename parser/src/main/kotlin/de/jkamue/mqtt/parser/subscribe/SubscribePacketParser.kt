package de.jkamue.mqtt.parser.subscribe

import de.jkamue.mqtt.packet.SubscribePacket
import de.jkamue.mqtt.valueobject.*
import de.jkamue.mqtt.parser.MQTTByteBuffer
import de.jkamue.mqtt.parser.createCopy
import de.jkamue.mqtt.parser.subscribe.properties.SubscribePropertiesParser

internal object SubscribePacketParser {
    fun parseSubscribePacket(buffer: MQTTByteBuffer): SubscribePacket {
        val packetIdentifier = buffer.getTwoByteInt()

        // Properties
        val subscribePropertyLength = buffer.getVariableByteInteger()
        val subscribePropertiesBuffer = buffer.getNextBytesAsBuffer(subscribePropertyLength)
        val subscribeProperties = SubscribePropertiesParser.parseSubscribeProperties(subscribePropertiesBuffer)

        // Payload
        val subscriptions = mutableListOf<Subscription>()
        while (buffer.remaining() > 0) {
            // Topic Filter of subscription is kept in memory for long times
            val topicFilter = TopicFilter(buffer.getBinaryData().createCopy())
            val optionsByte = buffer.getUnsignedByte()
            val options = parseSubscriptionOptions(optionsByte)
            subscriptions.add(Subscription(topicFilter, options))
        }

        return SubscribePacket(
            packetIdentifier = packetIdentifier,
            subscriptionIdentifier = subscribeProperties.first,
            userProperties = subscribeProperties.second,
            subscriptions = subscriptions as List<Subscription>
        )
    }

    fun parseSubscriptionOptions(byte: Int): SubscriptionOptions {
        return SubscriptionOptions(
            qualityOfService = QualityOfService.fromInt(byte and 0b11000000 shr 6),
            noLocal = byte and 0b00100000 != 0,
            retainAsPublished = byte and 0b00010000 != 0,
            retainHandlingOption = RetainHandlingOptions.fromInt(byte and 0b00001100 shr 2)
        )
    }
}