package de.jkamue.mqtt.parser.subscribe.properties

import de.jkamue.mqtt.MalformedPacketMqttException
import de.jkamue.mqtt.ProtocolErrorMqttException
import de.jkamue.mqtt.valueobject.*
import de.jkamue.mqtt.parser.MQTTByteBuffer

internal object SubscribePropertiesParser {

    fun parseSubscribeProperties(buffer: MQTTByteBuffer): Pair<Int?, UserProperties> {
        val builder = SubscribePropertiesBuilder()
        while (buffer.remaining() > 0) {
            val propertyIdentifier = buffer.getUnsignedByte()
            SubscribePropertyHandlers.handlers[propertyIdentifier]?.invoke(buffer, builder)
                ?: throw MalformedPacketMqttException("Unknown subscribe property: $propertyIdentifier")
        }

        return builder.build()
    }

    private object SubscribePropertyHandlers {
        // Inline function prevents unboxing of value classes even though the function is generic
        // If propertyName is passed as a String, String interpolation happens on every call
        // by sending it through a lambda interpolation is deferred to when the exception is actually thrown
        inline fun <T> T?.setOnce(newValue: T, inlineName: () -> String): T {
            if (this != null) throw ProtocolErrorMqttException("Subscribe Property ${inlineName()} was present multiple times")
            return newValue
        }

        val handlers: Map<Int, (MQTTByteBuffer, SubscribePropertiesBuilder) -> Unit> by lazy {
            mapOf(
                SubscribePropertyIdentifier.SUBSCRIPTION_IDENTIFIER.identifier to { buffer, builder ->
                    builder.subscriptionIdentifier = builder.subscriptionIdentifier.setOnce(
                        buffer.getVariableByteInteger()
                    ) { "subscriptionIdentifier" }
                },
                SubscribePropertyIdentifier.USER_PROPERTY.identifier to { buffer, builder ->
                    val key = buffer.getEncodedString()
                    val value = buffer.getEncodedString()
                    builder.addUserProperty(key, value)
                },
            )
        }

    }
}