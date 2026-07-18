package de.jkamue.mqtt.parser

import de.jkamue.mqtt.MalformedPacketMqttException
import de.jkamue.mqtt.ProtocolErrorMqttException
import de.jkamue.mqtt.packet.ConnectPacket
import de.jkamue.mqtt.parser.connect.ConnectPacketParser
import java.nio.ByteBuffer
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal fun String.toMqttBuffer(): MQTTByteBuffer {
    val hex = filterNot { it.isWhitespace() }
    require(hex.length % 2 == 0) { "Hex string must have an even number of characters" }
    val bytes = ByteArray(hex.length / 2) { i ->
        hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
    return MQTTByteBuffer.Companion.wrap(bytes)
}

// Wraps the literal text as UTF-8 bytes (unlike toConnectMqttBuffer, which decodes hex)
internal fun String.toByteBuffer(): ByteBuffer =
    ByteBuffer.wrap(toByteArray(Charsets.UTF_8)).asReadOnlyBuffer()

internal fun assertConnectHexParsedToPacket(hexStream: String, expectedParsedPacket: ConnectPacket) {
    val parsedPacket = ConnectPacketParser.parseConnectPacket(hexStream.toMqttBuffer())
    assertEquals(expectedParsedPacket, parsedPacket)
}

internal fun assertConnectHexMalformed(hexStream: String) {
    assertFailsWith(MalformedPacketMqttException::class) {
        ConnectPacketParser.parseConnectPacket(hexStream.toMqttBuffer())
    }
}

internal fun assertConnectHexProtocolError(hexStream: String) {
    assertFailsWith(ProtocolErrorMqttException::class) {
        ConnectPacketParser.parseConnectPacket(hexStream.toMqttBuffer())
    }
}

internal fun assertHexStreamParsedToPacket(hexStream: String, expectedParsedPacket: ConnectPacket) {
    val parsedPacket = ConnectPacketParser.parseConnectPacket(hexStream.toMqttBuffer())
    assertEquals(expectedParsedPacket, parsedPacket)
}

internal fun assertPacketMalformed(hexStream: String) {
    assertFailsWith(MalformedPacketMqttException::class) {
        ConnectPacketParser.parseConnectPacket(hexStream.toMqttBuffer())
    }
}
