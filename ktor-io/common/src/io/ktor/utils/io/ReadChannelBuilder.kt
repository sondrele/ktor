/*
 * Copyright 2014-2023 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlin.coroutines.*


public fun CoroutineScope.writer(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend ReadChannelBuilder.() -> Unit
): ByteReadChannel = object : ByteReadChannel, ReadChannelBuilder {

    init {
        launch(coroutineContext) {
            try {
            } catch (cause: Throwable) {
            } finally {
            }
        }
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

    override suspend fun write(packet: ByteReadPacket) {
        TODO("")
    }
}

public fun interface ReadChannelBuilder {
    public suspend fun write(packet: ByteReadPacket)
}
