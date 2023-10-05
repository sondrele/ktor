package io.ktor.utils.io

import io.ktor.utils.io.core.*
import kotlinx.cinterop.*

/**
 * Reads all available bytes to [dst] buffer and returns immediately or suspends if no bytes available
 * @return number of bytes were read or `-1` if the channel has been closed
 */
@OptIn(ExperimentalForeignApi::class)
public suspend fun ByteReadChannel.readAvailable(dst: CPointer<ByteVar>, offset: Int, length: Int): Int {
    return readAvailable(dst, offset.toLong(), length.toLong())
}

/**
 * Reads all available bytes to [dst] buffer and returns immediately or suspends if no bytes available
 * @return number of bytes were read or `-1` if the channel has been closed
 */
@OptIn(ExperimentalForeignApi::class)
public suspend fun ByteReadChannel.readAvailable(dst: CPointer<ByteVar>, offset: Long, length: Long): Int {
    if (availableForRead == 0) {
        awaitContent()
    }

    if (exhausted()) return -1

    return readablePacket.readAvailable(dst, offset, length).toInt()
}


/**
 * Reads all [length] bytes to [dst] buffer or fails if channel has been closed.
 * Suspends if not enough bytes available.
 */
@OptIn(ExperimentalForeignApi::class)
public suspend fun ByteReadChannel.readFully(dst: CPointer<ByteVar>, offset: Int, length: Int) {
    readFully(dst, offset.toLong(), length.toLong())
}

/**
 * Reads all [length] bytes to [dst] buffer or throws [EOFException] if channel has been closed.
 * Suspends if not enough bytes available.
 */
@OptIn(ExperimentalForeignApi::class)
public suspend fun ByteReadChannel.readFully(dst: CPointer<ByteVar>, offset: Long, length: Long) {
    while (availableForRead < length && !exhausted()) {
        awaitContent()
    }

    if (availableForRead < length) {
        readablePacket.release()
        throw EOFException("Not enough bytes available ($availableForRead) to read $length bytes")
    }

    readablePacket.readFully(dst, offset, length)
}
