package org.flux.core.util

data class Task(
    val id: String,
    val label: String,
    val progress: Float? = null
)

object TaskManager {

    private val tasks = mutableMapOf<String, Task>()

    fun begin(id: String, label: String) {
        MainThreadQueue.post {
            tasks[id] = Task(id, label)
        }
    }

    fun update(id: String, progress: Float, label: String? = null) {
        MainThreadQueue.post {
            tasks[id] = tasks[id]?.copy(
                progress = progress,
                label = label ?: tasks[id]?.label ?: ""
            ) ?: return@post
        }
    }

    fun finish(id: String) {
        MainThreadQueue.post {
            tasks.remove(id)
        }
    }

    fun getTasks() = synchronized(tasks) { tasks.values.toList() }
}
