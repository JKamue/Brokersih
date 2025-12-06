package de.jkamue.mqtt.logic.helpers.packets

import de.jkamue.mqtt.packet.ConnectPacket
import de.jkamue.mqtt.packet.ConnectProperties
import de.jkamue.mqtt.valueobject.*
import java.nio.ByteBuffer

fun testConnectPacket(
    protocolName: String = "MQTT",
    protocolVersion: Int = 5,
    username: Username? = null,
    password: Password? = null,
    cleanStart: Boolean = true,
    keepAlive: Interval = Interval(60),
    clientId: ClientId = ClientId("testClient"),
    properties: ConnectProperties = testConnectProperties(),
    will: Will? = null,
) = ConnectPacket(
    protocolName = protocolName,
    protocolVersion = protocolVersion,
    username = username,
    password = password,
    cleanStart = cleanStart,
    keepAlive = keepAlive,
    clientId = clientId,
    properties = properties,
    will = will
)

fun testConnectProperties(
    sessionExpiry: Interval = Interval(60),
    receiveMaximum: ReceiveMaximum = ReceiveMaximum(1),
    maximumPacketSize: MaximumPacketSize? = null,
    topicAliasMaximum: TopicAliasMaximum = TopicAliasMaximum(100),
    requestResponseInformation: RequestResponseInformation = RequestResponseInformation(false),
    requestProblemInformation: RequestProblemInformation = RequestProblemInformation(false),
    userProperties: UserProperties = UserProperties(emptyMap()),
    authenticationMethod: AuthenticationMethod? = null,
    authenticationData: ByteBuffer? = null
) = ConnectProperties(
    sessionExpiry = sessionExpiry,
    receiveMaximum = receiveMaximum,
    maximumPacketSize = maximumPacketSize,
    topicAliasMaximum = topicAliasMaximum,
    requestResponseInformation = requestResponseInformation,
    requestProblemInformation = requestProblemInformation,
    userProperties = userProperties,
    authenticationMethod = authenticationMethod,
    authenticationData = authenticationData
)
