/*
 * Copyright 2014-2023 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import io.ktor.utils.io.core.*
import kotlinx.coroutines.*

@Suppress("DEPRECATION")
internal class ConflatedByteChannel : ByteChannel {
    override fun attachJob(job: Job) {
        TODO("Not yet implemented")
    }

    override val readablePacket: ByteReadPacket
        get() = TODO("Not yet implemented")

    override fun exhausted(): Boolean {
        TODO("Not yet implemented")
    }

    override val closedCause: Throwable?
        get() = TODO("Not yet implemented")
    override val totalBytesRead: Long
        get() = TODO("Not yet implemented")

    override fun cancel(cause: Throwable?): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun awaitContent() {
        TODO("Not yet implemented")
    }

    override val maxSize: Int
        get() = TODO("Not yet implemented")
    override val writablePacket: BytePacketBuilder
        get() = TODO("Not yet implemented")
    override val isClosedForWrite: Boolean
        get() = TODO("Not yet implemented")
    override val totalBytesWritten: Long
        get() = TODO("Not yet implemented")

    override fun close(cause: Throwable?): Boolean {
        TODO("Not yet implemented")
    }

    override fun flush() {
        TODO("Not yet implemented")
    }
}
