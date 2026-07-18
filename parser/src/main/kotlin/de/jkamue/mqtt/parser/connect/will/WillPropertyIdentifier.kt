package de.jkamue.mqtt.parser.connect.will

internal enum class WillPropertyIdentifier(val identifier: Int) {
    WILL_DELAY_INTERVAL(24),
    PAYLOAD_FORMAT_INDICATOR(1),
    MESSAGE_EXPIRY_INTERVAL(2),
    CONTENT_TYPE(3),
    RESPONSE_TOPIC(8),
    CORRELATION_DATA(9),
    USER_PROPERTY(38)
}