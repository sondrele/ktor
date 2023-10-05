package io.ktor.utils.io

import io.ktor.utils.io.core.*
import io.ktor.utils.io.js.*
import org.khronos.webgl.*

/**
 * Creates channel for reading from the specified [ArrayBufferView]
 */
public fun ByteReadChannel(content: ArrayBufferView): ByteReadChannel {
    val packet = buildPacket {
        writeFully(content.buffer, content.byteOffset, content.byteLength)
    }

    return ByteReadChannel(packet)
}
