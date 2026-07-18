package de.jkamue.mqtt.parser.Connect

import de.jkamue.mqtt.parser.MandatoryNormativeStatementTest
import de.jkamue.mqtt.parser.SpecSection
import de.jkamue.mqtt.parser.assertHexStreamParsedToPacket
import de.jkamue.mqtt.parser.assertPacketMalformed
import de.jkamue.mqtt.valueobject.ClientId
import kotlin.test.Test

@SpecSection("3.1.3.1")
class ClientIdParserTest {

    @Test
    @MandatoryNormativeStatementTest("MQTT-3.1.3-3")
    fun `missing client id throws exception`() {
        val noClientIdPresent = "00044d5154540502003c051100000000"
        assertPacketMalformed(noClientIdPresent)
    }

    @Test
    @MandatoryNormativeStatementTest("MQTT-3.1.3-5")
    fun `clientIds up to 23 bytes with 09AZaz are allowed`() {
        val zeroToSmallM = "00044d5154540502003c0511000000000017303132333435363738396162636465666768696a6b6c6d"
        val smallNToCapJ = "00044d5154540502003c05110000000000176e6f707172737475767778797a4142434445464748494a"
        val capKToCapZ = "  00044d5154540502003c05110000000000104b4c4d4e4f505152535455565758595a"

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
        val packet = "00044d5154540502003c0511000000000000"
        val expectedParsedPacket = testConnectPacket(clientId = ClientId(""))
        assertHexStreamParsedToPacket(packet, expectedParsedPacket)
    }

    // TODO: Ensure MQTT-3.1.3-4 and MQTT-1.5.4-2 / MQTT-1.5.4-1
}