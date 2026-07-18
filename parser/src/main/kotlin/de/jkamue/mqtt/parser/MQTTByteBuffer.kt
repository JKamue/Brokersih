package de.jkamue.mqtt.parser

import de.jkamue.mqtt.MalformedPacketMqttException
import java.nio.ByteBuffer
import java.nio.CharBuffer

private val decoderThreadLocal = ThreadLocal.withInitial {
    Charsets.UTF_8.newDecoder()
}

@JvmInline
internal value class MQTTByteBuffer(val buffer: ByteBuffer) {

    companion object {
        fun wrap(bytes: ByteArray): MQTTByteBuffer =
            MQTTByteBuffer(ByteBuffer.wrap(bytes).asReadOnlyBuffer())

        fun wrap(buffer: ByteBuffer): MQTTByteBuffer =
            MQTTByteBuffer(buffer.asReadOnlyBuffer())
    }

    private fun ensureRemaining(needed: Int) {
        if (needed > buffer.remaining()) {
            throw MalformedPacketMqttException("Expected Packet to have $needed more bytes")
        }
    }

    fun getUnsignedByte(): Int {
        ensureRemaining(1)
        return buffer.get().toInt() and 0xFF
    }

    fun getTwoByteInt(): Int {
        val msb = getUnsignedByte()
        val lsb = getUnsignedByte()
        return (msb shl 8) or lsb
    }

    fun getFourByteInt(): Int {
        ensureRemaining(4)
        return buffer.getInt()
    }

    fun getNextBytesAsBuffer(length: Int): MQTTByteBuffer {
        return wrap(getNextBytes(length))
    }

    private fun getNextBytes(length: Int): ByteBuffer {
        ensureRemaining(length)
        val slice = buffer.slice()
        slice.limit(length)
        buffer.position(buffer.position() + length)
        return slice
    }

    // To get small strings the logic needs to work with like topic names or client ids
    fun getEncodedString(): String {
        val length = getTwoByteInt()
        if (length == 0) return ""
        return getString(length)
    }

    fun getString(length: Int? = null): String {
        val byteSlice = getNextBytes(length ?: buffer.remaining())
        return decoderThreadLocal.get().decode(byteSlice).toString()
    }

    // Large messages like the payload that the logic will only ever pass on
    fun getEncodedCharBuffer(): CharBuffer {
        val length = getTwoByteInt()
        if (length == 0) return CharBuffer.wrap("")

        val byteSlice = getNextBytes(length)
        val decodedView = decoderThreadLocal.get().decode(byteSlice)

        val standaloneBuffer = CharBuffer.allocate(decodedView.remaining())
        standaloneBuffer.put(decodedView)
        standaloneBuffer.flip()
        return standaloneBuffer.asReadOnlyBuffer()
    }

    fun getVariableByteInteger(): Int {
        var multiplier = 1
        var value = 0
        var bytesRead = 0
        do {
            val encodedByte = getUnsignedByte()
            value += (encodedByte and 0b01111111) * multiplier
            multiplier *= 128
            bytesRead++
            if (bytesRead > 4) throw MalformedPacketMqttException("Malformed Variable Byte Integer")
        } while ((encodedByte and 0x80) != 0)
        return value
    }

    fun getBinaryData(): ByteBuffer {
        val length = getTwoByteInt()
        return getNextBytes(length)
    }

    fun decodeCompleteBuffer(): String {
        return decoderThreadLocal.get().decode(buffer).toString()
    }

    fun remaining(): Int = buffer.remaining()

    fun position(): Int = buffer.position()
}

fun ByteBuffer.createCopy(): ByteBuffer {
    val copy = ByteArray(this.remaining())
    val duplicate = this.duplicate()
    duplicate.get(copy)
    return ByteBuffer.wrap(copy).asReadOnlyBuffer()
}