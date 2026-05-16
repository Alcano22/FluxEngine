package org.flux.core.util

object MainThreadQueue {

    private val queue = ArrayDeque<() -> Unit>()

    fun post(action: () -> Unit) {
        synchronized(queue) {
            queue.addLast(action)
        }
    }

    fun flush() {
        synchronized(queue) {
            while (queue.isNotEmpty())
                queue.removeFirst()()
        }
    }
}
