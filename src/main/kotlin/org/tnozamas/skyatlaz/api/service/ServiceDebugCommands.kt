package org.tnozamas.skyatlaz.api.service

import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import org.tnozamas.skyatlaz.core.command.CommandRegistry
import org.tnozamas.skyatlaz.core.scheduler.Scheduler
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.chat.Chat
import java.io.IOException
import java.nio.file.Files
import java.util.Base64

/**
 * `/skyatlaz debug service`: authenticates against the skyatlaz service and shows what it answers. The service is
 * the only holder of the Hypixel developer key, so this is the way to check that the whole chain works.
 *
 * `/skyatlaz debug key`: writes the profile key pair of the account to a file. The service checks the signature
 * Mojang produced over the public key, and the exact bytes Mojang signed are not public, so the file is what tells
 * which layout is the right one. Nothing secret goes into it: the public key and the signature of Mojang are sent to
 * every server the player joins.
 */
class ServiceDebugCommands(
    private val service: ServiceClient,
    private val scheduler: Scheduler,
) {
    fun register(registry: CommandRegistry) {
        registry.register {
            literal("debug") {
                literal("service") {
                    description("command.debug.service")
                    executes { run() }
                }
                literal("key") {
                    description("command.debug.key")
                    executes { dumpKey() }
                }
            }
        }
    }

    private fun run() {
        Chat.info(Tr.tr("debug.service.authenticating"))
        service.authenticate { result ->
            result.fold(
                onSuccess = { session ->
                    Chat.info(Tr.tr("debug.service.authenticated"))
                    service.fetchProfile(session) { profile ->
                        profile.fold(
                            onSuccess = { report(it) },
                            onFailure = { failed(it) },
                        )
                    }
                },
                onFailure = { failed(it) },
            )
        }
    }

    private fun report(profile: ServiceProfile) {
        Chat.info(
            Tr.tr(
                "debug.service.profile",
                Tr.verbatim(
                    profile.mining.perks.size
                        .toString(),
                ),
                Tr.verbatim(profile.mining.ability ?: "-"),
                Tr.verbatim(
                    profile.mining.powder.size
                        .toString(),
                ),
            ),
        )
    }

    private fun failed(error: Throwable) {
        Chat.error(Tr.tr("debug.service.failed", Tr.verbatim(error.message ?: error.toString())))
    }

    private fun dumpKey() {
        Chat.info(Tr.tr("debug.key.reading"))
        scheduler.async(
            work = {
                val material = ProfileKeyMaterial.current() ?: throw IOException("the account has no profile keys")
                val json =
                    JsonObject().apply {
                        addProperty("uuid", material.uuid)
                        addProperty("public_key", Base64.getEncoder().encodeToString(material.publicKey))
                        addProperty("public_key_signature", Base64.getEncoder().encodeToString(material.keySignature))
                        addProperty("expires_at", material.expiresAt)
                    }
                val directory =
                    FabricLoader
                        .getInstance()
                        .gameDir
                        .resolve("skyatlaz")
                        .resolve("dumps")
                Files.createDirectories(directory)
                val file = directory.resolve("service-key.json")
                Files.writeString(file, json.toString())
                file
            },
            onError = { failed(it) },
        ) { file -> Chat.info(Tr.tr("debug.key.saved", Tr.verbatim(file.toString()))) }
    }
}
