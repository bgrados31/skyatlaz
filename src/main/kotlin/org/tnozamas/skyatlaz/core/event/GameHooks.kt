package org.tnozamas.skyatlaz.core.event

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents

/**
 * Bridges game callbacks into the [EventBus]. Fabric API events are used wherever they exist (stable across game
 * updates, compatible with other mods); mixins only where Fabric has no hook. Every bridge checks
 * [EventBus.hasListeners] first, so a hook nobody uses costs one map lookup and builds no event.
 */
object GameHooks {
    fun install(bus: EventBus) {
        ClientReceiveMessageEvents.ALLOW_GAME.register { message, overlay ->
            !bus.hasListeners(ChatReceivedEvent::class.java) || !bus.post(ChatReceivedEvent(message, overlay)).isCancelled
        }
        ClientTickEvents.END_CLIENT_TICK.register {
            if (bus.hasListeners(ClientTickEvent::class.java)) bus.post(ClientTickEvent)
        }
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            if (bus.hasListeners(WorldJoinEvent::class.java)) bus.post(WorldJoinEvent)
        }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            if (bus.hasListeners(WorldLeaveEvent::class.java)) bus.post(WorldLeaveEvent)
        }
    }
}
