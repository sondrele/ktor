/*
 * Copyright 2014-2023 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import io.ktor.utils.io.core.*
import java.nio.*


/**
 * Creates a channel for reading from the specified byte buffer.
 */
public fun ByteReadChannel(content: ByteBuffer): ByteReadChannel = ByteReadChannel(buildPacket {
    writeFully(content)
})

/**
 * Reads bytes from the channel and invokes the specified consumer function on each read.
 *
 * @param min The minimum number of bytes to read before invoking the consumer function.
 * @param consumer The function to be invoked with the read bytes.
 */
public suspend fun ByteReadChannel.read(min: Int, consumer: (ByteBuffer) -> Unit) {
    while (availableForRead < min && !exhausted()) {
        awaitContent()
    }

    if (availableForRead < min) {
        throw EOFException("Not enough bytes available ($availableForRead) to read $min bytes")
    }

    readablePacket.readDirect(min, consumer)
}

/**
 * Reads at most `dst.remaining()` bytes to the specified [dst] byte buffer and changes its position accordingly.
 *
 * @param dst the destination byte buffer to read into
 * @return the number of bytes copied or -1 if channel has reached its end
 */
public suspend fun ByteReadChannel.readAvailable(dst: ByteBuffer): Int {
    if (availableForRead == 0) {
        awaitContent()
    }

    if (availableForRead == 0) return -1
    return readablePacket.readAvailable(dst)
}

public suspend fun ByteReadChannel.readFully(dst: ByteBuffer) {
    while (!exhausted() && availableForRead < dst.remaining()) {
        awaitContent()
    }

    if (availableForRead < dst.remaining()) {
        readablePacket.release()
        throw EOFException("Not enough bytes available ($availableForRead) to read ${dst.remaining()} bytes")
    }

    readablePacket.readFully(dst)
}

public fun ByteReadChannel.readAvailable(block: (ByteBuffer) -> Int): Int {
    if (availableForRead == 0) {
        return 0
    }

    readablePacket.readDirect(1) {
        return block(it)
    }
}

/**
 * Executes the given visitor function on the ByteReadChannel with look-ahead capabilities.
 *
 * @param visitor the function to execute on the ByteReadChannel
 * @return the result of executing the visitor function
 */
public fun <R> ByteReadChannel.lookAhead(visitor: LookAheadSession.() -> R): R {
    val session = Session(this)
    val result = session.visitor()
    return result
}

/**
 * Executes the given visitor function on the ByteReadChannel with look-ahead capabilities.
 *
 * @param visitor the function to execute on the ByteReadChannel
 * @return the result of executing the visitor function
 */
public suspend fun <R> ByteReadChannel.lookAheadSuspend(visitor: suspend LookAheadSuspendSession.() -> R): R {
    val session = Session(this)
    val result = session.visitor()
    return result
}

private class Session(private val channel: ByteReadChannel) : LookAheadSuspendSession {
    override suspend fun awaitAtLeast(n: Int): Boolean {
        runCatching {
            channel.awaitContent(n)
        }
        return channel.availableForRead >= n
    }

    override fun consumed(n: Int) {
        require(n >= 0) { "Can't discard negative amount of bytes: $n" }
        require(n <= channel.availableForRead) { "Can't discard $n, only ${channel.availableForRead} bytes available" }

        channel.readablePacket.discard(n)
    }

    override fun request(skip: Int, atLeast: Int): ByteBuffer? {
        if (channel.availableForRead < skip + atLeast) return null

        val packet = channel.readablePacket.copy()
        if (skip > 0) packet.discard(skip)
        return packet.readByteBuffer(atLeast)
    }
}
