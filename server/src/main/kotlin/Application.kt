package de.jkamue

import BrokerishConfig
import BufferPool
import ReferenceCountedRelease
import de.jkamue.mqtt.DisconnectReasonCode
import de.jkamue.mqtt.KeepAliveTimeoutMqttException
import de.jkamue.mqtt.MalformedPacketMqttException
import de.jkamue.mqtt.logic.*
import de.jkamue.mqtt.packet.ConnectPacket
import de.jkamue.mqtt.packet.ControlPacketType
import de.jkamue.mqtt.packet.DisconnectPacket
import de.jkamue.mqtt.packet.Packet
import de.jkamue.mqtt.valueobject.ClientId
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.server.config.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import mqtt.encoder.PacketEncoder
import mqtt.parser.PacketParser
import java.nio.ByteBuffer
import kotlin.system.measureNanoTime
import kotlin.time.Duration.Companion.seconds

/**
 * Concrete implementation of PayloadManager that manages a leased ByteBuffer.
 */
private class BufferPayloadManager(private val buffer: ByteBuffer) : PayloadManager {
    override fun getReleaseAction(): () -> Unit = { BufferPool.release(buffer) }
    override fun getSharedReleaseAction(count: Int): () -> Unit = ReferenceCountedRelease(buffer, count)
}

fun main() {
    val config = ApplicationConfig("application.yaml")
    val brokerishConfig = BrokerishConfig.create(config)

    runBlocking {
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val serverJob = SupervisorJob()
        val serverScope = CoroutineScope(Dispatchers.IO + serverJob)

        val mqttServer = MqttServer(serverScope, brokerishConfig)

        aSocket(selectorManager).tcp().bind("127.0.0.1", brokerishConfig.port).use { serverSocket ->
            log("MQTT-listening server is running at ${serverSocket.localAddress}")

            while (true) {
                val socket = serverSocket.accept()
                val remoteAddress = socket.remoteAddress

                serverScope.launch {
                    log("Accepted connection from $remoteAddress")
                    val readChannel = socket.openReadChannel()
                    val writeChannel = socket.openWriteChannel(autoFlush = false)
                    val outgoingPackets = Channel<OutgoingMessage>(Channel.BUFFERED)
                    var clientId: ClientId? = null // Hold clientId for the finally block

                    var buffer: ByteBuffer? = null

                    try {
                        val packetAndBuffer = readMqttPacket(readChannel)
                        if (packetAndBuffer == null || packetAndBuffer.first !is ConnectPacket) {
                            log("First packet was not CONNECT, closing connection.")
                            BufferPool.release(packetAndBuffer!!.second)
                            socket.close()
                            return@launch
                        }
                        val firstPacket = packetAndBuffer.first as ConnectPacket
                        val leasedBuffer = packetAndBuffer.second
                        buffer = leasedBuffer
                        clientId = firstPacket.clientId
                        val keepAlive =
                            (firstPacket.keepAlive.duration * brokerishConfig.keepAliveTimeoutMultiplier).seconds

                        val payloadManager = BufferPayloadManager(leasedBuffer)
                        mqttServer.commandChannel.send(ClientConnected(clientId, outgoingPackets))
                        mqttServer.commandChannel.send(PacketReceived(clientId, firstPacket, payloadManager))
                        buffer = null


                        // Writer coroutine
                        launch {
                            for (message in outgoingPackets) {
                                log("Sending packet ${message.packet.packetType} to $clientId.")
                                val encoded = PacketEncoder.encodeScatter(message.packet)
                                sendScatter(writeChannel, encoded)
                                message.afterSend()

                                if (message.packet is DisconnectPacket) {
                                    socket.close()
                                }
                            }
                        }

                        // Reader coroutine
                        try {
                            while (socket.isActive) {
                                val packetAndBuffer = withTimeoutOrNull(keepAlive) {
                                    readMqttPacket(readChannel)
                                } ?: throw KeepAliveTimeoutMqttException("")

                                val subsequentPayloadManager = BufferPayloadManager(packetAndBuffer.second)
                                mqttServer.commandChannel.send(
                                    PacketReceived(
                                        clientId,
                                        packetAndBuffer.first,
                                        subsequentPayloadManager
                                    )
                                )
                            }
                        } catch (_: KeepAliveTimeoutMqttException) {
                            log("No packet received within keepalive $keepAlive including tolerance for $remoteAddress")
                            clientId.let {
                                mqttServer.commandChannel.send(
                                    DisconnectClient(
                                        it,
                                        DisconnectReasonCode.KEEP_ALIVE_TIMEOUT
                                    )
                                )
                            }
                            delay(1000)
                        }
                    } catch (e: Exception) {
                        log("Error in client coroutine for $remoteAddress: ${e.message}")
                        clientId?.let { mqttServer.commandChannel.send(ClientDisconnected(it)) }
                        delay(1000)
                    } finally {
                        log("Closing connection for $remoteAddress")
                        outgoingPackets.close()
                        socket.close()
                        buffer?.let { BufferPool.release(it) }
                    }
                }
            }
        }
    }
}


