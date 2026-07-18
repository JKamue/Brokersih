package de.jkamue.mqtt.parser.subscribe.properties

import de.jkamue.mqtt.valueobject.UserProperties

internal data class SubscribePropertiesBuilder(
    var subscriptionIdentifier: Int? = null,
    var userProperties: MutableMap<String, MutableList<String>> = mutableMapOf(),
) {
    fun build(): Pair<Int?, UserProperties> =
        Pair(subscriptionIdentifier, UserProperties(userProperties))
    
    fun addUserProperty(key: String, value: String) {
        userProperties.getOrPut(key) { mutableListOf() }.add(value)
    }
}