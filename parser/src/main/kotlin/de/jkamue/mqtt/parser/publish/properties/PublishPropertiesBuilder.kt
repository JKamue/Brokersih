package de.jkamue.mqtt.parser.publish.properties

import de.jkamue.mqtt.packet.PublishProperties
import de.jkamue.mqtt.valueobject.*
import java.nio.ByteBuffer

internal data class PublishPropertiesBuilder(
    var payloadFormat: PayloadFormat? = null,
    var messageExpiryInterval: Interval? = null,
    var topicAlias: Int? = null,
    var responseTopic: Topic? = null,
    var correlationData: ByteBuffer? = null,
    var userProperties: MutableMap<String, MutableList<String>> = mutableMapOf(),
    var subscriptionIdentifier: Int? = null,
    var contentType: ContentType? = null,
) {
    fun build(): PublishProperties {
        return PublishProperties(
            payloadFormat = payloadFormat,
            messageExpiryInterval = messageExpiryInterval,
            topicAlias = topicAlias,
            responseTopic = responseTopic,
            correlationData = correlationData,
            userProperties = UserProperties(userProperties),
            subscriptionIdentifier = subscriptionIdentifier,
            contentType = contentType,
        )
    }

    fun addUserProperty(key: String, value: String) {
        userProperties.getOrPut(key) { mutableListOf() }.add(value)
    }
}