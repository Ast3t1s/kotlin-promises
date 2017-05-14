package net.kit.promises

import net.kit.functions.*
import net.kit.functions.Function
import java.util.*

class Promise<T> {

    private val callbacks = ArrayList<PromiseCallback<T>>()
    private val dispatcher: SimpleDispatcher

    @Volatile
    private var result: T? = null
    @Volatile
    private var exception: Exception? = null
    @Volatile
    private var isFinished: Boolean = false

    constructor(executor: PromiseFunction<T>) {
        this.dispatcher = DispatcherStorage.getDispatcher()
        executor.submit(PromisePermit(this))
    }

    private constructor(value: T) {
        this.dispatcher = DispatcherStorage.getDispatcher()
        this.result = value
        this.exception = null
        this.isFinished = true
    }

    private constructor(e: Exception) {
        this.dispatcher = DispatcherStorage.getDispatcher()
        this.result = null
        this.exception = e
        this.isFinished = true
    }

    @Synchronized
    fun then(then: Consumer<T>): Promise<T> {
        if (isFinished) {
            if (exception == null) {
                dispatcher.dispatch { then.apply(result) }
            }
        } else {
            callbacks.add(object : PromiseCallback<T> {
                override fun onResult(t: T) {
                    then.apply(t)
                }

                override fun onError(e: Exception) {

                }
            })
        }
        return this
    }

    @Synchronized
    fun failure(failure: Consumer<Exception>): Promise<T> {
        if (isFinished) {
            if (exception != null) {
                dispatcher.dispatch {
                    failure.apply(exception)
                }
            }
        } else {
            callbacks.add(object : PromiseCallback<T> {
                override fun onResult(t: T) {

                }

                override fun onError(e: Exception) {
                    failure.apply(e)
                }
            })
        }
        return this
    }

    @Synchronized
    internal fun error(e: Exception) {
        if (isFinished) {
            throw RuntimeException("Promise already completed")
        }
        if (e == null) {
            throw RuntimeException("Error can't be null")
        }
        isFinished = true
        exception = e
        sendResult()
    }

    @Synchronized
    internal fun tryError(e: Exception) {
        if (isFinished) {
            return
        }
        error(e)
    }

    @Synchronized
    internal fun result(res: T?) {
        if (isFinished) {
            throw RuntimeException("Promise " + this + " already completed!")
        }
        isFinished = true
        result = res
        sendResult()
    }

    @Synchronized
    internal fun tryResult(res: T?) {
        if (isFinished) {
            return
        }
        result(res)
    }

    internal fun sendResult() {
        if (callbacks.size > 0) {
            dispatcher.dispatch { receiveResult() }
        }
    }

    internal fun receiveResult() {
        if (exception != null) {
            for (callback in callbacks) {
                try {
                    callback.onError(exception!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            for (callback in callbacks) {
                try {
                    callback.onResult(result!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }
        callbacks.clear()
    }

    fun <R> map(res: Function<T, R>): Promise<R> {
        val self = this
        return Promise(PromiseFunction<R> { resolver ->
            self.then(Consumer<T> { t ->
                val r: R
                try {
                    r = res.apply(t)
                } catch (e: Exception) {
                    e.printStackTrace()
                    resolver.tryError(e)
                    return@Consumer
                }

                resolver.tryResult(r)
            })
            self.failure(Consumer<Exception> {
                e -> resolver.error(e)
            })
        })
    }

    fun <R> flatMap(res: Function<T, Promise<R>>): Promise<R> {
        val self = this
        return Promise(PromiseFunction<R> { resolver ->
            self.then(Consumer<T> { t ->
                val promise: Promise<R>
                try {
                    promise = res.apply(t)
                } catch (e: Exception) {
                    e.printStackTrace()
                    resolver.tryError(e)
                    return@Consumer
                }

                promise.then(Consumer<R> {
                    t2 -> resolver.result(t2)
                })
                promise.failure(Consumer<Exception> {
                    e -> resolver.error(e)
                })
            })
            self.failure(Consumer<Exception> {
                e -> resolver.error(e)
            })
        })
    }

    fun <R> chain(res: Function<T, Promise<R>>): Promise<T> {
        val self = this
        return Promise(PromiseFunction<T> { resolver ->
            self.then(Consumer<T> { t ->
                val chained = res.apply(t)
                chained.then(Consumer<R> {
                    resolver.result(t)
                })
                chained.failure(Consumer<Exception> {
                    e -> resolver.error(e)
                })
            })
            self.failure(Consumer<Exception> {
                e -> resolver.error(e)
            })
        })
    }

    fun after(afterHandler: ConsumerDouble<T, Exception>): Promise<T> {
        then(Consumer<T> {
            t -> afterHandler.apply(t, null)
        })
        failure(Consumer<Exception> {
            e -> afterHandler.apply(null, e)
        })
        return this
    }

    fun pipeTo(resolver: PromisePermit<T>): Promise<T> {
        then(Consumer<T> {
            t -> resolver.result(t)
        })
        failure(Consumer<Exception> {
            e -> resolver.error(e)
        })
        return this
    }

    fun mapIfNull(producer: Supplier<T>): Promise<T> {
        val self = this
        return Promise(PromiseFunction<T> { resolver ->
            self.then(Consumer<T> { t ->
                var t = t
                if (t == null) {
                    try {
                        t = producer.get()
                    } catch (e: Exception) {
                        resolver.error(e)
                        return@Consumer
                    }

                    resolver.result(t)
                } else {
                    resolver.result(t)
                }
            })
            self.failure(Consumer<Exception> {
                e -> resolver.error(e)
            })
        })
    }

    fun mapIfNullPromise(producer: Supplier<Promise<T>>): Promise<T> {
        val self = this
        return Promise(PromiseFunction<T> { resolver ->
            self.then(Consumer<T> { t ->
                if (t == null) {
                    val promise: Promise<T>
                    try {
                        promise = producer.get()
                    } catch (e: Exception) {
                        resolver.error(e)
                        return@Consumer
                    }

                    promise.then(Consumer<T> {
                        t2 -> resolver.result(t2)
                    })
                    promise.failure(Consumer<Exception> {
                        e -> resolver.error(e)
                    })
                } else {
                    resolver.result(t)
                }
            })
            self.failure(Consumer<Exception> {
                e -> resolver.error(e)
            })
        })
    }

    companion object {

        fun <T> success(value: T): Promise<T> {
            return Promise(value)
        }

        fun <T> failure(e: Exception): Promise<T> {
            return Promise(e)
        }
    }
}
