package de.jkamue.mqtt.encoder

import java.nio.ByteBuffer

internal object MqttEncoderHelpers {

    fun encodeVariableByteIntegerToBuffer(value: Int, buffer: ByteBuffer) {
        var v = value
        do {
            var encoded = (v % 128)
            v /= 128
            if (v > 0) encoded = encoded or 0x80
            buffer.put(encoded.toByte())
        } while (v > 0)
    }

    fun variableByteIntegerLength(value: Int): Int {
        var v = value
        var count = 1
        while (v >= 128) {
            v /= 128
            count++
        }
        return count
    }

    fun encodeTwoByteInt(int: Int, buffer: ByteBuffer) {
        buffer.put(intToByte(int ushr 8))
        buffer.put(intToByte(int))
    }

    // encode two-byte length prefixed UTF-8 string
    fun encodeUtf8StringUtf8Bytes(s: String): ByteArray {
        val bytes = s.toByteArray(Charsets.UTF_8)
        val out = ByteArray(2 + bytes.size)
        out[0] = ((bytes.size ushr 8) and 0xFF).toByte()
        out[1] = (bytes.size and 0xFF).toByte()
        System.arraycopy(bytes, 0, out, 2, bytes.size)
        return out
    }

    // size in bytes when encoding a variable byte integer
    fun variableByteIntSize(value: Int): Int {
        var v = value
        var count = 0
        do {
            v = v / 128
            count++
        } while (v > 0)
        return count
    }

    fun intToByte(value: Int): Byte {
        return (value and 0xFF).toByte()
    }
}
