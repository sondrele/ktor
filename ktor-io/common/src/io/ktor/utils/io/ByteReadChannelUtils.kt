/*
 * Copyright 2014-2023 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import io.ktor.utils.io.bits.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.core.internal.*
import kotlinx.atomicfu.*


public fun ByteReadChannel(packet: ByteReadPacket): ByteReadChannel = object : ByteReadChannel {
    private val initialSize: Long = packet.remaining
    override val readablePacket: ByteReadPacket = packet

    override fun exhausted(): Boolean {
        closedCause?.let { throw it }
        return packet.isEmpty
    }

    private val _closedCause = atomic<Throwable?>(null)
    override val closedCause: Throwable? = _closedCause.value

    override val totalBytesRead: Long = initialSize - availableForRead.toLong()

    override fun cancel(cause: Throwable?): Boolean {
        if (packet.isEmpty) return false
        if (!_closedCause.compareAndSet(null, cause)) return false

        packet.release()
        return true
    }

    override suspend fun awaitContent() {
        closedCause?.let { throw it }
    }
}

public fun ByteReadChannel.mapExceptions(
    mapper: (Throwable?) -> Throwable?
): ByteReadChannel = object : ByteReadChannel {
    override val readablePacket: ByteReadPacket get() = this@mapExceptions.readablePacket

    override fun exhausted(): Boolean = this@mapExceptions.exhausted()

    override val closedCause: Throwable?
        get() = this@mapExceptions.closedCause

    override val totalBytesRead: Long
        get() = this@mapExceptions.totalBytesRead

    override fun cancel(cause: Throwable?): Boolean {
        return this@mapExceptions.cancel(mapper(cause))
    }

    override suspend fun awaitContent() {
        this@mapExceptions.awaitContent()
    }
}

/**
 * Returns number of bytes that can be read without suspension. Read operations do no suspend and return
 * immediately when this number is at least the number of bytes requested for read.
 */
public val ByteReadChannel.availableForRead: Int get() = readablePacket.remaining.toInt()

/**
 * Same as [ByteReadChannel.exhausted]
 */
public val ByteReadChannel.isClosedForWrite: Boolean
    get() = kotlin.runCatching {
        exhausted()
    }.getOrNull() ?: true

/**
 * Returns `true` if the channel is closed and no remaining bytes are available for read.
 * It implies that [availableForRead] is zero.
 */
public val ByteReadChannel.isClosedForRead: Boolean get() = isClosedForWrite && availableForRead == 0

/**
 * Reads all available bytes to [dst] buffer and returns immediately or suspends if no bytes available
 * @return number of bytes were read or `-1` if the channel has been closed
 */
public suspend fun ByteReadChannel.readAvailable(dst: ByteArray, offset: Int = 0, length: Int = dst.size): Int {
    if (availableForRead == 0) awaitContent()
    if (isClosedForRead) return -1

    val count = minOf(length, availableForRead)
    readablePacket.readFully(dst, offset, count)
    return count
}

@Suppress("DEPRECATION")
public suspend fun ByteReadChannel.readAvailable(dst: ChunkBuffer): Int {
    if (availableForRead == 0) awaitContent()
    if (isClosedForRead) return -1

    val count = minOf(dst.writeRemaining, availableForRead)
    readablePacket.readFully(dst, count)
    return count
}

/**
 * Reads all [length] bytes to [dst] buffer or throws [EOFException] if channel has been closed and less bytes available.
 * Suspends if not enough bytes available.
 */
public suspend fun ByteReadChannel.readFully(dst: ByteArray, offset: Int = 0, length: Int = dst.size) {
    while (!exhausted() && availableForRead < length) {
        awaitContent()
    }

    if (availableForRead < length) {
        readablePacket.release()
        throw EOFException("Not enough bytes available ($availableForRead) to read $length bytes")
    }

    readablePacket.readFully(dst, offset, length)
}

/**
 * Reads all available bytes up to [max] to the [dst].
 */
@Suppress("DEPRECATION")
public suspend fun ByteReadChannel.readFully(dst: ChunkBuffer, max: Int = dst.writeRemaining) {
    while (availableForRead < max && !exhausted()) {
        awaitContent()
    }

    val count = minOf(max, availableForRead)
    readablePacket.readFully(dst, count)
}

/**
 * Reads the specified amount of bytes and makes a byte packet from them. Throws [EOFException] if channel has been closed
 * and not enough bytes available.
 */
public suspend fun ByteReadChannel.readPacket(size: Int): ByteReadPacket {
    while (!exhausted() && availableForRead < size) {
        awaitContent()
    }

    if (availableForRead < size) {
        readablePacket.release()
        throw EOFException("Not enough bytes available ($availableForRead) to read $size bytes")
    }

    return readablePacket.readPacket(size)
}

