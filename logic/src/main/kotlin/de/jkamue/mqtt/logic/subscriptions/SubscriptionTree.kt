package de.jkamue.mqtt.logic.subscriptions

import de.jkamue.mqtt.valueobject.ClientId
import de.jkamue.mqtt.valueobject.Topic
import de.jkamue.mqtt.valueobject.TopicFilter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

class SubscriptionTree {

    private val subscriptions = ConcurrentHashMap<ClientId, CopyOnWriteArraySet<TopicFilter>>()
    private val root = SubscriptionTreeNode()

    fun addSubscription(subscriptionWithClient: SubscriptionWithClient) {
        val set = subscriptions.computeIfAbsent(subscriptionWithClient.clientId) { CopyOnWriteArraySet() }
        set.add(subscriptionWithClient.subscription.topicFilter)
        root.addSubscription(subscriptionWithClient, subscriptionWithClient.subscription.topicFilter)
    }

    fun removeSubscriptionsFor(clientId: ClientId) {
        subscriptions[clientId]?.forEach { root.removeSubscription(clientId, it) }
    }

    fun getSubscriptionsForTopic(topic: Topic): List<SubscriptionWithClient> {
        return root.getSubscriptionsForTopic(topic)
    }

    override fun toString(): String {
        return root.toString()
    }
}