package de.jkamue.mqtt.parser.Connect

import de.jkamue.mqtt.parser.*
import de.jkamue.mqtt.valueobject.Username
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test

@SpecSection("3.1.2.8")
class UsernameParserTest {

    val flagAndUsernameSet = "00044d51545405 82 003c0511000000000008636c69656e744964 000475736572"
    val onlyUsernameNoFlag = "00044d51545405 02 003c0511000000000008636c69656e744964 000475736572"
    val onlyFlagNoUsername = "00044d51545405 82 003c0511000000000008636c69656e744964"

    @Test
    fun `can parse username if flag is set and username is present`() {
        val expectedPacket = testConnectPacket(username = Username("user"))
        assertHexStreamParsedToPacket(flagAndUsernameSet, expectedPacket)
    }

    @Test
    @MandatoryNormativeStatementTest("MQTT-3.1.2-16")
    fun `throws exception if username flag is set to 0 but username is present`() {
        assertPacketMalformed(onlyUsernameNoFlag)
    }

    @Test
    @MandatoryNormativeStatementTest("MQTT-3.1.2-17")
    fun `throws exception if username flag is set to 1 but username is not present`() {
        assertPacketMalformed(onlyFlagNoUsername)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedCases")
    @MandatoryNormativeStatementTest("MQTT-3.1.3-12")
    fun `ill-formed UTF-8 in username is a Malformed Packet`(name: String, hex: String) {
        assertPacketMalformed(onlyFlagNoUsername + hex)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validCases")
    fun `strange but valid UTF-8 username are parsed`(name: String, hex: String, expected: String) {
        assertHexStreamParsedToPacket(onlyFlagNoUsername + hex, testConnectPacket(username = Username(expected)))
    }

    companion object {
        @JvmStatic
        fun malformedCases() = (Utf8Fixtures.malformed.entries + Utf8Fixtures.disallowed.entries).stream()
            .map { Arguments.of(it.key, it.value) }

        @JvmStatic
        fun validCases() = Utf8Fixtures.valid.entries.stream()
            .map { Arguments.of(it.key, it.value.first, it.value.second) }
    }
}
