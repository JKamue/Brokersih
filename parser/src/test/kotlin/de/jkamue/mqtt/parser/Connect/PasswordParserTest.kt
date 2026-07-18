package de.jkamue.mqtt.parser.Connect

import de.jkamue.mqtt.parser.MandatoryNormativeStatementTest
import de.jkamue.mqtt.parser.SpecSection
import de.jkamue.mqtt.parser.assertHexStreamParsedToPacket
import de.jkamue.mqtt.parser.assertPacketMalformed
import de.jkamue.mqtt.valueobject.Password
import kotlin.test.Test

@SpecSection("3.1.2.9")
class PasswordParserTest {

    val flagAndPasswordSet = "00044d51545405 42 003c0511000000000008636c69656e744964 000470617373"
    val onlyPasswordNoFlag = "00044d51545405 02 003c0511000000000008636c69656e744964 000470617373"
    val onlyFlagNoPassword = "00044d51545405 42 003c0511000000000008636c69656e744964"

    @Test
    fun `can parse password if flag is set and password is present`() {
        val expectedPacket = testConnectPacket(password = Password("pass"))
        assertHexStreamParsedToPacket(flagAndPasswordSet, expectedPacket)
    }

    @Test
    @MandatoryNormativeStatementTest("MQTT-3.1.2-18")
    fun `throws exception if password flag is set to 0 but password is present`() {
        assertPacketMalformed(onlyPasswordNoFlag)
    }

    @Test
    @MandatoryNormativeStatementTest("MQTT-3.1.2-19")
    fun `throws exception if password flag is set to 1 but password is not present`() {
        assertPacketMalformed(onlyFlagNoPassword)
    }
}