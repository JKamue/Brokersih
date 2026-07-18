package de.jkamue.mqtt.parser.Connect

import de.jkamue.mqtt.packet.ConnectPacket
import de.jkamue.mqtt.packet.ConnectProperties
import de.jkamue.mqtt.parser.toByteBuffer
import de.jkamue.mqtt.valueobject.*
import java.nio.ByteBuffer


internal fun testWillProperties(
    willDelayInterval: Interval = Interval(0),
    payloadFormat: PayloadFormat = PayloadFormat.UNSPECIFIED_BYTES,
    messageExpiryInterval: Interval? = null,
    contentType: ContentType? = null,
    responseTopic: Topic? = null,
    correlationData: ByteBuffer? = null,
    userProperties: UserProperties = UserProperties(emptyMap())
) = WillProperties(
    willDelayInterval,
    payloadFormat,
    messageExpiryInterval,
    contentType,
    responseTopic,
    correlationData,
    userProperties
)

internal fun testWill(
    retain: Boolean = false,
    qualityOfService: QualityOfService = QualityOfService.AT_MOST_ONCE_DELIVERY,
    topic: Topic = Topic("will".toByteBuffer()),
    payload: Payload = Payload("hi".toByteBuffer()),
    properties: WillProperties = testWillProperties()
) = Will(
    retain,
    qualityOfService,
    topic,
    payload,
    properties
)

internal fun testConnectProperties(
    sessionExpiry: Interval = Interval(0),
    receiveMaximum: ReceiveMaximum = ReceiveMaximum(65535),
    maximumPacketSize: MaximumPacketSize? = null,
    topicAliasMaximum: TopicAliasMaximum = TopicAliasMaximum(0),
    requestResponseInformation: RequestResponseInformation = RequestResponseInformation(false),
    requestProblemInformation: RequestProblemInformation = RequestProblemInformation(true),
    userProperties: UserProperties = UserProperties(emptyMap()),
    authenticationMethod: AuthenticationMethod? = null,
    authenticationData: ByteBuffer? = null
) = ConnectProperties(
    sessionExpiry,
    receiveMaximum,
    maximumPacketSize,
    topicAliasMaximum,
    requestResponseInformation,
    requestProblemInformation,
    userProperties,
    authenticationMethod,
    authenticationData,
)

internal fun testConnectPacket(
    protocolName: String = "MQTT",
    protocolVersion: Int = 5,
    username: Username? = null,
    password: Password? = null,
    cleanStart: Boolean = true,
    keepAlive: Interval = Interval(60),
    clientId: ClientId = ClientId("clientId"),
    properties: ConnectProperties = testConnectProperties(),
    will: Will? = null
) = ConnectPacket(
    protocolName,
    protocolVersion,
    username,
    password,
    cleanStart,
    keepAlive,
    clientId,
    properties,
    will
)
