package org.tnozamas.skyatlaz.feature.skyblock.statushud

import org.tnozamas.skyatlaz.core.config.ConfigSection
import org.tnozamas.skyatlaz.util.NumberStyle

/**
 * Settings of the SkyBlock status HUD. The on/off switch of the feature is the module's own toggle
 * (`module.skyblock.statushud.enabled`); these entries only decide what the card shows.
 *
 * Every entry gets its texts from the config screen (`config.skyblock.statushud.<entry>.name`).
 */
object StatusHudConfig : ConfigSection("skyblock.statushud") {
    val showArea = boolean("show_area", true)
    val showProfile = boolean("show_profile", true)
    val showLevel = boolean("show_level", true)
    val showPurse = boolean("show_purse", true)
    val showBits = boolean("show_bits", false)
    val showBank = boolean("show_bank", false)
    val numberStyle = choice("number_style", NumberStyle.COMPACT)
}
