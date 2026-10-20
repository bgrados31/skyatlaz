package org.tnozamas.skyatlaz.feature.mining.hotm

import org.tnozamas.skyatlaz.core.config.ConfigSection
import org.tnozamas.skyatlaz.util.NumberStyle

/** The settings of the mining card. */
object HotmConfig : ConfigSection("mining.hotm") {
    /** Shows how much powder is left. */
    val showPowders = boolean("show_powders", true)

    /** Shows the pickaxe ability that is equipped. */
    val showAbility = boolean("show_ability", true)

    /** How big numbers are written. */
    val numberStyle = choice("number_style", NumberStyle.COMPACT)
}
