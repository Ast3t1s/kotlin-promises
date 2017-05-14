package net.kit.promises

class PromisePermit<T> internal constructor(val promise: Promise<T>) {

    fun result(res: T?) {
        promise.result(res)
    }

    fun tryResult(res: T?) {
        promise.tryResult(res)
    }

    fun error(e: Exception) {
        promise.error(e)
    }

    fun tryError(e: Exception) {
        promise.tryError(e)
    }

}
