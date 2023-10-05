/*
 * Copyright 2014-2023 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import io.ktor.utils.io.core.*
import kotlinx.cinterop.*


/**
 * Writes as much as possible and only suspends if buffer is full
 */
@OptIn(ExperimentalForeignApi::class)
public suspend fun ByteWriteChannel.writeAvailable(src: CPointer<ByteVar>, offset: Int, length: Int): Int =
    writeAvailable(src, offset.toLong(), length.toLong())

/**
 * Writes as much as possible and only suspends if buffer is full
 */
@OptIn(ExperimentalForeignApi::class)
public suspend fun ByteWriteChannel.writeAvailable(src: CPointer<ByteVar>, offset: Long, length: Long): Int {
    if (availableForWrite == 0) {
        flush()
    }

    val min = minOf(length, availableForWrite.toLong())
    writablePacket.writeFully(src, offset, min)
    return min.toInt()
}


/**
 * Writes all [src] bytes and suspends until all bytes written. Causes flush if buffer filled up or when [autoFlush]
 * Crashes if channel get closed while writing.
 */
@OptIn(ExperimentalForeignApi::class)
public suspend fun ByteWriteChannel.writeFully(src: CPointer<ByteVar>, offset: Int, length: Int) {
    writeFully(src, offset.toLong(), length.toLong())
}

/**
 * Writes all [src] bytes and suspends until all bytes written. Causes flush if buffer filled up or when [autoFlush]
 * Crashes if channel get closed while writing.
 */
@OptIn(ExperimentalForeignApi::class)
public suspend fun ByteWriteChannel.writeFully(src: CPointer<ByteVar>, offset: Long, length: Long) {
    writablePacket.writeFully(src, offset, length)
    if (availableForWrite == 0) {
        flush()
    }
}

