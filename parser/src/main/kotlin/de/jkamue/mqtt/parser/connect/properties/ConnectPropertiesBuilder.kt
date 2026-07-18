package de.jkamue.mqtt.parser.connect.properties

import de.jkamue.mqtt.packet.ConnectProperties
import de.jkamue.mqtt.valueobject.*
import java.nio.ByteBuffer

internal data class ConnectPropertiesBuilder(
    var sessionExpiry: Interval? = null,
    var receiveMaximum: ReceiveMaximum? = null,
    var maximumPacketSize: MaximumPacketSize? = null,
    var topicAliasMaximum: TopicAliasMaximum? = null,
    var requestResponseInformation: RequestResponseInformation? = null,
    var requestProblemInformation: RequestProblemInformation? = null,
    var userProperties: MutableMap<String, MutableList<String>> = mutableMapOf(),
    var authenticationMethod: AuthenticationMethod? = null,
    var authenticationData: ByteBuffer? = null
) {
    fun build(): ConnectProperties {
        // 3.1.2.11.2 - If the Session Expiry Interval is absent the value 0 is used
        val sessionExpiry = sessionExpiry ?: Interval(0)

        // 3.1.2.11.3 - If the Receive Maximum value is absent then its value defaults to 65,535
        val receiveMaximum = receiveMaximum ?: ReceiveMaximum(65535)

        // 3.1.2.11.5 - If the Topic Alias Maximum property is absent, the default value is 0
        val topicAliasMaximum = topicAliasMaximum ?: TopicAliasMaximum(0)

        // 3.1.2.11.6 - If the Request Response Information is absent, the value of 0 is used
        val requestResponseInformation = requestResponseInformation ?: RequestResponseInformation(false)

        // 3.1.2.11.7 - If the Request Problem Information is absent, the value of 1 is used
        val requestProblemInformation = requestProblemInformation ?: RequestProblemInformation(true)

        return ConnectProperties(
            sessionExpiry = sessionExpiry,
            receiveMaximum = receiveMaximum,
            maximumPacketSize = maximumPacketSize,
            topicAliasMaximum = topicAliasMaximum,
            requestResponseInformation = requestResponseInformation,
            requestProblemInformation = requestProblemInformation,
            userProperties = UserProperties(userProperties),
            authenticationMethod = authenticationMethod,
            authenticationData = authenticationData
        )
    }

    fun addUserProperty(key: String, value: String) {
        userProperties.getOrPut(key) { mutableListOf() }.add(value)
    }
}