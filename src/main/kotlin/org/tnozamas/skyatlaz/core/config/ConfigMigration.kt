package org.tnozamas.skyatlaz.core.config

import com.google.gson.JsonObject

/**
 * Upgrades stored values from [fromVersion] to `fromVersion + 1` (rename keys, convert units...). [migrate] edits
 * the flat `key -> value` object in place. Migrations are never removed, so any old file can still be read.
 */
class ConfigMigration(
    val fromVersion: Int,
    val migrate: (values: JsonObject) -> Unit,
) {
    companion object {
        /** The common case: a setting moved to a new key. */
        fun rename(
            fromVersion: Int,
            oldKey: String,
            newKey: String,
        ) = ConfigMigration(fromVersion) { values ->
            values.remove(oldKey)?.let { values.add(newKey, it) }
        }
    }
}
