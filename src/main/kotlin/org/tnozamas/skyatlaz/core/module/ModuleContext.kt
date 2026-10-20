package org.tnozamas.skyatlaz.core.module

import org.tnozamas.skyatlaz.api.profile.ProfileClient
import org.tnozamas.skyatlaz.api.repo.NeuRepository
import org.tnozamas.skyatlaz.api.repo.PriceRepository
import org.tnozamas.skyatlaz.core.command.CommandBuilder
import org.tnozamas.skyatlaz.core.config.ConfigEntry
import org.tnozamas.skyatlaz.core.event.ChatReceivedEvent
import org.tnozamas.skyatlaz.core.event.Event
import org.tnozamas.skyatlaz.core.event.Registration
import org.tnozamas.skyatlaz.core.keybind.KeyBind
import org.tnozamas.skyatlaz.ui.hud.HudElement
import org.tnozamas.skyatlaz.util.ChatMatch
import org.tnozamas.skyatlaz.util.ChatPattern

/**
 * What a running module uses to hook into shared systems. Every registration is tracked and released when the
 * module stops, so modules never unregister by hand and a disabled module leaves nothing behind.
 *
 * Errors thrown by the module's listeners and tasks are routed to its manager, which disables the module.
 */
class ModuleContext internal constructor(
    private val services: ModuleServices,
    private val onError: (Throwable) -> Unit,
) {
    private val registrations = ArrayList<Registration>()
    private var closed = false

    /** Listens to events of exactly [type] while the module runs. Lower [priority] runs first. */
    fun <E : Event> on(
        type: Class<E>,
        priority: Int = 0,
        listener: (E) -> Unit,
    ) {
        track(services.events.register(type, priority, onError, listener))
    }

    /**
     * Runs [listener] for chat lines (not the action bar) matching [pattern] while the module runs. The pattern's
     * prefilter keeps non-matching lines to a `contains` check.
     */
    fun onChat(
        pattern: ChatPattern,
        priority: Int = 0,
        listener: (match: ChatMatch, event: ChatReceivedEvent) -> Unit,
    ) {
        on(ChatReceivedEvent::class.java, priority) { event ->
            if (!event.actionBar) pattern.match(event.text)?.let { listener(it, event) }
        }
    }

    /** Reacts to changes of a setting while the module runs (recompute derived data here, not every frame). */
    fun <T : Any> watch(
        entry: ConfigEntry<T>,
        listener: (T) -> Unit,
    ) {
        track(entry.onChange(listener))
    }

    /**
     * Keeps bazaar prices and the item catalog loaded and refreshing while the module runs, and returns them. Listen
     * to `PricesUpdatedEvent` to recompute displays when new data arrives.
     */
    fun usePrices(): PriceRepository {
        track(services.prices.use())
        return services.prices
    }

    /**
     * The profile of the player, read from the Hypixel API. It needs the developer API key of the player; without
     * it the answers are empty and nothing is requested.
     */
    fun useProfile(): ProfileClient = services.profiles

    /**
     * The community data repository: the pages of the items, their recipes and the layout of the mining tree. It is
     * lazy: a page is downloaded only when a feature asks for it, so nothing has to be released.
     */
    fun useNeu(): NeuRepository = services.neu

    /** Shows [element] on the HUD and lets the player move it with `/skyatlaz hud` while the module runs. */
    fun hud(element: HudElement) {
        track(services.hud.register(element))
    }

    /**
     * Listens to a key while the module runs. The binding disappears with the module, and the key the player chose
     * is saved in the config.
     */
    fun keyBind(
        bind: KeyBind,
        onPress: () -> Unit,
    ) {
        track(services.keybinds.register(bind, onPress))
    }

    /**
     * Declares `/skyatlaz` subcommands that exist only while the module runs (see [CommandBuilder]). They show up
     * in the generated help under their category.
     */
    fun command(block: CommandBuilder.() -> Unit) {
        track(services.commands.register(block))
    }

    inline fun <reified E : Event> on(
        priority: Int = 0,
        noinline listener: (E) -> Unit,
    ) = on(E::class.java, priority, listener)

    /** Runs [action] once after [delayTicks] ticks, unless the module stops first. */
    fun later(
        delayTicks: Int,
        action: () -> Unit,
    ) {
        track(services.scheduler.later(delayTicks, onError, action))
    }

    /** Runs [action] every [intervalTicks] ticks while the module runs. The way to throttle expensive checks. */
    fun every(
        intervalTicks: Int,
        initialDelayTicks: Int = intervalTicks,
        action: () -> Unit,
    ) {
        track(services.scheduler.every(intervalTicks, initialDelayTicks, onError, action))
    }

    /** Runs [work] in the background; [onResult] runs on the client thread only if the module is still running. */
    fun <T> async(
        work: () -> T,
        onResult: (T) -> Unit,
    ) {
        services.scheduler.async(work, { if (!closed) onError(it) }) { if (!closed) onResult(it) }
    }

    /**
     * Ties a registration from another shared system (HUD, commands, scheduler) to the module's lifetime.
     * Registering after the module stopped (e.g. from a late async callback) releases it immediately.
     */
    fun track(registration: Registration) {
        if (closed) registration.unregister() else registrations.add(registration)
    }

    /** Releases every registration, newest first. */
    internal fun close() {
        closed = true
        for (index in registrations.indices.reversed()) registrations[index].unregister()
        registrations.clear()
    }
}
