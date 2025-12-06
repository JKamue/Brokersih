package de.jkamue.mqtt.logic.helpers

import de.jkamue.mqtt.logic.Client
import de.jkamue.mqtt.logic.OutgoingMessage
import de.jkamue.mqtt.valueobject.ClientId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class TestClient(clientId: ClientId) {
    val received = mutableListOf<OutgoingMessage>()
    val channel = Channel<OutgoingMessage>(Channel.UNLIMITED)
    val client = Client(clientId, channel)

    fun startCollecting(scope: CoroutineScope) {
        scope.launch {
            for (msg in channel) {
                received += msg
            }
        }
    }

    fun close() {
        channel.close()
    }
}