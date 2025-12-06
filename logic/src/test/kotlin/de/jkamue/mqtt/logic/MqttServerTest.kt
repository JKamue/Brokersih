package de.jkamue.mqtt.logic

import de.jkamue.mqtt.ConnectReasonCode
import de.jkamue.mqtt.DisconnectReasonCode
import de.jkamue.mqtt.logic.clients.ClientManager
import de.jkamue.mqtt.logic.helpers.TestMqttServerConfig
import de.jkamue.mqtt.logic.helpers.TestServer
import de.jkamue.mqtt.logic.helpers.packets.testConnectPacket
import de.jkamue.mqtt.logic.subscriptions.SubscriptionTree
import de.jkamue.mqtt.packet.ConnackPacket
import de.jkamue.mqtt.packet.DisconnectPacket
import de.jkamue.mqtt.packet.PingreqPacket
import de.jkamue.mqtt.packet.PingrespPacket
import de.jkamue.mqtt.valueobject.ClientId
import de.jkamue.mqtt.valueobject.QualityOfService
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MqttServerTest {

    private val serverConfig = TestMqttServerConfig()

    @Test
    fun `client can connect to server`() = runTest {
        TestServer(this, serverConfig).use { testServer ->
            // given
            val clientId = ClientId("newClient")
            val connectPacket = testConnectPacket()
            val expectedConnackPacket = ConnackPacket(
                sessionPresent = false,
                connectReasonCode = ConnectReasonCode.SUCCESS,
                maximumQualityOfService = QualityOfService.AT_MOST_ONCE_DELIVERY
            )

            // when
            val client = testServer.connectClient(clientId)
            testServer.drain()
            testServer.sendPacket(clientId, connectPacket)
            testServer.drain()

            // then
            val connacksReceivedByFirstClient = client.received.map { it.packet }
                .filterIsInstance<ConnackPacket>()
            assertEquals(1, connacksReceivedByFirstClient.size)
            assertEquals(expectedConnackPacket, connacksReceivedByFirstClient.first())
        }
    }

    @Test
    fun `server cleans up if client disconnects`() = runTest {
        // given
        val clientId = ClientId("disconnectingClient")
        val clientManager = mockk<ClientManager>(relaxUnitFun = true)
        val subscriptionTree = mockk<SubscriptionTree>(relaxUnitFun = true)

        // when
        TestServer(this, serverConfig, clientManager, subscriptionTree).use { testServer ->
            testServer.clientDisconnected(clientId)
            testServer.drain()
        }

        // then
        verify { clientManager.removeClient(clientId) }
        verify { subscriptionTree.removeSubscriptionsFor(clientId) }
        confirmVerified(clientManager, subscriptionTree)
    }

    @Test
    fun `server disconnects client if instructed to`() = runTest {
        // given
        val clientId = ClientId("clientToDisconnect")
        val clientManager = mockk<ClientManager>(relaxUnitFun = true)
        val subscriptionTree = mockk<SubscriptionTree>(relaxUnitFun = true)
        val disconnectReasonCode = DisconnectReasonCode.KEEP_ALIVE_TIMEOUT
        val expectedDisconnectPacket = DisconnectPacket(reasonCode = disconnectReasonCode)

        TestServer(this, serverConfig, clientManager, subscriptionTree).use { testServer ->
            val client = testServer.manage(clientId)
            client.startCollecting(this)
            every { clientManager.getById(clientId) } returns client.client

            // when
            testServer.disconnectClient(clientId, reasonCode = disconnectReasonCode)
            testServer.drain()

            // then
            verify { clientManager.removeClient(clientId) }
            verify { subscriptionTree.removeSubscriptionsFor(clientId) }
            verify { clientManager.getById(clientId) }
            confirmVerified(clientManager, subscriptionTree)
            assertEquals(1, client.received.count())
            val disconnectsReceivedByClient = client.received.map { it.packet }
                .filterIsInstance<DisconnectPacket>()
            assertEquals(expectedDisconnectPacket, disconnectsReceivedByClient.first())
        }
    }

    @Test
    @MandatoryNormativeStatementTest("MQTT-3.12.4-1")
    fun `server can play ping pong`() = runTest {
        // given
        val clientId = ClientId("pingPongClient")
        val clientManager = mockk<ClientManager>(relaxUnitFun = true)

        TestServer(this, serverConfig, clientManager).use { testServer ->
            val client = testServer.manage(clientId)
            client.startCollecting(this)
            every { clientManager.getById(clientId) } returns client.client

            // when
            testServer.sendPacket(clientId, PingreqPacket)
            testServer.drain()

            // then
            verify { clientManager.getById(clientId) }
            confirmVerified(clientManager)
            assertEquals(1, client.received.count())
            assertEquals(PingrespPacket, client.received.first().packet)
        }
    }

    @Test
    @MandatoryNormativeStatementTest("MQTT-3.1.4-3")
    fun `existing client is disconnected when new client with same id connects`() = runTest {
        TestServer(this, serverConfig).use { testServer ->
            // given
            val clientId = ClientId("sameForTwoClients")
            val expectedDisconnectPacket = DisconnectPacket(reasonCode = DisconnectReasonCode.SESSION_TAKEN_OVER)

            // when
            val firstClient = testServer.connectClient(clientId)
            testServer.drain()
            val secondClient = testServer.connectClient(clientId)
            testServer.drain()

            // then
            assertEquals(1, firstClient.received.size)
            assertEquals(expectedDisconnectPacket, firstClient.received.first().packet)
            assertTrue(secondClient.received.none { it.packet is DisconnectPacket })
        }
    }
}
