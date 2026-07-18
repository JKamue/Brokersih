package de.jkamue.mqtt.encoder

import de.jkamue.mqtt.packet.PingrespPacket
import java.nio.ByteBuffer

object PingrespEncoder {
    private val HEADER_BUF: ByteBuffer = ByteBuffer
        .wrap(
            byteArrayOf(
                0b11010000.toByte(), // Header + Reserved
                0b00000000.toByte()
            ) // Remaining Length 0
        )
        .asReadOnlyBuffer()

    fun encodeScatter(packet: PingrespPacket): Array<ByteBuffer> {
        return arrayOf(HEADER_BUF.duplicate())
    }
}