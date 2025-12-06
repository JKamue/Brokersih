package de.jkamue.mqtt.logic.helpers

import de.jkamue.mqtt.logic.PayloadManager

class DummyPayloadManager : PayloadManager {
    override fun getReleaseAction(): () -> Unit = {}
    override fun getSharedReleaseAction(count: Int): () -> Unit = {}
}