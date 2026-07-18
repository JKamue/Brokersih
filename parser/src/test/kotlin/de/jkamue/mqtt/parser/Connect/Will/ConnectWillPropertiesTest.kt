package de.jkamue.mqtt.parser.Connect.Will

import de.jkamue.mqtt.PayloadFormatInvalidMqttException
import de.jkamue.mqtt.parser.*
import de.jkamue.mqtt.parser.Connect.testConnectPacket
import de.jkamue.mqtt.parser.Connect.testWill
import de.jkamue.mqtt.parser.Connect.testWillProperties
import de.jkamue.mqtt.parser.connect.ConnectPacketParser
import de.jkamue.mqtt.valueobject.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertFailsWith

@SpecSection("3.1.3.2")
class ConnectWillPropertiesTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("singleProperties")
    fun `single will property is parsed`(name: String, propertyHex: String, expected: WillProperties) {
        assertConnectHexParsedToPacket(
            connectPacketWithWillProperties(propertyHex),
            testConnectPacket(will = testWill(properties = expected))
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("duplicatableProperties")
    fun `duplicated will property is a protocol error`(name: String, propertyHex: String) {
        assertConnectHexProtocolError(connectPacketWithWillProperties("$propertyHex $propertyHex"))
    }

    @Test
    @SpecSection("3.1.3.2.8")
    fun `will user properties may appear multiple times and accumulate`() {
        val fooBar = "26 0003 666f6f 0003 626172"
        val keyVal = "26 0003 6b6579 0003 76616c"
        val expected = testWillProperties(
            userProperties = UserProperties(
                mapOf(
                    "foo" to listOf("bar"),
                    "key" to listOf("val")
                )
            )
        )
        assertConnectHexParsedToPacket(
            connectPacketWithWillProperties("$fooBar $keyVal"),
            testConnectPacket(will = testWill(properties = expected))
        )
    }

    @Test
    @SpecSection("3.1.3.2.8")
    fun `will user property with same key twice keeps both values in order`() {
        val fooBar = "26 0003 666f6f 0003 626172"
        val fooBaz = "26 0003 666f6f 0003 62617a"
        val expected = testWillProperties(
            userProperties = UserProperties(mapOf("foo" to listOf("bar", "baz")))
        )
        assertConnectHexParsedToPacket(
            connectPacketWithWillProperties("$fooBar $fooBaz"),
            testConnectPacket(will = testWill(properties = expected))
        )
    }

    @Test
    fun `unknown will property identifier is malformed`() {
        assertConnectHexMalformed(connectPacketWithWillProperties("7f 00"))
    }

    @Test
    @SpecSection("3.1.3.2.3")
    fun `payload format indicator above 1 is invalid`() {
        assertFailsWith(PayloadFormatInvalidMqttException::class) {
            ConnectPacketParser.parseConnectPacket(
                connectPacketWithWillProperties("01 02").toMqttBuffer()
            )
        }
    }

    companion object {
        // @formatter:off
        @JvmStatic
        fun singleProperties(): List<Arguments> = listOf(
            case("will delay interval",         "18 0000003c",       testWillProperties(willDelayInterval = Interval(60))),
            case("payload format utf8",         "01 01",             testWillProperties(payloadFormat = PayloadFormat.UTF8)),
            case("payload format bytes",        "01 00",             testWillProperties(payloadFormat = PayloadFormat.UNSPECIFIED_BYTES)),
            case("message expiry interval",     "02 00000e10",       testWillProperties(messageExpiryInterval = Interval(3600))),
            case("content type",                "03 0004 6a736f6e",  testWillProperties(contentType = ContentType("json"))),
            case("response topic",              "08 0005 7265706c79",
                                                                     testWillProperties(responseTopic = Topic("reply".toByteBuffer()))),
            case("correlation data",            "09 0004 01020304",  testWillProperties(correlationData = ByteBuffer.wrap(byteArrayOf(1, 2, 3, 4)))),
            case("single user property",        "26 0003 666f6f 0003 626172",
                                                                     testWillProperties(userProperties = UserProperties(mapOf("foo" to listOf("bar"))))),
        )

        // User property is deliberately absent: it MAY appear multiple times (3.1.3.2.8)
        @JvmStatic
        fun duplicatableProperties(): List<Arguments> = listOf(
            Arguments.of("will delay interval",     "18 0000003c"),
            Arguments.of("payload format indicator","01 01"),
            Arguments.of("message expiry interval", "02 00000e10"),
            Arguments.of("content type",            "03 0004 6a736f6e"),
            Arguments.of("response topic",          "08 0005 7265706c79"),
            Arguments.of("correlation data",        "09 0004 01020304"),
        )
        // @formatter:on

        private fun case(name: String, propertyHex: String, expected: WillProperties): Arguments =
            Arguments.of(name, propertyHex, expected)
    }


    /**
     * Base CONNECT packet (will flag + clean start, keep alive 60, client id "clientId",
     * will topic "will", will payload "hi") with the given will properties section spliced in.
     * Only the property length byte is computed, everything else stays literal hex.
     */
    internal fun connectPacketWithWillProperties(willPropertiesHex: String): String {
        val propertyBytes = willPropertiesHex.filterNot { it.isWhitespace() }.length / 2
        require(propertyBytes < 0x80) { "Keep test property sections under 128 bytes" }
        return "0004 4d515454 05 06 003c 05 11 00000000 0008 636c69656e744964 " +
                "%02x ".format(propertyBytes) + willPropertiesHex +
                " 0004 77696c6c 0002 6869"
    }
}