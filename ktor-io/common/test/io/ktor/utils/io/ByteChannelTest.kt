/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

package io.ktor.utils.io

import io.ktor.test.dispatcher.*
import kotlinx.coroutines.*
import kotlin.test.*

@Suppress("DEPRECATION")
class ByteChannelTest {

    @Test
    fun testReadByteWithTimeout() = testSuspend {
        val channel = ByteChannel()

        launch {
            channel.writeByte(1)
            delay(1000)
            channel.writeByte(0)
            channel.flush()
        }

        var timedOut = false
        try {
            withTimeout(500) {
                channel.readByte()
            }
        } catch (_: TimeoutCancellationException) {
            timedOut = true
        }

        assertTrue(timedOut)
        assertEquals(1, channel.readByte())
    }

    @Test
    fun testReadIntWithTimeout() = testSuspend {
        val channel = ByteChannel()

        launch {
            channel.writeInt(1)
            delay(1000)
            channel.writeInt(0)
            channel.flush()
        }

        var timedOut = false
        try {
            withTimeout(500) {
                channel.readInt()
            }
        } catch (_: TimeoutCancellationException) {
            timedOut = true
        }

        assertTrue(timedOut)
        assertEquals(1, channel.readInt())
    }

    @Test
    fun testWriteByteWithTimeout() = testSuspend {
        val channel = ByteChannel()

        var timedOut = false
        try {
            withTimeout(500) {
                channel.writeFully(ByteArray(4088))
                channel.writeByte(1)
            }
        } catch (_: TimeoutCancellationException) {
            timedOut = true
        }

        assertTrue(timedOut)

        channel.readByte()
        channel.writeByte(42)

        channel.readFully(ByteArray(4087))
        channel.flush()
        assertEquals(42, channel.readByte())
    }
}
