package org.tnozamas.skyatlaz.ui.config

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import org.tnozamas.skyatlaz.core.config.ConfigEntry
import org.tnozamas.skyatlaz.core.config.ConfigManager
import org.tnozamas.skyatlaz.core.config.ConfigSection
import org.tnozamas.skyatlaz.core.keybind.KeyBindManager
import org.tnozamas.skyatlaz.core.module.ModuleManager
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.i18n.withGradient
import org.tnozamas.skyatlaz.ui.component.UiButton
import org.tnozamas.skyatlaz.ui.component.UiChoice
import org.tnozamas.skyatlaz.ui.component.UiKeyBind
import org.tnozamas.skyatlaz.ui.component.UiList
import org.tnozamas.skyatlaz.ui.component.UiMetrics
import org.tnozamas.skyatlaz.ui.component.UiNote
import org.tnozamas.skyatlaz.ui.component.UiScreen
import org.tnozamas.skyatlaz.ui.component.UiSection
import org.tnozamas.skyatlaz.ui.component.UiSlider
import org.tnozamas.skyatlaz.ui.component.UiTabs
import org.tnozamas.skyatlaz.ui.component.UiTextField
import org.tnozamas.skyatlaz.ui.component.UiToggle
import org.tnozamas.skyatlaz.ui.component.UiWidget
import org.tnozamas.skyatlaz.ui.theme.Spacing
import org.tnozamas.skyatlaz.ui.theme.Theme
import java.math.BigDecimal
import kotlin.math.roundToInt

/**
 * Real settings screen: tabs group the registered [ConfigSection]s by their first id segment, and module toggles
 * live in a trailing "Modules" tab. A tab with one section uses the section title; with several, each section gets
 * its own heading. `hud.*` sections are owned by the HUD editor and stay out of this screen.
 *
 * The screen only maps config metadata to widgets. Saving stays in [ConfigManager] (debounced, atomic) and modules
 * keep reacting to their entries through `onChange`.
 */
