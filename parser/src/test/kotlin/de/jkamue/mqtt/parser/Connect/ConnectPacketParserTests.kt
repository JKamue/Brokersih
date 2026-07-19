package de.jkamue.mqtt.parser.Connect

import de.jkamue.mqtt.parser.*
import de.jkamue.mqtt.valueobject.*
import kotlin.test.Test

@SpecSection("3.1")
class ConnectPacketParserTests {

    @Test
    fun `basic connect packet can be parsed`() {
        val basicConnectPacket = "00044d5154540502003c0511000000000008636c69656e744964"
        val expectedParsedPacket = testConnectPacket()
        assertHexStreamParsedToPacket(basicConnectPacket, expectedParsedPacket)
    }

    @Test
    fun `complex connect packet can be parsed`() {
        val complexConnectPacket =
            "00044d51545405ee001435110000032021019027000000c822000219011701260004313233340004353637382600047465737400016126000474657374000162000d636f6d706c6578436c69656e742718000000c801010200000014030004736c617008000a63687269732f726f636b09000466616365000a77696c6c2f736d697468002b4b656570206d7920776966652773206e616d65206f757420796f7572206675636b696e67206d6f75746821000474657374000470617373"
        val expectedWill = testWill(
            retain = true,
            qualityOfService = QualityOfService.AT_LEAST_ONCE_DELIVERY,
            topic = Topic("will/smith".toByteBuffer()),
            payload = Payload("Keep my wife's name out your fucking mouth!".toByteBuffer()),
            properties = testWillProperties(
                willDelayInterval = Interval(200),
                payloadFormat = PayloadFormat.UTF8,
                messageExpiryInterval = Interval(20),
                contentType = ContentType("slap"),
                responseTopic = Topic("chris/rock".toByteBuffer()),
                correlationData = "face".toByteBuffer(),
                userProperties = UserProperties(emptyMap())
            )
        )
        val expectedConnectProperties = testConnectProperties(
            sessionExpiry = Interval(800),
            receiveMaximum = ReceiveMaximum(400),
            maximumPacketSize = MaximumPacketSize(200),
            topicAliasMaximum = TopicAliasMaximum(2),
            requestProblemInformation = RequestProblemInformation(true),
            requestResponseInformation = RequestResponseInformation(true),
            userProperties = UserProperties(
                mapOf(
                    "test" to listOf("a", "b"),
                    "1234" to listOf("5678"),
                )
            ),
        )
        val expectedParsedPacket = testConnectPacket(
            username = Username("test"),
            password = Password("pass"),
            cleanStart = true,
            keepAlive = Interval(20),
            clientId = ClientId("complexClient"),
            properties = expectedConnectProperties,
            will = expectedWill
        )
        assertHexStreamParsedToPacket(complexConnectPacket, expectedParsedPacket)
    }

    @Test
    @SpecSection("3.1.2.4")
    fun `clean start can be turned off`() {
        val packet = "00044d5154540500003c0511000000000008636c69656e744964"
        val expectedParsedPacket = testConnectPacket(cleanStart = false)
        assertHexStreamParsedToPacket(packet, expectedParsedPacket)
    }

    @Test
    @MandatoryNormativeStatementTest("MQTT-3.1.2-3")
    fun `reserved connect flag set to 1 is malformed`() {
        val reservedBitSet = "00044d51545405 03 003c0511000000000008636c69656e744964"
        assertPacketMalformed(reservedBitSet)
    }

    @Test
    @SpecSection("3.1.3")
    fun `leftover bytes after the connect payload are malformed`() {
        val trailingGarbage = "00044d5154540502003c0511000000000008636c69656e744964 ff"
        val unannouncedUsername = "00044d5154540502003c0511000000000008636c69656e744964 000475736572"
        assertPacketMalformed(trailingGarbage)
        assertPacketMalformed(unannouncedUsername)
    }
}