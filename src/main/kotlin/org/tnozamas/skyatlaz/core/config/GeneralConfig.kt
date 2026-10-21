package org.tnozamas.skyatlaz.core.config

import net.fabricmc.loader.api.FabricLoader

/** Mod-wide settings that belong to no feature. */
object GeneralConfig : ConfigSection("general") {
    /** Shows `/skyatlaz debug` tools. On by default only in the dev environment. */
    val debugMode = boolean("debug_mode", FabricLoader.getInstance().isDevelopmentEnvironment)

    /** The address of the skyatlaz service, the only holder of the Hypixel developer key. */
    val serviceUrl = text("service_url", "http://127.0.0.1:8080")
}
