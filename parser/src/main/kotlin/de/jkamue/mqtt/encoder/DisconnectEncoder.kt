package de.jkamue.mqtt.encoder

import de.jkamue.mqtt.packet.DisconnectPacket
import java.nio.ByteBuffer

object DisconnectEncoder {
    
    fun encodeScatter(packet: DisconnectPacket): Array<ByteBuffer> {
        val buffer = ByteBuffer.allocate(1 + 1 + 1 + 1) // Remaining control type + length + variable length + reason

        buffer.put(0b11100000.toByte()) // header
        buffer.put(0b00000011.toByte()) // remaining length
        buffer.put((packet.reasonCode.value and 0xFF).toByte()) // reason code
        buffer.put(0b00000000.toByte()) // properties length

        buffer.flip()

        return arrayOf(buffer)
    }
}