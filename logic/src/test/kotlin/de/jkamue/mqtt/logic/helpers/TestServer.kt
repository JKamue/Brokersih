package de.jkamue.mqtt.logic.helpers

import de.jkamue.mqtt.logic.*
import de.jkamue.mqtt.packet.Packet
import de.jkamue.mqtt.valueobject.ClientId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import java.io.Closeable
import kotlin.coroutines.ContinuationInterceptor

class TestServer(
    val scope: TestScope,
    val config: MqttServerConfig
) : Closeable {

    private val createdClients = mutableListOf<TestClientConnection>()
    val server =
        MqttServer(
            scope = scope,
            config = config,
            dispatcher = scope.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
        )

    fun connectClient(id: ClientId): TestClientConnection {
        val conn = TestClientConnection()
        createdClients.add(conn)
        conn.startCollecting(scope)
        scope.launch {
            server.commandChannel.send(
                ClientConnected(
                    clientId = id,
                    outgoing = conn.channel
                )
            )
        }
        return conn
    }

    fun disconnectClient(id: ClientId) {
        scope.launch {
            server.commandChannel.send(ClientDisconnected(id))
        }
    }

    fun sendPacket(id: ClientId, packet: Packet, payloadManager: PayloadManager = DummyPayloadManager()) {
        scope.launch {
            server.commandChannel.send(
                PacketReceived(id, packet, payloadManager)
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun drain() {
        // allow processing of all queued commands
        scope.advanceUntilIdle()
    }

    override fun close() {
        createdClients.forEach { it.close() }
        server.close()
    }
}