package de.jkamue.mqtt.parser.connect.properties

import de.jkamue.mqtt.MalformedPacketMqttException
import de.jkamue.mqtt.ProtocolErrorMqttException
import de.jkamue.mqtt.packet.ConnectProperties
import de.jkamue.mqtt.valueobject.*
import de.jkamue.mqtt.parser.MQTTByteBuffer
import de.jkamue.mqtt.parser.createCopy

internal object ConnectPropertiesParser {

    fun parseConnectProperties(buffer: MQTTByteBuffer): ConnectProperties {
        val builder = ConnectPropertiesBuilder()
        while (buffer.remaining() > 0) {
            val propertyIdentifier = buffer.getUnsignedByte()
            ConnectPropertyHandlers.handlers[propertyIdentifier]?.invoke(buffer, builder)
                ?: throw MalformedPacketMqttException("Unknown connect property: $propertyIdentifier")
        }

        return builder.build()
    }

    private object ConnectPropertyHandlers {
        // Inline function prevents unboxing of value classes even though the function is generic
        // If propertyName is passed as a String, String interpolation happens on every call
        // by sending it through a lambda interpolation is deferred to when the exception is actually thrown
        inline fun <T> T?.setOnce(newValue: T, inlineName: () -> String): T {
            if (this != null) throw ProtocolErrorMqttException("Connect Property ${inlineName()} was present multiple times")
            return newValue
        }

        val handlers: Map<Int, (MQTTByteBuffer, ConnectPropertiesBuilder) -> Unit> by lazy {
            mapOf(
                ConnectPropertyIdentifier.SESSION_EXPIRY_INTERVAL.identifier to { buffer, builder ->
                    builder.sessionExpiry = builder.sessionExpiry.setOnce(
                        Interval(buffer.getFourByteInt())
                    ) { "sessionExpiry" }
                },
                ConnectPropertyIdentifier.RECEIVE_MAXIMUM.identifier to { buffer, builder ->
                    builder.receiveMaximum = builder.receiveMaximum.setOnce(
                        ReceiveMaximum(buffer.getTwoByteInt())
                    ) { "receiveMaximum" }
                },
                ConnectPropertyIdentifier.MAXIMUM_PACKET_SIZE.identifier to { buffer, builder ->
                    builder.maximumPacketSize = builder.maximumPacketSize.setOnce(
                        MaximumPacketSize(buffer.getFourByteInt())
                    ) { "maximumPacketSize" }
                },
                ConnectPropertyIdentifier.TOPIC_ALIAS_MAXIMUM.identifier to { buffer, builder ->
                    builder.topicAliasMaximum = builder.topicAliasMaximum.setOnce(
                        TopicAliasMaximum(buffer.getTwoByteInt())
                    ) { "topicAliasMaximum" }
                },
                ConnectPropertyIdentifier.REQUEST_RESPONSE_INFORMATION.identifier to { buffer, builder ->
                    builder.requestResponseInformation = builder.requestResponseInformation.setOnce(
                        RequestResponseInformation(buffer.getUnsignedByte() == 1)
                    ) { "requestResponseInformation" }
                },
                ConnectPropertyIdentifier.REQUEST_PROBLEM_INFORMATION.identifier to { buffer, builder ->
                    builder.requestProblemInformation = builder.requestProblemInformation.setOnce(
                        RequestProblemInformation(buffer.getUnsignedByte() == 1)
                    ) { "requestProblemInformation" }
                },
                ConnectPropertyIdentifier.USER_PROPERTY.identifier to { buffer, builder ->
                    val key = buffer.getEncodedString()
                    val value = buffer.getEncodedString()
                    builder.addUserProperty(key, value)
                },
                ConnectPropertyIdentifier.AUTHENTICATION_METHOD.identifier to { buffer, builder ->
                    builder.authenticationMethod = builder.authenticationMethod.setOnce(
                        AuthenticationMethod(buffer.getEncodedString())
                    ) { "authenticationMethod" }
                },
                ConnectPropertyIdentifier.AUTHENTICATION_DATA.identifier to { buffer, builder ->
                    builder.authenticationData = builder.authenticationData.setOnce(
                        // copy needs to be created otherwise the whole packet will be kept in memory
                        buffer.getBinaryData().createCopy()
                    ) { "authenticationData" }
                },
            )
        }

    }
}