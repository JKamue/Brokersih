package de.jkamue.mqtt.parser.publish.properties

import de.jkamue.mqtt.MalformedPacketMqttException
import de.jkamue.mqtt.ProtocolErrorMqttException
import de.jkamue.mqtt.packet.PublishProperties
import de.jkamue.mqtt.valueobject.ContentType
import de.jkamue.mqtt.valueobject.Interval
import de.jkamue.mqtt.valueobject.PayloadFormat
import de.jkamue.mqtt.valueobject.Topic
import de.jkamue.mqtt.parser.MQTTByteBuffer
import de.jkamue.mqtt.parser.createCopy

internal object PublishPropertiesParser {

    fun parsePublishProperties(buffer: MQTTByteBuffer): PublishProperties {
        val builder = PublishPropertiesBuilder()
        while (buffer.remaining() > 0) {
            val propertyIdentifier = buffer.getUnsignedByte()
            PublishPropertyHandlers.handlers[propertyIdentifier]?.invoke(buffer, builder)
                ?: throw MalformedPacketMqttException("Unknown publish property: $propertyIdentifier")
        }

        return builder.build()
    }

    private object PublishPropertyHandlers {
        // Inline function prevents unboxing of value classes even though the function is generic
        // If propertyName is passed as a String, String interpolation happens on every call
        // by sending it through a lambda interpolation is deferred to when the exception is actually thrown
        inline fun <T> T?.setOnce(newValue: T, inlineName: () -> String): T {
            if (this != null) throw ProtocolErrorMqttException("Publish Property ${inlineName()} was present multiple times")
            return newValue
        }

        val handlers: Map<Int, (MQTTByteBuffer, PublishPropertiesBuilder) -> Unit> by lazy {
            mapOf(
                PublishPropertyIdentifier.PAYLOAD_FORMAT_INDICATOR.identifier to { buffer, builder ->
                    builder.payloadFormat = builder.payloadFormat.setOnce(
                        PayloadFormat.fromInt(buffer.getUnsignedByte())
                    ) { "payloadFormat" }
                },
                PublishPropertyIdentifier.MESSAGE_EXPIRY_INTERVAL.identifier to { buffer, builder ->
                    builder.messageExpiryInterval = builder.messageExpiryInterval.setOnce(
                        Interval(buffer.getFourByteInt())
                    ) { "messageExpiryInterval" }
                },
                PublishPropertyIdentifier.TOPIC_ALIAS.identifier to { buffer, builder ->
                    builder.topicAlias = builder.topicAlias.setOnce(
                        buffer.getTwoByteInt()
                    ) { "topicAlias" }
                },
                PublishPropertyIdentifier.RESPONSE_TOPIC.identifier to { buffer, builder ->
                    builder.responseTopic = builder.responseTopic.setOnce(
                        Topic(buffer.getBinaryData())
                    ) { "responseTopic" }
                },
                PublishPropertyIdentifier.RESPONSE_TOPIC.identifier to { buffer, builder ->
                    builder.responseTopic = builder.responseTopic.setOnce(
                        Topic(buffer.getBinaryData())
                    ) { "responseTopic" }
                },
                PublishPropertyIdentifier.CORRELATION_DATA.identifier to { buffer, builder ->
                    builder.correlationData = builder.correlationData.setOnce(
                        // copy needs to be created otherwise the whole packet will be kept in memory
                        buffer.getBinaryData().createCopy()
                    ) { "correlationData" }
                },
                PublishPropertyIdentifier.USER_PROPERTY.identifier to { buffer, builder ->
                    val key = buffer.getEncodedString()
                    val value = buffer.getEncodedString()
                    builder.addUserProperty(key, value)
                },
                PublishPropertyIdentifier.SUBSCRIPTION_IDENTIFIER.identifier to { buffer, builder ->
                    builder.subscriptionIdentifier = builder.subscriptionIdentifier.setOnce(
                        buffer.getTwoByteInt()
                    ) { "responseTopic" }
                },
                PublishPropertyIdentifier.CONTENT_TYPE.identifier to { buffer, builder ->
                    builder.contentType = builder.contentType.setOnce(
                        ContentType(buffer.getEncodedString())
                    ) { "contentType" }
                },
            )
        }

    }
}