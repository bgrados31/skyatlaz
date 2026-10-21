package org.tnozamas.skyatlaz.feature.mining.hotm

import org.tnozamas.skyatlaz.core.config.ConfigSection

/** The settings of the mining card. */
object HotmConfig : ConfigSection("mining.hotm") {
    /** Shows the pickaxe ability that is equipped. */
    val showAbility = boolean("show_ability", true)
}