/**
 * Reads up to [limit] bytes and makes a byte packet or until end of stream encountered.
 */
public suspend fun ByteReadChannel.readRemaining(limit: Long = Long.MAX_VALUE): ByteReadPacket {
    while (!exhausted() && availableForRead < limit) {
        awaitContent()
    }

    val size = minOf(limit, availableForRead.toLong())
    return readablePacket.readPacket(size.toInt())
}

/**
 * Reads a long number (suspending if not enough bytes available) or fails if channel has been closed
 * and not enough bytes.
 */
public suspend fun ByteReadChannel.readLong(): Long {
    while (availableForRead < 8 && !exhausted()) {
        awaitContent()
    }

    if (availableForRead < 8) {
        readablePacket.release()
        throw EOFException("Not enough bytes available ($availableForRead) to read 8 bytes")
    }

    return readablePacket.readLong()
}

/**
 * Reads an int number (suspending if not enough bytes available) or fails if channel has been closed
 * and not enough bytes.
 */
public suspend fun ByteReadChannel.readInt(): Int {
    while (availableForRead < 4 && !exhausted()) {
        awaitContent()
    }

    if (availableForRead < 4) {
        readablePacket.release()
        throw EOFException("Not enough bytes available ($availableForRead) to read 4 bytes")
    }

    return readablePacket.readInt()
}

/**
 * Reads a short number (suspending if not enough bytes available) or fails if channel has been closed
 * and not enough bytes.
 */
public suspend fun ByteReadChannel.readShort(): Short {
    while (availableForRead < 2 && !exhausted()) {
        awaitContent()
    }

    if (availableForRead < 2) {
        readablePacket.release()
        throw EOFException("Not enough bytes available ($availableForRead) to read 2 bytes")
    }

    return readablePacket.readShort()
}

/**
 * Reads a byte (suspending if no bytes available yet) or fails if channel has been closed
 * and not enough bytes.
 */
public suspend fun ByteReadChannel.readByte(): Byte {
    while (availableForRead < 1 && !exhausted()) {
        awaitContent()
    }

    if (availableForRead < 1) {
        readablePacket.release()
        throw EOFException("Not enough bytes available ($availableForRead) to read 1 byte")
    }

    return readablePacket.readByte()
}

/**
 * Reads a boolean value (suspending if no bytes available yet) or fails if channel has been closed
 * and not enough bytes.
 */
public suspend fun ByteReadChannel.readBoolean(): Boolean = readByte().toInt() != 0

/**
 * Reads double number (suspending if not enough bytes available) or fails if channel has been closed
 * and not enough bytes.
 */
public suspend fun ByteReadChannel.readDouble(): Double {
    while (availableForRead < 8 && !exhausted()) {
        awaitContent()
    }

    if (availableForRead < 8) {
        readablePacket.release()
        throw EOFException("Not enough bytes available ($availableForRead) to read 8 bytes")
    }

    return readablePacket.readDouble()
}

/**
 * Reads float number (suspending if not enough bytes available) or fails if channel has been closed
 * and not enough bytes.
 */
public suspend fun ByteReadChannel.readFloat(): Float {
    while (availableForRead < 4 && !exhausted()) {
        awaitContent()
    }

    if (availableForRead < 4) {
        readablePacket.release()
        throw EOFException("Not enough bytes available ($availableForRead) to read 4 bytes")
    }

    return readablePacket.readFloat()
}

/**
 * Starts non-suspendable read session. After channel preparation [consumer] lambda will be invoked immediately
 * even if there are no bytes available for read yet.
 */
@Suppress("DEPRECATION")
@Deprecated("Use read { } instead.")
public fun ByteReadChannel.readSession(consumer: ReadSession.() -> Unit) {
    val session = object : ReadSession {
        override val availableForRead: Int
            get() = this@readSession.availableForRead

        override fun discard(n: Int): Int {
            val count = minOf(n, availableForRead)
            readablePacket.discard(count)
            return count
        }

        override fun request(atLeast: Int): ChunkBuffer? {
            if (availableForRead < atLeast) return null
            return readablePacket.prepareReadHead(atLeast)
        }
    }

    consumer(session)
}

/**
 * Starts a suspendable read session. After channel preparation [consumer] lambda will be invoked immediately
 * even if there are no bytes available for read yet. [consumer] lambda could suspend as much as needed.
 */
