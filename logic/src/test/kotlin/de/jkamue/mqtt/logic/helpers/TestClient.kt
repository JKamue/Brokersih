package de.jkamue.mqtt.logic.helpers

import de.jkamue.mqtt.logic.Client
import de.jkamue.mqtt.logic.OutgoingMessage
import de.jkamue.mqtt.valueobject.ClientId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.Closeable

class TestClient(clientId: ClientId) : Closeable {
    val channel = Channel<OutgoingMessage>(Channel.UNLIMITED)
    val client = Client(clientId, channel)
    val received = mutableListOf<OutgoingMessage>()

    fun startCollecting(scope: CoroutineScope) {
        scope.launch {
            for (msg in channel) {
                received += msg
            }
        }
    }

    override fun close() {
        channel.close()
    }
}

fun testClient(
    clientId: ClientId
): Client {
    val channel = Channel<OutgoingMessage>(Channel.UNLIMITED)
    return Client(
        id = clientId,
        sendChannel = channel,
    )
}