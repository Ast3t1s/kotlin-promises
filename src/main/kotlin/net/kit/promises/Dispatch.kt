package net.kit.promises

interface Dispatch {
    fun dispatch(runnable: () -> Unit)
}
