package org.tnozamas.skyatlaz.core.config

import net.fabricmc.loader.api.FabricLoader

/** Mod-wide settings that belong to no feature. */
object GeneralConfig : ConfigSection("general") {
    /** Shows `/skyatlaz debug` tools. On by default only in the dev environment. */
    val debugMode = boolean("debug_mode", FabricLoader.getInstance().isDevelopmentEnvironment)

    /**
     * The Hypixel developer API key of the player. The profile endpoints ask for it in a header; without it the
     * features that need the profile of the player (the perks of the Heart of the Mountain, the dungeons) stay
     * quiet. It is kept in the config file of the player and never leaves the machine.
     */
    val apiKey = text("api_key", "", 64)
}
