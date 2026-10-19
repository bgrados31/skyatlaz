package org.tnozamas.skyatlaz.core.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Runs the registry against a real Brigadier dispatcher, the same way Fabric does in game. */
class CommandRegistryTest {
    private val helped = ArrayList<String>()
    private val failed = ArrayList<String>()
    private var debug = false
    private val registry =
        CommandRegistry(
            rootName = "sa",
            rootDescriptionKey = "command.root",
            debugEnabled = { debug },
            onHelp = { helped.add(it.usage) },
            onError = { node, _ -> failed.add(node.usage) },
        )
    private val dispatcher = CommandDispatcher<Any>()
    private val log = ArrayList<String>()

    init {
        registry.onTreeChanged = { registry.install(dispatcher) }
        registry.install(dispatcher)
    }

    private fun run(input: String) = dispatcher.execute(input, Any())

    private fun suggestions(input: String): List<String> =
        dispatcher
            .getCompletionSuggestions(dispatcher.parse(input, Any()))
            .get()
            .list
            .map { it.text }

    private fun registerTools() =
        registry.register {
            literal("tools") {
                category(CommandCategory.TOOLS)
                description("command.tools")
                literal("calc") {
                    description("command.tools.calc")
                    argument("amount", IntegerArgumentType.integer(1)) {
                        description("command.tools.calc.amount")
                        executes { log.add("calc ${it.int("amount")}") }
                    }
                }
                literal("say") {
                    description("command.tools.say")
                    argument("text", StringArgumentType.word()) {
                        description("command.tools.say.text")
                        suggests { listOf("hello", "help", "bye") }
                        executes { log.add("say ${it.string("text")}") }
                    }
                }
            }
        }

    @Test
    fun `root and every group without an action show help instead of failing`() {
        registerTools()

        run("sa")
        run("sa tools")
        run("sa tools calc")

        assertEquals(listOf("/sa", "/sa tools", "/sa tools calc"), helped)
    }

    @Test
    fun `executes with typed arguments`() {
        registerTools()

        run("sa tools calc 5")
        run("sa tools say hi")

        assertEquals(listOf("calc 5", "say hi"), log)
    }

    @Test
    fun `argument suggestions filter by prefix`() {
        registerTools()

        assertEquals(listOf("help", "hello"), suggestions("sa tools say he").sorted().reversed())
    }

    @Test
    fun `unregistered commands neither run nor complete`() {
        val registration = registerTools()
        registration.unregister()

        assertThrows(CommandSyntaxException::class.java) { run("sa tools calc 5") }
        // In game, Fabric copies only nodes passing canUse into the suggestion tree.
        assertFalse(
            dispatcher.root
                .getChild("sa")
                .getChild("tools")
                .canUse(Any()),
        )
        assertTrue(registry.helpFor(registry.root).isEmpty())
    }

    @Test
    fun `commands can be registered again after removal`() {
        registerTools().unregister()
        registerTools()

        run("sa tools calc 2")

        assertEquals(listOf("calc 2"), log)
    }

    @Test
    fun `shared groups stay while any registration uses them`() {
        val first = registerTools()
        val second =
            registry.register {
                literal("tools") {
                    literal("ping") {
                        description("command.tools.ping")
                        executes { log.add("pong") }
                    }
                }
            }

        first.unregister()
        run("sa tools ping")
        assertEquals(listOf("pong"), log)
        assertThrows(CommandSyntaxException::class.java) { run("sa tools calc 1") }

        second.unregister()
        assertThrows(CommandSyntaxException::class.java) { run("sa tools") }
    }

    @Test
    fun `help groups children by category in declaration order`() {
        registerTools()
        registry.register {
            literal("version") {
                category(CommandCategory.GENERAL)
                description("command.version")
                executes {}
            }
        }

        val sections = registry.helpFor(registry.root)

        assertEquals(listOf(CommandCategory.GENERAL, CommandCategory.TOOLS), sections.map { it.category })
        assertEquals(listOf("/sa version"), sections[0].commands.map { it.usage })
    }

    @Test
    fun `debug commands are hidden unless debug is on`() {
        registry.register {
            literal("debug") {
                category(CommandCategory.DEBUG)
                description("command.debug")
                literal("theme") {
                    description("command.debug.theme")
                    executes { log.add("theme") }
                }
            }
        }

        assertThrows(CommandSyntaxException::class.java) { run("sa debug theme") }
        assertTrue(registry.helpFor(registry.root).isEmpty())

        debug = true
        run("sa debug theme")
        assertEquals(listOf("theme"), log)
    }

    @Test
    fun `visibleWhen hides a command`() {
        var onSkyBlock = false
        registry.register {
            literal("pets") {
                description("command.pets")
                visibleWhen { onSkyBlock }
                executes { log.add("pets") }
            }
        }

        assertThrows(CommandSyntaxException::class.java) { run("sa pets") }
        onSkyBlock = true
        run("sa pets")

        assertEquals(listOf("pets"), log)
    }

    @Test
    fun `a throwing command is reported, not propagated`() {
        registry.register {
            literal("boom") {
                description("command.boom")
                executes { error("broken") }
            }
        }

        run("sa boom")

        assertEquals(listOf("/sa boom"), failed)
    }

    @Test
    fun `usage and suggestion text`() {
        registerTools()
        val amount =
            registry.root.childMap
                .getValue("tools")
                .childMap
                .getValue("calc")
                .childMap
                .getValue("amount")

        assertEquals("/sa tools calc <amount>", amount.usage)
        assertEquals("/sa tools calc", amount.suggestion)
    }

    @Test
    fun `invalid declarations throw and leave the tree untouched`() {
        assertThrows(IllegalArgumentException::class.java) {
            registry.register { literal("nodesc") { executes {} } }
        }
        assertThrows(IllegalArgumentException::class.java) {
            registry.register {
                literal("Bad Name") {
                    description("command.bad")
                }
            }
        }
        registerTools()
        assertThrows(IllegalArgumentException::class.java) {
            registry.register {
                literal("tools") {
                    literal("calc") {
                        argument("amount", IntegerArgumentType.integer()) { executes {} }
                    }
                }
            }
        }

        assertEquals(
            listOf("tools"),
            registry.root.childMap.keys
                .toList(),
        )
        run("sa tools calc 3")
        assertEquals(listOf("calc 3"), log)
    }
}
