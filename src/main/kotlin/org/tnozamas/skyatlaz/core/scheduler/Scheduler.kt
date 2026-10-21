package org.tnozamas.skyatlaz.core.scheduler

import org.tnozamas.skyatlaz.core.error.rethrowIfFatal
import org.tnozamas.skyatlaz.core.event.Registration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * The only owner of timing and threads.
 * - Tick tasks ([later], [every]) run on the client thread at the end of each client tick. Use [every] to throttle
 *   expensive checks instead of counting ticks by hand. Ticking allocates nothing.
 * - Background work ([async]) runs on one shared pool; results come back on the client thread.
 * - [runOnClient] is the thread-safe way to hand work to the client thread.
 *
 * Errors from a task go to its own handler (a module's context disables that module) or to [onUnhandledError].
 */
class Scheduler(
    private val executor: Executor,
    private val onUnhandledError: (Throwable) -> Unit,
) {
    private class Task(
        var nextTick: Long,
        val interval: Int,
        val action: () -> Unit,
        val onError: ((Throwable) -> Unit)?,
    ) {
        var cancelled = false
    }

    private var tasks = arrayOfNulls<Task>(16)
    private var size = 0
    private val clientQueue = ConcurrentLinkedQueue<Runnable>()

    /** Client ticks since start. Read on the client thread. */
    var currentTick: Long = 0
        private set

    /** Runs [action] once, [delayTicks] ticks from now (0 = end of the current tick). */
    fun later(
        delayTicks: Int,
        onError: ((Throwable) -> Unit)? = null,
        action: () -> Unit,
    ): Registration {
        require(delayTicks >= 0) { "delayTicks must be >= 0" }
        return add(Task(currentTick + delayTicks, 0, action, onError))
    }

    /** Runs [action] every [intervalTicks] ticks (20 = once per second), first run after [initialDelayTicks]. */
    fun every(
        intervalTicks: Int,
        initialDelayTicks: Int = intervalTicks,
        onError: ((Throwable) -> Unit)? = null,
        action: () -> Unit,
    ): Registration {
        require(intervalTicks > 0) { "intervalTicks must be > 0" }
        require(initialDelayTicks >= 0) { "initialDelayTicks must be >= 0" }
        return add(Task(currentTick + initialDelayTicks, intervalTicks, action, onError))
    }

    /**
     * Runs [work] off the client thread, then [onResult] on the client thread. Errors (from either side) go to
     * [onError] on the client thread. Never touch game state inside [work].
     */
    fun <T> async(
        work: () -> T,
        onError: ((Throwable) -> Unit)? = null,
        onResult: (T) -> Unit,
    ) {
        executor.execute {
            val result =
                try {
                    work()
                } catch (error: Throwable) {
                    error.rethrowIfFatal()
                    runOnClient { report(error, onError) }
                    return@execute
                }
            runOnClient {
                try {
                    onResult(result)
                } catch (error: Throwable) {
                    error.rethrowIfFatal()
                    report(error, onError)
                }
            }
        }
    }

    /** Thread-safe: queues [action] for the start of the next client tick. */
    fun runOnClient(action: Runnable) {
        clientQueue.add(action)
    }

    /** Called once per client tick by the entrypoint (or by tests). */
    fun tick() {
        while (true) {
            val action = clientQueue.poll() ?: break
            try {
                action.run()
            } catch (error: Throwable) {
                error.rethrowIfFatal()
                onUnhandledError(error)
            }
        }

        // Tasks added while ticking are appended past `count` and wait for the next tick.
        val count = size
        var removed = false
        for (index in 0 until count) {
            val task = tasks[index]!!
            if (task.cancelled) {
                removed = true
                continue
            }
            if (task.nextTick > currentTick) continue
            if (task.interval == 0) {
                task.cancelled = true
                removed = true
            } else {
                task.nextTick = currentTick + task.interval
            }
            try {
                task.action()
            } catch (error: Throwable) {
                error.rethrowIfFatal()
                report(error, task.onError)
            }
        }
        if (removed) compact()
        currentTick++
    }

    private fun add(task: Task): Registration {
        if (size == tasks.size) tasks = tasks.copyOf(size * 2)
        tasks[size++] = task
        return Registration { task.cancelled = true }
    }

    private fun compact() {
        var target = 0
        for (index in 0 until size) {
            val task = tasks[index]!!
            if (!task.cancelled) tasks[target++] = task
        }
        for (index in target until size) tasks[index] = null
        size = target
    }

    private fun report(
        error: Throwable,
        handler: ((Throwable) -> Unit)?,
    ) {
        if (handler != null) handler(error) else onUnhandledError(error)
    }

    companion object {
        /** The shared background pool: a few daemon threads, so it never keeps the game alive on exit. */
        fun createExecutor(): Executor {
            val threads = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(2, 4)
            val counter = AtomicInteger()
            return Executors.newFixedThreadPool(threads) { runnable ->
                Thread(runnable, "skyatlaz-Worker-${counter.incrementAndGet()}").apply { isDaemon = true }
            }
        }
    }
}
