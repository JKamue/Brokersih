package de.jkamue.mqtt.logic.helpers

import de.jkamue.mqtt.logic.OutgoingMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.Closeable

class TestClientConnection : Closeable {
    val received = mutableListOf<OutgoingMessage>()
    val channel = Channel<OutgoingMessage>(Channel.UNLIMITED)

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