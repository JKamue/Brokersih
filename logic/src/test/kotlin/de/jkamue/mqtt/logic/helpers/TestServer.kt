package de.jkamue.mqtt.logic.helpers

import de.jkamue.mqtt.DisconnectReasonCode
import de.jkamue.mqtt.logic.*
import de.jkamue.mqtt.logic.clients.ClientManager
import de.jkamue.mqtt.logic.subscriptions.SubscriptionTree
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
    val config: MqttServerConfig,
    val clientManager: ClientManager = ClientManager(),
    val subscriptionTree: SubscriptionTree = SubscriptionTree()
) : Closeable {

    private val managedCloseables = mutableListOf<TestClient>()
    val server =
        MqttServer(
            scope = scope,
            config = config,
            clientManager = clientManager,
            subscriptionTree = subscriptionTree,
            dispatcher = scope.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
        )

    fun connectClient(id: ClientId): TestClient {
        val conn = TestClient(id)
        managedCloseables.add(conn)
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

    fun manage(clientId: ClientId): TestClient {
        val client = TestClient(clientId)
        managedCloseables.add(client)
        return client
    }

    fun clientDisconnected(id: ClientId) {
        scope.launch {
            server.commandChannel.send(ClientDisconnected(id))
        }
    }

    fun disconnectClient(id: ClientId, reasonCode: DisconnectReasonCode) {
        scope.launch {
            server.commandChannel.send(DisconnectClient(id, reasonCode))
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
        managedCloseables.forEach { it.close() }
        server.close()
    }
}