package io.ktor.utils.io

import io.ktor.utils.io.core.*

/**
 * Channel for asynchronous writing of sequences of bytes.
 * This is a **single-writer channel**.
 *
 * Operations on this channel cannot be invoked concurrently, unless explicitly specified otherwise
 * in description. Exceptions are [close] and [flush].
 */
public interface ByteWriteChannel {

    public val maxSize: Int

    public val writablePacket: BytePacketBuilder

    /**
     * Returns `true` is channel has been closed and attempting to write to the channel will cause an exception.
     */
    public val isClosedForWrite: Boolean

    /**
     * Number of bytes written to the channel.
     * It is not guaranteed to be atomic so could be updated in the middle of write operation.
     */
    public val totalBytesWritten: Long

    /**
     * A closure causes exception or `null` if closed successfully or not yet closed
     */
    public val closedCause: Throwable?

    /**
     * Flush all pending bytes and close the channel
     */
    public suspend fun close()

    /**
     * Discard all not flushed bytes and close the channel
     */
    public fun cancel(cause: Throwable?): Boolean

    /**
     * Flushes all available bytes to the channel's destination.
     */
    public suspend fun flush()
}

/**
 * Indicates attempt to write on [isClosedForWrite][ByteWriteChannel.isClosedForWrite] channel
 * that was closed without a cause. A _failed_ channel rethrows the original [close][ByteWriteChannel.close] cause
 * exception on send attempts.
 */
public class ClosedWriteChannelException(message: String?) : CancellationException(message)
