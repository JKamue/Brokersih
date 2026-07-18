package de.jkamue.mqtt.parser.connect.will

import de.jkamue.mqtt.valueobject.*
import java.nio.ByteBuffer

internal data class WillPropertiesBuilder(
    var willDelayInterval: Interval? = null,
    var payloadFormatIndicator: PayloadFormat? = null,
    var messageExpiryInterval: Interval? = null,
    var contentType: ContentType? = null,
    var responseTopic: Topic? = null,
    var correlationData: ByteBuffer? = null,
    val userProperties: MutableMap<String, MutableList<String>> = mutableMapOf()
) {
    fun build(): WillProperties {
        // 3.1.3.2.2 - If the Will Delay Interval is absent, the default value is 0
        val willDelayInterval = willDelayInterval ?: Interval(0)

        // 3.1.3.2.3 - unspecified bytes, [...] is equivalent to not sending a Payload Format Indicator
        val payloadFormatIndicator = payloadFormatIndicator ?: PayloadFormat.UNSPECIFIED_BYTES

        return WillProperties(
            willDelayInterval = willDelayInterval,
            payloadFormat = payloadFormatIndicator,
            messageExpiryInterval = messageExpiryInterval,
            contentType = contentType,
            responseTopic = responseTopic,
            correlationData = correlationData,
            userProperties = UserProperties(userProperties)
        )
    }

    fun addUserProperty(key: String, value: String) {
        userProperties.getOrPut(key) { mutableListOf() }.add(value)
    }
}