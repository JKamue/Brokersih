package de.jkamue.mqtt.encoder

import de.jkamue.mqtt.packet.SubackPacket
import java.nio.ByteBuffer

object SubackEncoder {
    fun encodeScatter(packet: SubackPacket): Array<ByteBuffer> {

        val reasonCodeLength = packet.reasonCodes.count()

        val variableAndPayloadLength =
            // variable header
            2 + // packet identifier
                    1 + // property length (0)
                    // payload
                    reasonCodeLength // amount of reason codes

        val remainingLength = MqttEncoderHelpers.variableByteIntSize(variableAndPayloadLength)
        val packetLength = 1 + remainingLength + variableAndPayloadLength

        val buffer = ByteBuffer.allocate(packetLength)

        buffer.put(0b10010000.toByte()) // control packet type + reserved
        MqttEncoderHelpers.encodeVariableByteIntegerToBuffer(variableAndPayloadLength, buffer)
        MqttEncoderHelpers.encodeTwoByteInt(packet.packetIdentifier, buffer)
        buffer.put(0b00000000.toByte()) // property length
        packet.reasonCodes.forEach { buffer.put((it.value and 0xFF).toByte()) }

        buffer.flip()

        return arrayOf(buffer)
    }
}