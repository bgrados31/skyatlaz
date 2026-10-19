package org.tnozamas.skyatlaz.core.module

import org.tnozamas.skyatlaz.core.error.rethrowIfFatal

/**
 * Owns every module's lifecycle. A module runs while it is enabled (config toggle), active ([Module.isActive])
 * and not failed. Any exception from a module (enable, disable or one of its listeners) stops it and marks it
 * failed until it is toggled off and on again; [onFailure] reports it (log + chat notice), the game keeps going.
 *
 * Client thread only.
 */
class ModuleManager(
    private val services: ModuleServices,
    private val onFailure: (module: Module, error: Throwable) -> Unit,
) {
    private val slots = LinkedHashMap<String, Slot>()

    private inner class Slot(
        val module: Module,
    ) {
        var enabled = false
        var failure: Throwable? = null
        var context: ModuleContext? = null

        /** Built once per slot so listener error routing allocates nothing per registration. */
        val errorHandler: (Throwable) -> Unit = { fail(this, it) }
    }

    val modules: Collection<Module>
        get() = slots.values.map { it.module }

    /** Adds [module], initially disabled. Ids are unique. */
    fun register(module: Module) {
        require(module.id !in slots) { "Duplicate module id '${module.id}'" }
        slots[module.id] = Slot(module)
    }

    /** Applies the config toggle. Turning a failed module off clears its failure so it can be retried. */
    fun setEnabled(
        id: String,
        enabled: Boolean,
    ) {
        val slot = slot(id)
        if (slot.enabled == enabled) return
        slot.enabled = enabled
        if (!enabled) slot.failure = null
        update(slot)
    }

    /** Re-evaluates every activation condition. Called when game state (island, area...) changes. */
    fun refreshActivation() {
        for (slot in slots.values) update(slot)
    }

    fun isEnabled(id: String): Boolean = slot(id).enabled

    fun isRunning(id: String): Boolean = slot(id).context != null

    fun failure(id: String): Throwable? = slot(id).failure

    private fun slot(id: String): Slot = slots[id] ?: throw IllegalArgumentException("Unknown module id '$id'")

    private fun update(slot: Slot) {
        val shouldRun = slot.enabled && slot.failure == null && safeIsActive(slot)
        val running = slot.context != null
        if (shouldRun && !running) {
            start(slot)
        } else if (!shouldRun && running) {
            stop(slot)
        }
    }

    private fun safeIsActive(slot: Slot): Boolean =
        try {
            slot.module.isActive()
        } catch (error: Throwable) {
            error.rethrowIfFatal()
            fail(slot, error)
            false
        }

    private fun start(slot: Slot) {
        val context = ModuleContext(services, slot.errorHandler)
        slot.context = context
        try {
            slot.module.dispatchEnable(context)
        } catch (error: Throwable) {
            error.rethrowIfFatal()
            fail(slot, error)
        }
    }

    private fun stop(slot: Slot) {
        val context = slot.context ?: return
        slot.context = null
        try {
            slot.module.dispatchDisable()
        } catch (error: Throwable) {
            error.rethrowIfFatal()
            recordFailure(slot, error)
        } finally {
            context.close()
        }
    }

    private fun fail(
        slot: Slot,
        error: Throwable,
    ) {
        recordFailure(slot, error)
        stop(slot)
    }

    private fun recordFailure(
        slot: Slot,
        error: Throwable,
    ) {
        if (slot.failure != null) return
        slot.failure = error
        onFailure(slot.module, error)
    }
}
