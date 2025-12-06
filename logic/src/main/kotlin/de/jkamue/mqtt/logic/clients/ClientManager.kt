package de.jkamue.mqtt.logic.clients

import de.jkamue.mqtt.logic.Client
import de.jkamue.mqtt.valueobject.ClientId
import java.util.concurrent.ConcurrentHashMap

class ClientManager {
    private val clients = ConcurrentHashMap<ClientId, Client>()

    fun clientExists(clientId: ClientId): Boolean {
        return clients.containsKey(clientId)
    }

    fun addClient(client: Client) {
        clients[client.id] = client
    }

    fun removeClient(clientId: ClientId) {
        clients.remove(clientId)
    }

    fun getById(clientId: ClientId): Client? {
        return clients[clientId]
    }

}