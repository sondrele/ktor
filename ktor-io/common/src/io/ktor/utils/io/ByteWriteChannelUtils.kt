/*
 * Copyright 2014-2023 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import io.ktor.utils.io.bits.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.core.internal.*
import kotlin.math.*

public fun ByteWriteChannel.cancel(): Boolean = cancel(null)

@Deprecated(
    "Close with exception is deprecated. Use cancel instead.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("cancel(cause)")
)
public fun ByteWriteChannel.close(cause: Throwable) {
    cancel(cause)
}

/**
 * Returns number of bytes that can be written without suspension. Write operations do no suspend and return
 * immediately when this number is at least the number of bytes requested for write.
 */
public val ByteWriteChannel.availableForWrite: Int
    get() = max(0, writablePacket.size - maxSize)

/**
 * Returns `true` if channel flushes automatically all pending bytes after every write function call.
 * If `false` then flush only happens at manual [flush] invocation or when the buffer is full.
 */
public val ByteWriteChannel.autoFlush: Boolean get() = false

/**
 * Writes as much as possible and only suspends if buffer is full
 */
public suspend fun ByteWriteChannel.writeAvailable(
    src: ByteArray,
    offset: Int = 0,
    length: Int = src.size - offset
): Int {
    require(offset >= 0)
    require(length >= 0)
    require(offset + length <= src.size)

    val size = min(length, availableForWrite)

    if (size == 0) {
        flush()
    }

    writablePacket.writeFully(src, offset, size)
    return size
}

@Suppress("DEPRECATION")
public suspend fun ByteWriteChannel.writeAvailable(src: ChunkBuffer): Int {
    val size = min(src.readRemaining, availableForWrite)

    if (size == 0) {
        flush()
    }

    writablePacket.writeFully(src, size)
    return size
}

/**
 * Writes all [src] bytes and suspends until all bytes written. Causes flush if buffer filled up or when [autoFlush]
 * Crashes if channel get closed while writing.
 */
public suspend fun ByteWriteChannel.writeFully(src: ByteArray, offset: Int = 0, length: Int = src.size - offset) {
    require(offset >= 0)
    require(length >= 0)
    require(offset + length <= src.size)

    writablePacket.writeFully(src, offset, length)
    if (availableForWrite == 0) {
        flush()
    }
}

@Suppress("DEPRECATION")
public suspend fun ByteWriteChannel.writeFully(src: Buffer) {
    writablePacket.writeFully(src)
    if (availableForWrite == 0) {
        flush()
    }
}

@Suppress("DEPRECATION")
public suspend fun ByteWriteChannel.writeFully(memory: Memory, startIndex: Int, endIndex: Int) {
    writablePacket.writeFully(memory, startIndex, endIndex)
    if (availableForWrite == 0) {
        flush()
    }
}

@Suppress("DEPRECATION")
public suspend fun ByteWriteChannel.writeSuspendSession(visitor: suspend WriterSuspendSession.() -> Unit) {
    val session = object : WriterSuspendSession {
        override suspend fun tryAwait(n: Int) {
            TODO("Not yet implemented")
        }

        override fun request(min: Int): ChunkBuffer? {
            TODO("Not yet implemented")
        }

        override fun written(n: Int) {
            TODO("Not yet implemented")
        }

        override fun flush() {
            TODO("Not yet implemented")
        }
    }

    visitor(session)
}

/**
 * Writes a [packet] fully or fails if channel get closed before the whole packet has been written
 */
public suspend fun ByteWriteChannel.writePacket(packet: ByteReadPacket) {
    writablePacket.writePacket(packet)

    if (availableForWrite == 0) {
        flush()
    }
}

/**
 * Writes long number and suspends until written.
 * Crashes if channel get closed while writing.
 */
public suspend fun ByteWriteChannel.writeLong(l: Long) {
    writablePacket.writeLong(l)
    if (availableForWrite == 0) {
        flush()
    }
}

/**
 * Writes int number and suspends until written.
 * Crashes if channel get closed while writing.
 */