/**
 * Reads a single MQTT Control Packet from the provided ByteReadChannel into the given ByteBuffer.
 * The buffer is used to read the packet content directly from the socket.
 *
 * Returns null when the channel is closed / EOF reached.
 */
internal suspend fun readMqttPacket(channel: ByteReadChannel): Pair<Packet, ByteBuffer>? {
    var buffer: ByteBuffer? = null
    try {
        val controlPacketType = try {
            readControlPacketType(channel)
        } catch (e: java.io.EOFException) {
            log("readMqttPacket: EOF while reading first byte -> connection closed by peer")
            return null
        }
        log("Starting to receive packet of type $controlPacketType")

        buffer = BufferPool.lease()
        val content = try {
            getPacketContent(channel, buffer)
        } catch (e: java.io.EOFException) {
            log("readMqttPacket: EOF while reading remaining length / payload -> connection closed by peer")
            BufferPool.release(buffer)
            return null
        }

        lateinit var packet: Packet
        val parsingTimeNanos = measureNanoTime {
            packet = PacketParser.parsePacket(content, controlPacketType)
        }
        val parsingTimeMicros = parsingTimeNanos / 1000.0
        log("Parsing took $parsingTimeNanos ns (or $parsingTimeMicros µs).")
        return Pair(packet, buffer)
    } catch (ex: MalformedPacketMqttException) {
        // rethrow known MQTT protocol errors to be handled by caller if desired
        buffer?.let { BufferPool.release(buffer) }
        throw ex
    } catch (ex: Throwable) {
        // unexpected errors: log and rethrow so outer handler can close the socket and print stack trace
        buffer?.let { BufferPool.release(buffer) }
        log("readMqttPacket: unexpected error: ${ex.message}")
        throw ex
    }
}

suspend fun readControlPacketType(channel: ByteReadChannel): ControlPacketType {
    val firstByte = channel.readByte()
    return ControlPacketType.detect(firstByte.toInt() and 0xFF)
}

suspend fun getPacketContent(channel: ByteReadChannel, buffer: ByteBuffer): ByteBuffer {
    val packetBodyLength = getPacketContentLength(channel)
    if (packetBodyLength < 0) throw MalformedPacketMqttException("Negative content length")
    if (packetBodyLength > buffer.capacity()) throw MalformedPacketMqttException("Package too large for buffer")

    buffer.limit(packetBodyLength)
    channel.readFully(buffer)
    buffer.flip()
    return buffer.slice().asReadOnlyBuffer()
}

suspend fun getPacketContentLength(channel: ByteReadChannel): Int {
    // Variable Length decoding as described in 1.5.5
    var multiplier = 1
    var contentLength = 0
    val remainingLengthBytes = mutableListOf<Byte>()
    do {
        val nextByte = channel.readByte()
        val encoded = nextByte.toInt() and 0b11111111
        remainingLengthBytes.add(nextByte)
        contentLength += (encoded and 0b01111111) * multiplier
        multiplier *= 128
        if (multiplier > 128 * 128 * 128) throw MalformedPacketMqttException("Malformed Variable Byte Integer")
    } while ((remainingLengthBytes.last().toInt() and 0b10000000) != 0)
    return contentLength
}


suspend fun sendScatter(socketWrite: ByteWriteChannel, parts: Array<ByteBuffer>) {
    for (part in parts) {
        part.rewind()
        socketWrite.writeFully(part)
    }
}

private fun log(msg: String) {
    AsyncLogger.log(msg)
}