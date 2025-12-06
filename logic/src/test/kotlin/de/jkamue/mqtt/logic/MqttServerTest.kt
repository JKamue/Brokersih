package de.jkamue.mqtt.logic

import de.jkamue.mqtt.ConnectReasonCode
import de.jkamue.mqtt.DisconnectReasonCode
import de.jkamue.mqtt.logic.helpers.CoroutineTest
import de.jkamue.mqtt.logic.helpers.TestMqttServerConfig
import de.jkamue.mqtt.logic.helpers.TestServer
import de.jkamue.mqtt.logic.helpers.packets.testConnectPacket
import de.jkamue.mqtt.packet.ConnackPacket
import de.jkamue.mqtt.packet.DisconnectPacket
import de.jkamue.mqtt.valueobject.ClientId
import de.jkamue.mqtt.valueobject.QualityOfService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MqttServerTest : CoroutineTest() {

    private val serverConfig = TestMqttServerConfig()

    @Test
    fun `client can connect to server`() = coroutineTest {
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
    @MandatoryNormativeStatementTest("MQTT-3.1.4-3")
    fun `existing client is disconnected when new client with same id connects`() = coroutineTest {
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
            val disconnectsReceivedByFirstClient = firstClient.received.map { it.packet }
                .filterIsInstance<DisconnectPacket>()
            assertEquals(1, disconnectsReceivedByFirstClient.size)
            assertEquals(expectedDisconnectPacket, disconnectsReceivedByFirstClient.first())
            assertTrue(secondClient.received.none { it.packet is DisconnectPacket })
        }
    }
}