package io.ktor.utils.io

/**
 * Creates a buffered channel for asynchronous reading and writing of sequences of bytes using [close] function to close
 * a channel.
 */
@Suppress("DEPRECATION")
@Deprecated(
    "Exception mapping for [ByteChannel] is deprecated. Please consider using mapping for [ByteReadChannel] and [ByteWriteChannel] instead.",
    level = DeprecationLevel.ERROR
)
public fun ByteChannel(autoFlush: Boolean = false, exceptionMapper: (Throwable?) -> Throwable?): ByteChannel {
    error("Exception mapping for [ByteChannel] is deprecated. Please consider using mapping for [ByteReadChannel] and [ByteWriteChannel] instead.")
}

