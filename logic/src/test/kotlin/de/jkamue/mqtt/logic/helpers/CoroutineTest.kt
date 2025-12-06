package de.jkamue.mqtt.logic.helpers

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

abstract class CoroutineTest {
    fun coroutineTest(block: suspend TestScope.() -> Unit) = runTest { block() }
}
