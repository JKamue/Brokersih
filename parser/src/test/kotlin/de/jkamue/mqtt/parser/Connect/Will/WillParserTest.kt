package de.jkamue.mqtt.parser.Connect.Will

import de.jkamue.mqtt.parser.*
import de.jkamue.mqtt.parser.Connect.testConnectPacket
import de.jkamue.mqtt.parser.Connect.testWill
import de.jkamue.mqtt.parser.Connect.testWillProperties
import de.jkamue.mqtt.valueobject.Payload
import de.jkamue.mqtt.valueobject.QualityOfService
import de.jkamue.mqtt.valueobject.Topic
import de.jkamue.mqtt.valueobject.Will
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test

class WillParserTest {
    // @formatter:off
    val willFlagButNoWill      = "00044d51545405 06 003c0511000000000008636c69656e744964"
    val willFlag0ButQosNot0    = "00044d51545405 0a 003c0511000000000008636c69656e744964"
    val willFlag0ButRetainNot0 = "00044d51545405 22 003c0511000000000008636c69656e744964"
    val willQosNotExisting     = "00044d51545405 1e 003c0511000000000008636c69656e744964 00000161000162"
    // @formatter:on

    @Test
    @MandatoryNormativeStatementTest("MQTT-3.1.2-9")
    fun `throws exception if will flag is set to 1 but will data not set`() {
        assertPacketMalformed(willFlagButNoWill)
    }

    @Test
    @MandatoryNormativeStatementTest("MQTT-3.1.2-11")
    fun `throws exception if will flag is set to 0 but will qos not 0`() {
        assertPacketMalformed(willFlag0ButQosNot0)
    }

    @Test
    @MandatoryNormativeStatementTest("MQTT-3.1.2-13")
    fun `throws exception if will flag is set to 0 but will retain not 0`() {
        assertPacketMalformed(willFlag0ButRetainNot0)
    }

    @Test
    @SpecSection("3.1.2.6")
    fun `throws exception if will qos is 3`() {
        assertPacketMalformed(willQosNotExisting)
    }


    @Test
    @SpecSection("3.1.3.3-3.1.3.6")
    fun `will is parsed correctly`() {
        val expectedWill = Will(
            retain = true,
            qualityOfService = QualityOfService.EXACTLY_ONCE_DELIVERY,
            topic = Topic("will/smith".toByteBuffer()),
            payload = Payload("Keep my wife's name out your fucking mouth!".toByteBuffer()),
            properties = testWillProperties()
        )
        val expectedWillPacket = testConnectPacket(will = expectedWill)
        val willPacket =
            "0004 4d515454 05 36 003c 05 11 00000000 0008 636c69656e744964 " +
                    "00 " + // will properties length 0
                    "000a 77696c6c2f736d697468 " + // will topic "will/smith"
                    "002b 4b656570206d7920776966652773206e616d65206f757420796f7572206675636b696e67206d6f75746821"

        assertHexStreamParsedToPacket(willPacket, expectedWillPacket)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedCases")
    @MandatoryNormativeStatementTest("MQTT-3.1.3-11")
    fun `ill-formed UTF-8 in will topic is a Malformed Packet`(name: String, willTopic: String) {
        assertPacketMalformed(connectWithWillTopic(willTopic))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validCases")
    fun `strange but valid UTF-8 will topic are parsed`(name: String, willTopic: String, expected: String) {
        val hexStream = connectWithWillTopic(willTopic)
        val expectedPacket = testConnectPacket(will = testWill(topic = Topic(expected.toByteBuffer())))
        assertHexStreamParsedToPacket(hexStream, expectedPacket)
    }

    companion object {
        @JvmStatic
        fun malformedCases() = (Utf8Fixtures.malformed.entries + Utf8Fixtures.disallowed.entries).stream()
            .map { Arguments.of(it.key, it.value) }

        @JvmStatic
        fun validCases() = Utf8Fixtures.valid.entries.stream()
            .map { Arguments.of(it.key, it.value.first, it.value.second) }
    }

    private fun connectWithWillTopic(topic: String): String {
        return "0004 4d515454 05 06 003c 05 11 00000000 0008 636c69656e744964 00" +
                topic +
                "00026869" // payload
    }
}
