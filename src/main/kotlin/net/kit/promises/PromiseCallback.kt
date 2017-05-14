package net.kit.promises

interface PromiseCallback<T> {
    fun onResult(t: T)

    fun onError(e: Exception)
}
