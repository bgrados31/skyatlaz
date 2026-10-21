package org.tnozamas.skyatlaz.api.service

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.tnozamas.skyatlaz.api.http.HttpResponse
import org.tnozamas.skyatlaz.api.http.HttpTransport
import org.tnozamas.skyatlaz.core.scheduler.Scheduler
import java.io.IOException
import java.util.Base64

/** A session with the skyatlaz service. */
data class ServiceSession(
    /** The bearer token that proves who the player is. */
    val token: String,
    /** When the token expires, in milliseconds since the epoch. */
    val expiresAt: Long,
)

/**
 * The two calls the repository needs from the service. The client implements it and a fake implements it in the
 * tests, so the repository can be checked without a server.
 */
interface ServiceApi {
    /** Authenticates with the profile key pair. The callback runs on the client thread. */
    fun authenticate(callback: (Result<ServiceSession>) -> Unit)

    /** Reads the profile of the session. The callback runs on the client thread. */
    fun fetchProfile(
        session: ServiceSession,
        callback: (Result<ServiceProfile>) -> Unit,
    )
}

/**
 * Talks to the skyatlaz service, the only holder of the Hypixel developer key.
 *
 * The player never sees a key: the mod signs a challenge with the Minecraft profile key pair and the service
 * answers with a token that only works for that player.
 */
class ServiceClient(
    private val transport: HttpTransport,
    private val scheduler: Scheduler,
    private val baseUrl: () -> String,
) : ServiceApi {
    override fun authenticate(callback: (Result<ServiceSession>) -> Unit) {
        scheduler.async(
            work = { authenticateBlocking() },
            onError = { callback(Result.failure(it)) },
        ) { session -> callback(Result.success(session)) }
    }

    override fun fetchProfile(
        session: ServiceSession,
        callback: (Result<ServiceProfile>) -> Unit,
    ) {
        scheduler.async(
            work = { fetchProfileBlocking(session) },
            onError = { callback(Result.failure(it)) },
        ) { profile -> callback(Result.success(profile)) }
    }

    private fun authenticateBlocking(): ServiceSession {
        val material = ProfileKeyMaterial.current() ?: throw IOException("The account has no profile keys")
        val challenge = readChallenge()
        val signature = material.sign(Base64.getDecoder().decode(challenge.nonce))
        val body =
            JsonObject().apply {
                addProperty("uuid", material.uuid)
                addProperty("public_key", Base64.getEncoder().encodeToString(material.publicKey))
                addProperty("public_key_signature", Base64.getEncoder().encodeToString(material.keySignature))
                addProperty("expires_at", material.expiresAt)
                addProperty("nonce", challenge.nonce)
                addProperty("signature", Base64.getEncoder().encodeToString(signature))
            }
        val response = post("/v1/auth/verify", body)
        val json = readJson(response, "/v1/auth/verify")
        val token = json.get("token")?.takeIf { it.isJsonPrimitive }?.asString ?: throw IOException("The service sent no token")
        val expiresAt = json.get("expires_at")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0L
        return ServiceSession(token, expiresAt)
    }

    private fun fetchProfileBlocking(session: ServiceSession): ServiceProfile {
        val response =
            transport.get(
                "${baseUrl()}/v1/profile",
                mapOf("Authorization" to "Bearer ${session.token}"),
            )
        return parseServiceProfile(readJson(response, "/v1/profile"))
    }

    private fun readChallenge(): Challenge {
        val response = transport.get("${baseUrl()}/v1/auth/challenge", emptyMap())
        val json = readJson(response, "/v1/auth/challenge")
        val nonce = json.get("nonce")?.takeIf { it.isJsonPrimitive }?.asString ?: throw IOException("The service sent no challenge")
        return Challenge(nonce)
    }

    private fun post(
        path: String,
        body: JsonObject,
    ): HttpResponse = transport.post("${baseUrl()}$path", mapOf("Content-Type" to "application/json"), body.toString())

    private fun readJson(
        response: HttpResponse,
        path: String,
    ): JsonObject {
        if (response.status !in 200..299) throw IOException("HTTP ${response.status} for $path")
        return JsonParser.parseString(response.body).asJsonObject
    }

    private data class Challenge(
        val nonce: String,
    )
}
