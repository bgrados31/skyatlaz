package org.tnozamas.skyatlaz.core.error

/**
 * Feature code is isolated by catching [Throwable] (a `NoSuchMethodError` after a game update must disable one
 * module, not crash the client), but errors the JVM cannot recover from must still propagate.
 */
internal fun Throwable.rethrowIfFatal() {
    if (this is VirtualMachineError) throw this
}
