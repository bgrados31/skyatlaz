package org.tnozamas.skyatlaz.core.module

/**
 * A feature that can be turned on and off at runtime. The [ModuleManager] owns the lifecycle: a module runs only
 * while its config toggle is on **and** [isActive] holds, and everything registered through the [ModuleContext]
 * in [onEnable] is removed automatically when it stops. A stopped module must cost nothing.
 *
 * [id] doubles as the config and translation namespace (`config.<id>.name`), so it is lowercase and dot
 * separated, e.g. `dungeons.scorehud`.
 */
abstract class Module(
    val id: String,
    val category: FeatureCategory,
) {
    init {
        require(ID_PATTERN.matches(id)) { "Invalid module id '$id': lowercase segments separated by dots" }
    }

    /**
     * Activation condition (e.g. "on SkyBlock", "in Dungeons"), re-evaluated by [ModuleManager.refreshActivation]
     * when game state changes. Must be cheap and side-effect free.
     */
    open fun isActive(): Boolean = true

    /** Registers listeners, HUD elements and loads data. Register everything through [ctx]. */
    protected open fun onEnable(ctx: ModuleContext) {}

    /** Releases anything not tracked by the context (caches, open screens). Listeners are already handled. */
    protected open fun onDisable() {}

    internal fun dispatchEnable(ctx: ModuleContext) = onEnable(ctx)

    internal fun dispatchDisable() = onDisable()

    override fun toString(): String = id

    private companion object {
        val ID_PATTERN = Regex("[a-z0-9_]+(\\.[a-z0-9_]+)*")
    }
}
