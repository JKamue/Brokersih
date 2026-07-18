package de.jkamue.mqtt.server

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

/**
 * A release action that uses a reference counter to release the underlying buffer
 * only after a specific number of calls have been made.
 *
 * This is a function (invokable) itself for convenience.
 */
class ReferenceCountedRelease(
    private val buffer: ByteBuffer,
    initialCount: Int
) : () -> Unit {

    private val counter = AtomicInteger(initialCount)

    init {
        if (initialCount <= 0) {
            // If there are no consumers, release immediately.
            release()
        }
    }

    override fun invoke() {
        if (counter.decrementAndGet() == 0) {
            release()
        }
    }

    private fun release() {
        BufferPool.release(buffer)
    }
}
