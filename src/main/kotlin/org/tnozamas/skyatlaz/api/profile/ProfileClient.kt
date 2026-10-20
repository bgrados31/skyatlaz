package org.tnozamas.skyatlaz.api.profile

import com.google.gson.JsonParser
import org.tnozamas.skyatlaz.api.http.HttpResult
import org.tnozamas.skyatlaz.api.http.HttpService
import java.time.Duration

/**
 * Reads the profile of the player from the Hypixel API.
 *
 * The profile endpoints ask for the developer API key of the player in a header, so without a key nothing is
 * requested. The uuid of the player comes from the Mojang API (the name of the session), and it never changes, so
 * it is asked only once.
 *
 * Call from the client thread; [callback] also runs there.
 */
class ProfileClient(
    private val http: HttpService,
    private val apiKey: () -> String,
    private val onProblem: (String) -> Unit,
) {
    private val uuids = HashMap<String, String>()

    /**
     * The mining tree of the player with that name. [callback] gets null when the name has no profile, when there is
     * no API key or when the request fails.
     */
    fun loadMining(
        name: String,
        callback: (ProfileMining?) -> Unit,
    ) {
        val key = apiKey().trim()
        if (key.isEmpty()) {
            callback(null)
            return
        }
        uuidOf(name) { uuid ->
            if (uuid == null) {
                callback(null)
                return@uuidOf
            }
            loadMiningOf(uuid, key, callback)
        }
    }

    /** The mining tree of the player with that uuid (with dashes or without). */
    fun loadMiningOf(
        uuid: String,
        callback: (ProfileMining?) -> Unit,
    ) {
        val key = apiKey().trim()
        if (key.isEmpty()) {
            callback(null)
            return
        }
        loadMiningOf(uuid, key, callback)
    }

    private fun loadMiningOf(
        uuid: String,
        key: String,
        callback: (ProfileMining?) -> Unit,
    ) {
        val url = ProfileApi.PROFILE_URL + "?uuid=" + uuid
        http.get(
            url = url,
            maxAge = MAX_AGE,
            headers = mapOf(ProfileApi.API_KEY_HEADER to key),
            parse = { ProfileApi.parseMining(it, uuid) },
            callback = { result ->
                when (result) {
                    is HttpResult.Success -> {
                        callback(result.value)
                    }

                    is HttpResult.Failure -> {
                        onProblem("Could not read the profile: ${result.error.message}")
                        callback(null)
                    }
                }
            },
        )
    }

    /** The uuid of the player with that name, asked once. */
    private fun uuidOf(
        name: String,
        callback: (String?) -> Unit,
    ) {
        uuids[name]?.let {
            callback(it)
            return
        }
        http.get(
            url = MOJANG_URL + name,
            maxAge = UUID_MAX_AGE,
            parse = { body ->
                JsonParser
                    .parseString(body)
                    .asJsonObject
                    .get("id")
                    ?.asString
                    .orEmpty()
            },
            callback = { result ->
                when (result) {
                    is HttpResult.Success -> {
                        val uuid = result.value
                        if (uuid.isEmpty()) {
                            onProblem("The name $name is not a Minecraft account")
                            callback(null)
                        } else {
                            uuids[name] = uuid
                            callback(uuid)
                        }
                    }

                    is HttpResult.Failure -> {
                        onProblem("Could not ask for the uuid of $name: ${result.error.message}")
                        callback(null)
                    }
                }
            },
        )
    }

    private companion object {
        /** The Mojang API maps a name to the uuid of the account. */
        const val MOJANG_URL = "https://api.mojang.com/users/profiles/minecraft/"

        /** The uuid of a name never changes. */
        val UUID_MAX_AGE: Duration = Duration.ofDays(30)

        /** The profile of a player changes slowly; a minute is enough for a HUD that shows it. */
        val MAX_AGE: Duration = Duration.ofMinutes(5)
    }
}
