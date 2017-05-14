package net.kit.promises

import java.util.concurrent.Executor
import java.util.concurrent.Executors

class SimpleDispatcher : Dispatch {

    private val executor: Executor

    init {
        executor = Executors.newSingleThreadExecutor { r ->
            val thread = Thread(r)
            thread.priority = Thread.MIN_PRIORITY
            thread.name = "PromiseDispatcher#" + hashCode()
            thread
        }
    }

    override fun dispatch(runnable: () -> Unit) {
        executor.execute(runnable)
    }
}