@Suppress("DEPRECATION")
@Deprecated("Use read { } instead.")
public suspend fun ByteReadChannel.readSuspendableSession(consumer: suspend SuspendableReadSession.() -> Unit) {
    val session = object : SuspendableReadSession {
        override suspend fun await(atLeast: Int): Boolean {
            while (availableForRead < atLeast && !exhausted()) {
                awaitContent()
            }

            return availableForRead >= atLeast
        }

        override val availableForRead: Int
            get() = this@readSuspendableSession.availableForRead

        override fun discard(n: Int): Int {
            val count = minOf(n, availableForRead)
            readablePacket.discard(count)
            return count
        }

        override fun request(atLeast: Int): ChunkBuffer? {
            if (availableForRead < atLeast) return null
            return readablePacket.prepareReadHead(atLeast)
        }
    }

    consumer(session)
}

public suspend fun ByteReadChannel.awaitContent(size: Int) {
    while (availableForRead < size && !exhausted()) {
        awaitContent()
    }

    if (availableForRead < size) {
        readablePacket.release()
        throw EOFException("Not enough bytes available ($availableForRead) to read $size bytes")
    }
}

/**
 * Discard up to [max] bytes
 *
 * @return number of bytes were discarded
 */
public suspend fun ByteReadChannel.discard(max: Long = Long.MAX_VALUE): Long {
    var remaining = max

    while (remaining > 0 && !exhausted()) {
        if (availableForRead == 0) awaitContent()

        val discarded = readablePacket.discard(minOf(remaining, readablePacket.remaining))
        remaining -= discarded
    }

    return max - remaining
}

/**
 * Discards exactly [n] bytes or fails if not enough bytes in the channel
 */
public suspend inline fun ByteReadChannel.discardExact(n: Long) {
    if (discard(n) != n) throw EOFException("Unable to discard $n bytes")
}

/**
 * Try to copy at least [min] but up to [max] bytes to the specified [destination] buffer from this input
 * skipping [offset] bytes. If there are not enough bytes available to provide [min] bytes after skipping [offset]
 * bytes then it will trigger the underlying source reading first and after that will
 * simply copy available bytes even if EOF encountered so [min] is not a requirement but a desired number of bytes.
 * It is safe to specify [max] greater than the destination free space.
 * `min` shouldn't be bigger than the [destination] free space.
 * This function could trigger the underlying source suspending reading.
 * It is allowed to specify too big [offset] so in this case this function will always return `0` after prefetching
 * all underlying bytes but note that it may lead to significant memory consumption.
 * This function usually copy more bytes than [min] (unless `max = min`) but it is not guaranteed.
 * When `0` is returned with `offset = 0` then it makes sense to check [isClosedForRead].
 *
 * @param destination to write bytes
 * @param offset to skip input
 * @param min bytes to be copied, shouldn't be greater than the buffer free space. Could be `0`.
 * @param max bytes to be copied even if there are more bytes buffered, could be [Int.MAX_VALUE].
 * @return number of bytes copied to the [destination] possibly `0`
 */
@Suppress("DEPRECATION")
@Deprecated(
    "Peek to is deprecated without replacement",
    level = DeprecationLevel.ERROR,
)
public suspend fun ByteReadChannel.peekTo(
    destination: Memory,
    destinationOffset: Long,
    offset: Long = 0,
    min: Long = 1,
    max: Long = Long.MAX_VALUE
): Long {
    error("Peek to is deprecated without replacement")
}

public suspend fun ByteReadChannel.joinTo(dst: ByteWriteChannel, closeOnEnd: Boolean) {
    try {
        while (!exhausted()) {
            awaitContent()
            dst.writePacket(readablePacket)
        }
    } catch (cause: Throwable) {
        dst.cancel(cause)
        throw cause
    } finally {
        if (closeOnEnd) dst.close()
    }
}

/**
 * Reads up to [limit] bytes from receiver channel and writes them to [dst] channel.
 * Closes [dst] channel if fails to read or write with cause exception.
 * @return a number of copied bytes
 */
public suspend fun ByteReadChannel.copyTo(dst: ByteWriteChannel, limit: Long = Long.MAX_VALUE): Long {
    try {
        var remaining = limit
        while (remaining > 0 && !exhausted()) {
            awaitContent()
            if (remaining > readablePacket.remaining) {
                remaining -= readablePacket.remaining
                dst.writePacket(readablePacket)
            } else {
                val packet = readablePacket.readPacket(remaining.toInt())
                dst.writePacket(packet)
                remaining = 0
            }
        }

        return limit - remaining
    } catch (cause: Throwable) {
        dst.cancel(cause)
        throw cause
    }
}

/**
 * Reads all the bytes from receiver channel and writes them to [dst] channel and then closes it.
 * Closes [dst] channel if fails to read or write with cause exception.
 * @return a number of copied bytes
 */

public suspend fun ByteReadChannel.copyAndClose(dst: ByteWriteChannel, limit: Long = Long.MAX_VALUE): Long = try {
    copyTo(dst, limit)
} finally {
    dst.close()
}

