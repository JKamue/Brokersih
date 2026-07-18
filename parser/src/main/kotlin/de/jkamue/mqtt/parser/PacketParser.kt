package de.jkamue.mqtt.parser

import de.jkamue.mqtt.packet.ControlPacketType
import de.jkamue.mqtt.packet.Packet
import de.jkamue.mqtt.parser.connect.ConnectPacketParser
import de.jkamue.mqtt.parser.pingreq.PingreqPacketParser
import de.jkamue.mqtt.parser.publish.PublishPacketParser
import de.jkamue.mqtt.parser.subscribe.SubscribePacketParser
import java.nio.ByteBuffer

object PacketParser {
    fun parsePacket(bytes: ByteBuffer, packetType: ControlPacketType): Packet {
        val buffer = MQTTByteBuffer.wrap(bytes)
        return when (packetType) {
            ControlPacketType.CONNECT -> ConnectPacketParser.parseConnectPacket(buffer)
            ControlPacketType.PINGREQ -> PingreqPacketParser.parsePingReqPacket(buffer)
            ControlPacketType.SUBSCRIBE -> SubscribePacketParser.parseSubscribePacket(buffer)
            ControlPacketType.PUBLISH -> PublishPacketParser.parsePublishPacket(buffer)
            else -> throw NotImplementedError("Control Packet $packetType not implemented yet")
        }
    }
}