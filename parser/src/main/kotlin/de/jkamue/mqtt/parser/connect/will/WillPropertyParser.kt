package de.jkamue.mqtt.parser.connect.will

import de.jkamue.mqtt.MalformedPacketMqttException
import de.jkamue.mqtt.ProtocolErrorMqttException
import de.jkamue.mqtt.valueobject.*
import de.jkamue.mqtt.parser.MQTTByteBuffer
import de.jkamue.mqtt.parser.createCopy

internal object WillPropertyParser {

    fun parseConnectWillProperties(buffer: MQTTByteBuffer): WillProperties {
        val builder = WillPropertiesBuilder()
        while (buffer.remaining() > 0) {
            val propertyIdentifier = buffer.getUnsignedByte()
            WillPropertyHandlers.handlers[propertyIdentifier]?.invoke(buffer, builder)
                ?: throw MalformedPacketMqttException("Unknown will property: $propertyIdentifier")
        }

        return builder.build()
    }

    private object WillPropertyHandlers {
        // Inline function prevents unboxing of value classes even though the function is generic
        // If propertyName is passed as a String, String interpolation happens on every call
        // by sending it through a lambda interpolation is deferred to when the exception is actually thrown
        inline fun <T> T?.setOnce(newValue: T, inlineName: () -> String): T {
            if (this != null) throw ProtocolErrorMqttException("Will Property ${inlineName()} was present multiple times")
            return newValue
        }

        val handlers: Map<Int, (MQTTByteBuffer, WillPropertiesBuilder) -> Unit> by lazy {
            mapOf(
                WillPropertyIdentifier.WILL_DELAY_INTERVAL.identifier to { buffer, builder ->
                    builder.willDelayInterval = builder.willDelayInterval.setOnce(
                        Interval(buffer.getFourByteInt())
                    ) { "willDelayInterval" }
                },
                WillPropertyIdentifier.PAYLOAD_FORMAT_INDICATOR.identifier to { buffer, builder ->
                    builder.payloadFormatIndicator = builder.payloadFormatIndicator.setOnce(
                        PayloadFormat.fromInt(buffer.getUnsignedByte())
                    ) { "payloadFormatIndicator" }
                },
                WillPropertyIdentifier.MESSAGE_EXPIRY_INTERVAL.identifier to { buffer, builder ->
                    builder.messageExpiryInterval = builder.messageExpiryInterval.setOnce(
                        Interval(buffer.getFourByteInt())
                    ) { "messageExpiryInterval" }
                },
                WillPropertyIdentifier.CONTENT_TYPE.identifier to { buffer, builder ->
                    builder.contentType = builder.contentType.setOnce(
                        ContentType(buffer.getEncodedString())
                    ) { "contentType" }
                },
                WillPropertyIdentifier.RESPONSE_TOPIC.identifier to { buffer, builder ->
                    builder.responseTopic = builder.responseTopic.setOnce(
                        Topic(buffer.getBinaryData())
                    ) { "responseTopic" }
                },
                WillPropertyIdentifier.CORRELATION_DATA.identifier to { buffer, builder ->
                    builder.correlationData = builder.correlationData.setOnce(
                        // copy needs to be created otherwise the whole packet will be kept in memory
                        buffer.getBinaryData().createCopy()
                    ) { "correlationData" }
                },
                WillPropertyIdentifier.USER_PROPERTY.identifier to { buffer, builder ->
                    val key = buffer.getEncodedString()
                    val value = buffer.getEncodedString()
                    builder.addUserProperty(key, value)
                },
            )
        }
    }
}