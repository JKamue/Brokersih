package de.jkamue.mqtt.parser.Connect

import de.jkamue.mqtt.parser.MandatoryNormativeStatementTest
import de.jkamue.mqtt.parser.SpecSection
import de.jkamue.mqtt.parser.assertHexStreamParsedToPacket
import de.jkamue.mqtt.parser.assertPacketMalformed
import de.jkamue.mqtt.valueobject.Username
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
}