package org.tnozamas.skyatlaz.core.config

import net.fabricmc.loader.api.FabricLoader

/** Mod-wide settings that belong to no feature. */
object GeneralConfig : ConfigSection("general") {
    /** Shows `/skyatlaz debug` tools. On by default only in the dev environment. */
    val debugMode = boolean("debug_mode", FabricLoader.getInstance().isDevelopmentEnvironment)
}
