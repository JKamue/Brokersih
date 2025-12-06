package de.jkamue.mqtt.logic

import de.jkamue.mqtt.valueobject.ClientId
import kotlinx.coroutines.channels.SendChannel

class Client(
    val id: ClientId,
    val sendChannel: SendChannel<OutgoingMessage>
)