package org.tnozamas.skyatlaz.core.debug

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

/**
 * Dump files in `<game dir>/skyatlaz/dumps/<name>.json`. File I/O: call [save] and [load] from the scheduler's
 * async work, never directly on the client thread.
 */
class SnapshotStore(
    private val directory: Path,
) {
    fun save(snapshot: Snapshot): Path {
        Files.createDirectories(directory)
        val file = file(snapshot.name)
        Files.writeString(file, snapshot.toJson())
        return file
    }

    /** The dump called [name], or null if there is none. */
    fun load(name: String): Snapshot? {
        val file = file(name)
        return if (Files.exists(file)) Snapshot.fromJson(Files.readString(file)) else null
    }

    /** Names of saved dumps, for tab completion. Reads one small directory listing. */
    fun names(): List<String> {
        if (!Files.isDirectory(directory)) return emptyList()
        return Files.list(directory).use { files ->
            files
                .filter { it.extension == "json" }
                .map { it.nameWithoutExtension }
                .sorted()
                .toList()
        }
    }

    private fun file(name: String): Path {
        require(NAME_PATTERN.matches(name)) { "Invalid dump name '$name'" }
        return directory.resolve("$name.json")
    }

    companion object {
        /** Also what the `dump` command accepts, so names are safe file names on every OS. */
        val NAME_PATTERN = Regex("[a-z0-9_-]{1,48}")
    }
}
