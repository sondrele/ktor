/*
 * Copyright 2014-2023 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import io.ktor.utils.io.core.*
import org.khronos.webgl.*

/**
 * Reads bytes from the channel into the given destination array starting at the specified offset,
 * up to the specified length.
 *
 * @param dst the destination array buffer
 * @param offset the starting offset in the destination array buffer
 * @param length the number of bytes to read
 * @return the number of bytes read, or -1 if the channel is exhausted
 * @throws IllegalArgumentException if offset or length is negative, or if offset + length > dst.byteLength
 * @throws Throwable if the channel has been cancelled with an exception
 */
public suspend fun ByteReadChannel.readAvailable(dst: ArrayBuffer, offset: Int, length: Int): Int {
    require(offset >= 0) { "offset shouldn't be negative: $offset" }
    require(length >= 0) { "length shouldn't be negative: $length" }
    require(offset + length <= dst.byteLength) { "offset + length > dst.size: $offset + $length > ${dst.byteLength}" }

    if (availableForRead == 0) {
        awaitContent()
    }

    if (availableForRead == 0) {
        return -1
    }

    return readablePacket.readAvailable(dst, offset, length)
}

/**
 * Reads bytes from the ByteReadChannel and stores them in the provided ArrayBuffer.
 *
 * @param dst The ArrayBuffer to store the read bytes.
 * @param offset The starting index in the dst ArrayBuffer to store the read bytes.
 * @param length The number of bytes to read from the channel.
 * @throws IllegalArgumentException if offset is negative, length is negative, or offset + length > dst.byteLength.
 * @throws IllegalStateException if the channel has been cancelled with exception.
 * @throws IOException if an I/O error occurs.
 */
public suspend fun ByteReadChannel.readFully(dst: ArrayBuffer, offset: Int, length: Int) {
    require(offset >= 0) { "offset shouldn't be negative: $offset" }
    require(length >= 0) { "length shouldn't be negative: $length" }
    require(offset + length <= dst.byteLength) { "offset + length > dst.size: $offset + $length > ${dst.byteLength}" }

    if (length == 0) return

    while (availableForRead < length) {
        awaitContent()
    }

    readablePacket.readFully(dst, offset, length)
}

