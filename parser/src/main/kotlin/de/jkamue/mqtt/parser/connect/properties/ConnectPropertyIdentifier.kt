package de.jkamue.mqtt.parser.connect.properties

internal enum class ConnectPropertyIdentifier(val identifier: Int) {
    SESSION_EXPIRY_INTERVAL(17),
    RECEIVE_MAXIMUM(33),
    MAXIMUM_PACKET_SIZE(39),
    TOPIC_ALIAS_MAXIMUM(34),
    REQUEST_RESPONSE_INFORMATION(25),
    REQUEST_PROBLEM_INFORMATION(23),
    USER_PROPERTY(38),
    AUTHENTICATION_METHOD(21),
    AUTHENTICATION_DATA(22)
}