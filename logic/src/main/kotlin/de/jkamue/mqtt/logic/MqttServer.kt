package de.jkamue.mqtt.logic

import de.jkamue.mqtt.ConnectReasonCode
import de.jkamue.mqtt.DisconnectReasonCode
import de.jkamue.mqtt.logic.clients.ClientManager
import de.jkamue.mqtt.logic.subscriptions.SubscriptionTree
import de.jkamue.mqtt.logic.subscriptions.SubscriptionWithClient
import de.jkamue.mqtt.packet.*
import de.jkamue.mqtt.valueobject.QualityOfService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class MqttServer(
    scope: CoroutineScope,
    private val config: MqttServerConfig,
    private val clientManager: ClientManager = ClientManager(),
    private val subscriptionTree: SubscriptionTree = SubscriptionTree(),
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    val commandChannel = Channel<ServerCommand>(Channel.UNLIMITED)

    init {
        scope.launch(dispatcher) {
            for (command in commandChannel) {
                when (command) {
                    is ClientConnected -> {
                        val client = Client(command.clientId, command.outgoing)
                        if (clientManager.clientExists(client.id)) {
                            val disconnectMsg =
                                OutgoingMessage(DisconnectPacket(DisconnectReasonCode.SESSION_TAKEN_OVER))
                            clientManager.getById(client.id)?.sendChannel?.trySend(disconnectMsg)
                        }
                        clientManager.addClient(client)
                    }

                    is ClientDisconnected -> {
                        subscriptionTree.removeSubscriptionsFor(command.clientId)
                        clientManager.removeClient(command.clientId)
                        // TODO: Publish Will message by sending to other client channels
                    }

                    is DisconnectClient -> {
                        val disconnectMsg = OutgoingMessage(DisconnectPacket(command.reasonCode))
                        clientManager.getById(command.clientId)?.sendChannel?.trySend(disconnectMsg)
                        subscriptionTree.removeSubscriptionsFor(command.clientId)
                        clientManager.removeClient(command.clientId)
                    }

                    is PacketReceived -> {
                        handlePacket(command)
                    }
                }
            }
        }
    }

    fun close() {
        commandChannel.close()
    }

    private suspend fun handlePacket(command: PacketReceived) {
        val (clientId, packet, payloadManager) = command
        val client = clientManager.getById(command.clientId) ?: run {
            // If client is not found, we must release the payload
            payloadManager.getReleaseAction().invoke()
            return
        }

        when (packet) {
            is ConnectPacket -> {
                val response = ConnackPacket(
                    sessionPresent = false, // TODO: session handling
                    connectReasonCode = ConnectReasonCode.SUCCESS,
                    maximumQualityOfService = config.maximumQualityOfService
                )
                payloadManager.getReleaseAction().invoke()
                client.sendChannel.send(OutgoingMessage(response))
            }

            is PingreqPacket -> {
                payloadManager.getReleaseAction().invoke()
                client.sendChannel.send(OutgoingMessage(PingrespPacket))
            }

            is SubscribePacket -> {
                val response = SubackPacket(
                    packetIdentifier = packet.packetIdentifier,
                    reasonCodes = packet.subscriptions.map { requestedSubscription ->
                        val chosenQoS = minOf(
                            config.maximumQualityOfService.number,
                            requestedSubscription.options.qualityOfService.number
                        ).let { QualityOfService.fromInt(it) }
                        SubackReasonCode.fromQoS(chosenQoS)
                    }
                )
                client.sendChannel.send(OutgoingMessage(response))
                payloadManager.getReleaseAction().invoke()
                packet.subscriptions.forEach {
                    subscriptionTree.addSubscription(
                        SubscriptionWithClient(it, packet.subscriptionIdentifier, clientId)
                    )
                }
            }

            is PublishPacket -> {
                val subscriptions = subscriptionTree.getSubscriptionsForTopic(packet.topic)
                if (subscriptions.isEmpty()) {
                    payloadManager.getReleaseAction().invoke()
                    return
                } else {
                    val sharedReleaseAction = payloadManager.getSharedReleaseAction(subscriptions.size)
                    subscriptions.forEach {
                        val packetToSend = if (it.subscriptionIdentifier != null) {
                            packet.copy(properties = packet.properties.copy(subscriptionIdentifier = it.subscriptionIdentifier))
                        } else {
                            packet
                        }
                        val message = OutgoingMessage(packetToSend, sharedReleaseAction)
                        clientManager.getById(it.clientId)?.sendChannel?.send(message) ?: sharedReleaseAction.invoke()
                    }
                }
            }

            else -> {
                // For any other packet type, we don't know what to do, so just release the resource.
                payloadManager.getReleaseAction().invoke()
            }
        }
    }
}
