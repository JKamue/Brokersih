package de.jkamue.mqtt.parser

import de.jkamue.mqtt.MalformedPacketMqttException
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction

private val decoderThreadLocal = ThreadLocal.withInitial {
    Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
}

private val charBufferThreadLocal = ThreadLocal.withInitial { CharBuffer.allocate(65535) }

@JvmInline
internal value class MQTTByteBuffer(val buffer: ByteBuffer) {

    companion object {
        fun wrap(bytes: ByteArray): MQTTByteBuffer =
            MQTTByteBuffer(ByteBuffer.wrap(bytes).asReadOnlyBuffer())

        fun wrap(buffer: ByteBuffer): MQTTByteBuffer =
            MQTTByteBuffer(buffer.asReadOnlyBuffer())

        fun validateMqttString(src: ByteBuffer): CharBuffer {
            val dec = decoderThreadLocal.get().reset()
            val cb = charBufferThreadLocal.get().also { it.clear() }

            try {
                dec.decode(src, cb, true).let {
                    if (it.isOverflow) throw IllegalStateException("...")
                    else if (it.isError) it.throwException()
                }
                dec.flush(cb)
                cb.flip()
            } catch (e: CharacterCodingException) {
                throw MalformedPacketMqttException("ill-formed UTF-8 [MQTT-1.5.4-1] cause: ${e.message}")
            }

            val a = cb.array()
            val off = cb.arrayOffset() + cb.position()
            val end = off + cb.remaining()

            var i = off
            while (i < end) {
                val cp = Character.codePointAt(a, i, end)

                // fast path
                if (cp in 0x20..0x7E) {
                    i++; continue
                }

                when {
                    cp == 0x0000 ->
                        throw MalformedPacketMqttException("U+0000 not allowed [MQTT-1.5.4-2]")

                    cp < 0x20 || cp in 0x7F..0x9F ->
                        throw MalformedPacketMqttException("control character U+%04X".format(cp))

                    cp in 0xFDD0..0xFDEF ->
                        throw MalformedPacketMqttException("noncharacter U+%04X".format(cp))

                    (cp and 0xFFFE) == 0xFFFE ->
                        throw MalformedPacketMqttException("noncharacter U+%04X".format(cp))
                }

                i += Character.charCount(cp)
            }

            return cb
        }
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
        val n = length ?: buffer.remaining()
        if (n == 0) return ""
        val src = getNextBytes(n)
        return validateMqttString(src).toString()
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