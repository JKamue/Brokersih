package de.jkamue.mqtt.parser.Connect

import de.jkamue.mqtt.parser.*
import de.jkamue.mqtt.valueobject.ClientId
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test

@SpecSection("3.1.3.1")
class ClientIdParserTest {

    private val connectHead = "00044d5154540502003c051100000000"

    @Test
    @MandatoryNormativeStatementTest("MQTT-3.1.3-3")
    fun `missing client id throws exception`() {
        val noClientIdPresent = connectHead
        assertPacketMalformed(noClientIdPresent)
    }

    @Test
    @MandatoryNormativeStatementTest("MQTT-3.1.3-5")
    fun `clientIds up to 23 bytes with 09AZaz are allowed`() {
        val zeroToSmallM = "$connectHead 0017303132333435363738396162636465666768696a6b6c6d"
        val smallNToCapJ = "$connectHead 00176e6f707172737475767778797a4142434445464748494a"
        val capKToCapZ = "  $connectHead 00104b4c4d4e4f505152535455565758595a"

        val zeroToSmallMPacket = testConnectPacket(clientId = ClientId("0123456789abcdefghijklm"))
        val smallNToCapJPacket = testConnectPacket(clientId = ClientId("nopqrstuvwxyzABCDEFGHIJ"))
        val capKToCapZPacket = testConnectPacket(clientId = ClientId("KLMNOPQRSTUVWXYZ"))

        assertHexStreamParsedToPacket(zeroToSmallM, zeroToSmallMPacket)
        assertHexStreamParsedToPacket(smallNToCapJ, smallNToCapJPacket)
        assertHexStreamParsedToPacket(capKToCapZ, capKToCapZPacket)
    }

    @Test
    @MandatoryNormativeStatementTest("MQTT-3.1.3-6")
    fun `empty clientId is allowed`() {
        val packet = "$connectHead 0000"
        val expectedParsedPacket = testConnectPacket(clientId = ClientId(""))
        assertHexStreamParsedToPacket(packet, expectedParsedPacket)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedCases")
    @MandatoryNormativeStatementTest("MQTT-3.1.3-4")
    fun `ill-formed UTF-8 in clientId is a Malformed Packet`(name: String, hex: String) {
        assertPacketMalformed(connectHead + hex)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validCases")
    fun `strange but valid UTF-8 clientIds are parsed`(name: String, hex: String, expected: String) {
        assertHexStreamParsedToPacket(connectHead + hex, testConnectPacket(clientId = ClientId(expected)))
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