class ConfigScreen(
    private val config: ConfigManager,
    private val modules: ModuleManager,
    private val keybinds: KeyBindManager,
) : UiScreen("config.title") {
    private val heading = Tr.tr("config.title").withGradient(Theme.BRAND_GRADIENT)
    private val subtitle = Tr.tr("config.note")
    private val hint = Tr.tr("config.hint")

    private val list = UiList()
    private lateinit var tabBar: UiTabs
    private lateinit var search: UiTextField
    private var chrome: List<UiWidget> = emptyList()

    private var tabs: List<Tab> = emptyList()
    private var tabIndex = 0
    private var query = ""

    private var panelX = 0
    private var panelY = 0
    private var panelWidth = 0
    private var panelHeight = 0

    override fun init() {
        panelWidth = minOf(470, width - 40)
        panelHeight = minOf(420, height - 24)
        panelX = (width - panelWidth) / 2
        panelY = (height - panelHeight) / 2

        tabs = collectTabs()
        tabIndex = tabIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))

        tabBar =
            UiTabs(
                tabs.map { it.titleKey },
                { tabIndex },
                { index ->
                    tabIndex = index
                    list.scrollToTop()
                    rebuild()
                },
            )
        tabBar.layout(panelX + MARGIN, panelY + 52, tabsWidth(), font)

        search =
            UiTextField(
                labelKey = null,
                placeholderKey = "config.search",
                get = { query },
                onChange = {
                    query = it
                    list.scrollToTop()
                    rebuild()
                },
            )
        // The search lives in the title row, so the tabs have the whole width for their names.
        search.layout(panelX + panelWidth - MARGIN - SEARCH_WIDTH, panelY + 12, SEARCH_WIDTH, font)

        val reset =
            UiButton("config.reset", false) {
                resetCurrentTab()
            }
        reset.layout(panelX + panelWidth - MARGIN - 78 - BUTTON_GAP - 78, panelY + panelHeight - 36, 78, font)

        val done =
            UiButton("config.done", true) {
                onClose()
            }
        done.layout(panelX + panelWidth - MARGIN - 78, panelY + panelHeight - 36, 78, font)

        chrome = listOf(tabBar, search, reset, done)
        list.layout(panelX + 14, panelY + 90, panelWidth - 28, panelHeight - 136, font)
        rebuild()
    }

    override fun drawChrome(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
    ) {
        context.fill(panelX + 2, panelY + 3, panelX + panelWidth + 2, panelY + panelHeight + 3, Theme.argb(Theme.SCREEN_DIM, 110))
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, Theme.argb(Theme.SURFACE, 248))
        context.outline(panelX, panelY, panelWidth, panelHeight, Theme.argb(Theme.BORDER, 220))

        context.text(font, heading, panelX + MARGIN, panelY + 13, Theme.argb(Theme.TEXT))
        context.text(font, subtitle, panelX + MARGIN, panelY + 30, Theme.argb(Theme.TEXT_MUTED))
        context.fill(panelX + 14, panelY + 84, panelX + panelWidth - 14, panelY + 85, Theme.argb(Theme.BORDER, 170))
        context.fill(
            panelX + 14,
            panelY + panelHeight - 44,
            panelX + panelWidth - 14,
            panelY + panelHeight - 43,
            Theme.argb(Theme.BORDER, 170),
        )
        val hintX = panelX + MARGIN
        val hintLimit = panelX + panelWidth - MARGIN - 78 - BUTTON_GAP - 78 - 12
        if (font.width(hint) <= hintLimit - hintX) {
            context.text(font, hint, hintX, panelY + panelHeight - 28, Theme.argb(Theme.TEXT_SUBTLE))
        }
    }

    private fun collectTabs(): List<Tab> {
        val grouped = LinkedHashMap<String, MutableList<ConfigSection>>()
        for (section in config.registeredSections) {
            if (RESERVED_SECTIONS.any { section.id == it || section.id.startsWith("$it.") }) continue
            val group = section.id.substringBefore('.')
            grouped.getOrPut(group) { ArrayList() }.add(section)
        }
        val result = ArrayList<Tab>(grouped.size + 2)
        for ((id, sections) in grouped) {
            val withHeadings = sections.size > 1
            result +=
                Tab(
                    titleKey = if (withHeadings) "config.tab.$id" else sections.first().titleKey,
                    groups = sections.map { Group(if (withHeadings) it.titleKey else null, it.entries) },
                )
        }
        val toggles = modules.toggles
        if (toggles.isNotEmpty()) {
            result += Tab("config.tab.modules", listOf(Group(null, toggles.map { it.entry })))
        }
        val bindings = keyBindRows()
        if (bindings.isNotEmpty()) {
            result += Tab("config.tab.controls", listOf(Group(null, widgets = bindings)))
        }
        return result
    }

    /** How wide the tab bar needs to be for the tab names it shows. */
    private fun tabsWidth(): Int {
        val needed = tabs.sumOf { font.width(Tr.tr(it.titleKey)) + UiMetrics.TAB_PADDING } + UiMetrics.TAB_PADDING
        return minOf(needed, panelWidth - MARGIN * 2)
    }

    /** One row per active key binding. */
    private fun keyBindRows(): List<UiWidget> =
        keybinds.active().map { bind ->
            UiKeyBind(
                bind.nameKey,
                bind.descriptionKey,
                { keybinds.comboOf(bind.id) },
                { combo -> keybinds.setCombo(bind.id, combo) },
            )
        }

    private fun rebuild() {
        list.setWidgets(buildRows())
        root.setWidgets(chrome + list)
    }

    private fun buildRows(): List<UiWidget> {
        val tab = tabs.getOrNull(tabIndex) ?: return listOf(UiNote("config.no_results"))
        val rows = ArrayList<UiWidget>()
        for (group in tab.groups) {
            val visible = group.entries.filter(::matches)
            val widgets = group.widgets.filter { matchesText(it.searchText) }
            if (visible.isEmpty() && widgets.isEmpty()) continue
            group.titleKey?.let { rows += UiSection(it) }
            for (entry in visible) rows += widgetFor(entry)
            rows += widgets
        }
        if (rows.isEmpty()) rows += UiNote("config.no_results")
        return rows
    }

    private fun matches(entry: ConfigEntry<*>): Boolean =
        matchesText(Tr.tr(entry.nameKey).string) || matchesText(Tr.tr(entry.descriptionKey).string)

    /** True when the row matches the search, or when the search is empty. */
    private fun matchesText(text: String?): Boolean {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return true
        return text?.lowercase()?.contains(needle) == true
    }

    private fun widgetFor(entry: ConfigEntry<*>): UiWidget =
        when (entry) {
            is ConfigEntry.Bool -> {
                UiToggle(
                    entry.nameKey,
                    entry.descriptionKey,
                    { entry.value },
                    { entry.set(it) },
                )
            }

            is ConfigEntry.Integer -> {
                UiSlider(
                    entry.nameKey,
                    entry.descriptionKey,
                    entry.range.first,
                    entry.range.last,
                    1,
                    { entry.value },
                    { entry.set(it) },
                    { Tr.verbatim(it.toString()) },
                )
            }

            is ConfigEntry.Decimal -> {
                decimalSlider(entry)
            }

            is ConfigEntry.Text -> {
                UiTextField(
                    labelKey = entry.nameKey,
                    placeholderKey = null,
                    get = { entry.value },
                    onChange = { entry.set(it) },
                    descriptionKey = entry.descriptionKey,
                )
            }

            is ConfigEntry.Choice<*> -> {
                choice(entry)
            }
        }

    private fun decimalSlider(entry: ConfigEntry.Decimal): UiWidget {
        val scale = if (entry.range.endInclusive - entry.range.start < 1.0) DECIMAL_SCALE_SMALL else DECIMAL_SCALE
        return UiSlider(
            entry.nameKey,
            entry.descriptionKey,
            (entry.range.start * scale).roundToInt(),
            (entry.range.endInclusive * scale).roundToInt(),
            1,
            { (entry.value * scale).roundToInt() },
            { entry.set(it.toDouble() / scale) },
            { decimalText(it.toDouble() / scale) },
        )
    }

    private fun choice(entry: ConfigEntry.Choice<*>): UiWidget {
        val names = entry.optionNames()
        val options = names.map { name -> Tr.tr("config.${entry.key}.value.${name.lowercase()}") }
        return UiChoice(
            entry.nameKey,
            entry.descriptionKey,
            options,
            { names.indexOf(entry.value.name).coerceAtLeast(0) },
            entry::selectIndex,
        )
    }

    private fun decimalText(value: Double): Component = Tr.verbatim(BigDecimal.valueOf(value).stripTrailingZeros().toPlainString())

    private fun resetCurrentTab() {
        tabs.getOrNull(tabIndex)?.groups?.forEach { group -> group.entries.forEach { it.reset() } }
        list.scrollToTop()
        rebuild()
    }

    private data class Tab(
        val titleKey: String,
        val groups: List<Group>,
    )

    private data class Group(
        val titleKey: String?,
        val entries: List<ConfigEntry<*>> = emptyList(),
        val widgets: List<UiWidget> = emptyList(),
    )

    private companion object {
        /** Sections with their own screen: the HUD editor and the key bindings. */
        val RESERVED_SECTIONS = listOf("hud", "keybind")
        const val DECIMAL_SCALE = 100.0
        const val DECIMAL_SCALE_SMALL = 1000.0
        const val MARGIN = Spacing.MARGIN
        const val BUTTON_GAP = Spacing.PAD_WIDE
        const val SEARCH_WIDTH = 168
    }
}
