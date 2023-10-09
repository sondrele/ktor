package io.ktor.utils.io

import io.ktor.utils.io.core.*

/**
 * Channel for asynchronous reading of sequences of bytes.
 * This is a **single-reader channel**.
 *
 * Operations on this channel cannot be invoked concurrently.
 */
public interface ByteReadChannel {

    /**
     * The [readablePacket] represents bytes available to read from this channel.
     *
     * If you need to have more bytes, use [awaitContent] method.
     */
    public val readablePacket: ByteReadPacket

    /**
     * Checks if the channel is exhausted, meaning there are no more bytes available for [awaitContent].
     * It still can have some bytes in the [readablePacket].
     *
     * @return true if the channel is exhausted, false otherwise
     * @throws [closedCause] if the channel has been cancelled with exception
     */
    public fun exhausted(): Boolean

    /**
     * A closure causes exception or `null` if closed successfully or not yet closed
     */
    public val closedCause: Throwable?

    /**
     * Number of bytes read from the channel.
     * It is not guaranteed to be atomic so could be updated in the middle of long-running read operation.
     */
    public val totalBytesRead: Long

    /**
     * Close channel with optional [cause] cancellation. Unlike [ByteWriteChannel.close] that could close channel
     * normally, cancel does always close with error so any operations on this channel will always fail
     * and all suspensions will be resumed with exception.
     *
     * Please note that if the channel has been provided by [reader] or [writer] then the corresponding owning
     * coroutine will be cancelled as well
     *
     * @see ByteWriteChannel.close
     */
    public fun cancel(cause: Throwable? = null): Boolean

    /**
     * Suspend until the channel has bytes to read or gets closed. Throws exception if the channel was closed with an error.
     */
    public suspend fun awaitContent()

    public companion object {
        /**
         * Represents an empty byte read channel. This channel is already exhausted, meaning there are no more bytes available
         * for reading. It has a total of 0 bytes read and no throwable cause for being closed.
         */
        public val Empty: ByteReadChannel = object : ByteReadChannel {
            override val readablePacket: ByteReadPacket = ByteReadPacket.Empty

            override fun exhausted(): Boolean = true

            override val closedCause: Throwable? = null
            override val totalBytesRead: Long = 0

            override fun cancel(cause: Throwable?): Boolean {
                return false
            }

            override suspend fun awaitContent() {
            }
        }
    }
}
