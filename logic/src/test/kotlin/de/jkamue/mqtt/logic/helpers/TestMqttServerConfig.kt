package de.jkamue.mqtt.logic.helpers

import de.jkamue.mqtt.logic.MqttServerConfig
import de.jkamue.mqtt.valueobject.QualityOfService

class TestMqttServerConfig : MqttServerConfig {
    override val maximumQualityOfService: QualityOfService = QualityOfService.AT_MOST_ONCE_DELIVERY
}