public suspend fun ByteWriteChannel.writeInt(i: Int) {
    writablePacket.writeInt(i)
    if (availableForWrite == 0) {
        flush()
    }
}

/**
 * Writes short number and suspends until written.
 * Crashes if channel get closed while writing.
 */
public suspend fun ByteWriteChannel.writeShort(s: Short) {
    writablePacket.writeShort(s)
    if (availableForWrite == 0) {
        flush()
    }
}

/**
 * Writes byte and suspends until written.
 * Crashes if channel get closed while writing.
 */
public suspend fun ByteWriteChannel.writeByte(b: Byte) {
    writablePacket.writeByte(b)
    if (availableForWrite == 0) {
        flush()
    }
}

/**
 * Writes double number and suspends until written.
 * Crashes if channel get closed while writing.
 */
public suspend fun ByteWriteChannel.writeDouble(d: Double) {
    writablePacket.writeDouble(d)
    if (availableForWrite == 0) {
        flush()
    }
}

/**
 * Writes float number and suspends until written.
 * Crashes if channel get closed while writing.
 */
public suspend fun ByteWriteChannel.writeFloat(f: Float) {
    writablePacket.writeFloat(f)
    if (availableForWrite == 0) {
        flush()
    }
}

/**
 * Invokes [block] when at least 1 byte is available for write.
 */
public suspend fun ByteWriteChannel.awaitFreeSpace() {
    if (availableForWrite == 0) flush()
}

public suspend fun ByteWriteChannel.writeShort(s: Int) {
    return writeShort((s and 0xffff).toShort())
}

public suspend fun ByteWriteChannel.writeShort(s: Int, byteOrder: ByteOrder) {
    return writeShort((s and 0xffff).toShort(), byteOrder)
}

public suspend fun ByteWriteChannel.writeByte(b: Int) {
    return writeByte((b and 0xff).toByte())
}

public suspend fun ByteWriteChannel.writeInt(i: Long) {
    return writeInt(i.toInt())
}

public suspend fun ByteWriteChannel.writeInt(i: Long, byteOrder: ByteOrder) {
    return writeInt(i.toInt(), byteOrder)
}

public suspend fun ByteWriteChannel.writeStringUtf8(s: CharSequence) {
    val packet = buildPacket {
        writeText(s)
    }

    return writePacket(packet)
}

public suspend fun ByteWriteChannel.writeStringUtf8(s: String) {
    val packet = buildPacket {
        writeText(s)
    }

    return writePacket(packet)
}

public suspend fun ByteWriteChannel.writeBoolean(b: Boolean) {
    return writeByte(if (b) 1 else 0)
}

/**
 * Writes UTF16 character
 */
public suspend fun ByteWriteChannel.writeChar(ch: Char) {
    return writeShort(ch.code)
}

public suspend inline fun ByteWriteChannel.writePacket(builder: BytePacketBuilder.() -> Unit) {
    return writePacket(buildPacket(builder))
}

public suspend fun ByteWriteChannel.writePacketSuspend(builder: suspend BytePacketBuilder.() -> Unit) {
    return writePacket(buildPacket { builder() })
}

public fun ByteWriteChannel.mapExceptions(
    block: (Throwable?) -> Throwable?
): ByteWriteChannel = object : ByteWriteChannel {
    override val maxSize: Int
        get() = this@mapExceptions.maxSize

    override val writablePacket: BytePacketBuilder
        get() = this@mapExceptions.writablePacket

    override val isClosedForWrite: Boolean
        get() = this@mapExceptions.isClosedForWrite

    override val totalBytesWritten: Long
        get() = this@mapExceptions.totalBytesWritten

    override val closedCause: Throwable?
        get() = this@mapExceptions.closedCause

    override suspend fun close() {
        this@mapExceptions.close()
    }

    override fun cancel(cause: Throwable?): Boolean {
        return this@mapExceptions.cancel(block(cause))
    }

    override suspend fun flush() {
        this@mapExceptions.flush()
    }
}
