package de.jkamue.mqtt.parser.publish.properties

internal enum class PublishPropertyIdentifier(val identifier: Int) {
    PAYLOAD_FORMAT_INDICATOR(1),
    MESSAGE_EXPIRY_INTERVAL(2),
    TOPIC_ALIAS(35),
    RESPONSE_TOPIC(8),
    CORRELATION_DATA(9),
    USER_PROPERTY(38),
    SUBSCRIPTION_IDENTIFIER(11),
    CONTENT_TYPE(3),
}