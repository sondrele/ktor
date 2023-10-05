/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

package io.ktor.client.network.sockets

import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.*

/**
 * This exception is thrown in case connect timeout exceeded.
 */
public expect class ConnectTimeoutException(message: String, cause: Throwable? = null) : IOException

/**
 * This exception is thrown in case socket timeout (read or write) exceeded.
 */
public expect class SocketTimeoutException(message: String, cause: Throwable? = null) : IOException

internal expect val timeoutExceptionMapper: (HttpRequestData) -> ((Throwable?) -> Throwable?)

/**
 * Returns [ByteReadChannel] with [ByteChannel.close] handler that returns [SocketTimeoutException] instead of
 * [SocketTimeoutException].
 */
@InternalAPI
public fun CoroutineScope.mapEngineExceptions(input: ByteReadChannel, request: HttpRequestData): ByteReadChannel {
    if (PlatformUtils.IS_NATIVE) {
        return input
    }

    val mapper = timeoutExceptionMapper(request)
    return input.mapExceptions(mapper)
}

/**
 * Returns [ByteWriteChannel] with [ByteChannel.close] handler that returns [SocketTimeoutException] instead of
 * [SocketTimeoutException].
 */
@InternalAPI
public fun CoroutineScope.mapEngineExceptions(output: ByteWriteChannel, request: HttpRequestData): ByteWriteChannel {
    if (PlatformUtils.IS_NATIVE) {
        return output
    }

    val mapper = timeoutExceptionMapper(request)
    return output.mapExceptions(mapper)
}
