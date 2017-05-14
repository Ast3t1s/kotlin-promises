package net.kit.promises

import java.util.*

object DispatcherStorage {
    private val currentDispatcher = ThreadLocal<ArrayList<SimpleDispatcher>>()

    fun getDispatcher(): SimpleDispatcher {
        val currentDispatchers = currentDispatcher.get()
        if (currentDispatchers == null || currentDispatchers.isEmpty()) {
            val dispatcher = SimpleDispatcher()
            val dispatchers = ArrayList<SimpleDispatcher>()
            dispatchers.add(dispatcher)
            currentDispatcher.set(dispatchers)
            return dispatchers[dispatchers.size - 1]
        } else {
            return currentDispatchers[currentDispatchers.size - 1]
        }
    }
}
