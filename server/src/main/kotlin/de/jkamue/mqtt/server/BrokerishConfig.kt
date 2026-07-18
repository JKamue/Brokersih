package de.jkamue.mqtt.server

import de.jkamue.mqtt.logic.MqttServerConfig
import de.jkamue.mqtt.valueobject.QualityOfService
import io.ktor.server.config.*

@kotlinx.serialization.Serializable
data class BrokerishConfig(
    override val maximumQualityOfService: QualityOfService,
    val keepAliveTimeoutMultiplier: Double,
    val port: Int,
) : MqttServerConfig {
    companion object {
        fun create(config: ApplicationConfig): BrokerishConfig {
            val brokerishConfig = config.config("brokerish")
            return BrokerishConfig(
                maximumQualityOfService = brokerishConfig.property("maximumQualityOfService").getAs(),
                keepAliveTimeoutMultiplier = brokerishConfig.property("keepAliveTimeoutMultiplier").getAs(),
                port = brokerishConfig.property("port").getAs(),
            )
        }
    }
}