package de.jkamue.mqtt.parser.Connect

import de.jkamue.mqtt.packet.ConnectProperties
import de.jkamue.mqtt.parser.SpecSection
import de.jkamue.mqtt.parser.assertConnectHexMalformed
import de.jkamue.mqtt.parser.assertConnectHexParsedToPacket
import de.jkamue.mqtt.parser.assertConnectHexProtocolError
import de.jkamue.mqtt.valueobject.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.ByteBuffer
import kotlin.test.Test

@SpecSection("3.1.2.11")
class ConnectPacketPropertiesTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("singleProperties")
    fun `single property is parsed`(name: String, propertyHex: String, expected: ConnectProperties) {
        assertConnectHexParsedToPacket(
            connectPacketWithProperties(propertyHex),
            testConnectPacket(properties = expected)
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("duplicatableProperties")
    fun `duplicated property is a protocol error`(name: String, propertyHex: String) {
        assertConnectHexProtocolError(connectPacketWithProperties("$propertyHex $propertyHex"))
    }

    @Test
    fun `user properties may appear multiple times and accumulate`() {
        val fooBar = "26 0003 666f6f 0003 626172"
        val keyVal = "26 0003 6b6579 0003 76616c"
        val expected = testConnectProperties(
            userProperties = UserProperties(
                mapOf(
                    "foo" to listOf("bar"),
                    "key" to listOf("val")
                )
            )
        )
        assertConnectHexParsedToPacket(
            connectPacketWithProperties("$fooBar $keyVal"),
            testConnectPacket(properties = expected)
        )
    }

    @Test
    fun `user property with same key twice keeps both values`() {
        val fooBar = "26 0003 666f6f 0003 626172"
        val fooBaz = "26 0003 666f6f 0003 62617a"
        val expected = testConnectProperties(
            userProperties = UserProperties(mapOf("foo" to listOf("bar", "baz")))
        )
        assertConnectHexParsedToPacket(
            connectPacketWithProperties("$fooBar $fooBaz"),
            testConnectPacket(properties = expected)
        )
    }

    @Test
    fun `unknown property identifier is malformed`() {
        assertConnectHexMalformed(connectPacketWithProperties("7f 00"))
    }

    @Test
    @SpecSection("3.1.2.11.3")
    fun `receive maximum of zero is a protocol error`() {
        assertConnectHexProtocolError(connectPacketWithProperties("21 0000"))
    }

    @Test
    @SpecSection("3.1.2.11.4")
    fun `maximum packet size of zero is a protocol error`() {
        assertConnectHexProtocolError(connectPacketWithProperties("27 00000000"))
    }

    companion object {
        // @formatter:off
        @JvmStatic
        fun singleProperties(): List<Arguments> = listOf(
            case("session expiry interval",        "11 0000003c",       testConnectProperties(sessionExpiry = Interval(60))),
            case("receive maximum",                "21 0005",           testConnectProperties(receiveMaximum = ReceiveMaximum(5))),
            case("maximum packet size",            "27 00000400",       testConnectProperties(maximumPacketSize = MaximumPacketSize(1024))),
            case("topic alias maximum",            "22 000a",           testConnectProperties(topicAliasMaximum = TopicAliasMaximum(10))),
            case("request response info true",     "19 01",             testConnectProperties(requestResponseInformation = RequestResponseInformation(true))),
            case("request response info false",    "19 00",             testConnectProperties(requestResponseInformation = RequestResponseInformation(false))),
            case("request problem info true",      "17 01",             testConnectProperties(requestProblemInformation = RequestProblemInformation(true))),
            case("request problem info false",     "17 00",             testConnectProperties(requestProblemInformation = RequestProblemInformation(false))),
            case("single user property",           "26 0003 666f6f 0003 626172", testConnectProperties(userProperties = UserProperties(mapOf("foo" to listOf("bar"))))),
            case("authentication method",          "15 0004 61757468",  testConnectProperties(authenticationMethod = AuthenticationMethod("auth"))),
            case("authentication data",            "16 0004 01020304",  testConnectProperties(authenticationData = ByteBuffer.wrap(byteArrayOf(1, 2, 3, 4)))),
        )

        // User property is deliberately absent: it MAY appear multiple times (3.1.2.11.8)
        @JvmStatic
        fun duplicatableProperties(): List<Arguments> = listOf(
            Arguments.of("session expiry interval", "11 0000003c"),
            Arguments.of("receive maximum",         "21 0005"),
            Arguments.of("maximum packet size",     "27 00000400"),
            Arguments.of("topic alias maximum",     "22 000a"),
            Arguments.of("request response info",   "19 01"),
            Arguments.of("request problem info",    "17 01"),
            Arguments.of("authentication method",   "15 0004 61757468"),
            Arguments.of("authentication data",     "16 0004 01020304"),
        )

        private fun case(name: String, propertyHex: String, expected: ConnectProperties): Arguments =
            Arguments.of(name, propertyHex, expected)
    }

    /**
     * Base CONNECT packet (clean start, keep alive 60, client id "clientId") with the
     * given properties section spliced in. Only the property length byte is computed,
     * everything else stays literal hex.
     */
    internal fun connectPacketWithProperties(propertiesHex: String): String {
        val propertyBytes = propertiesHex.filterNot { it.isWhitespace() }.length / 2
        require(propertyBytes < 0x80) { "Keep test property sections under 128 bytes" }
        return "0004 4d515454 05 02 003c " +
                "%02x ".format(propertyBytes) + propertiesHex +
                " 0008 636c69656e744964"
    }
}
