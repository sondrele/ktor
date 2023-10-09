/*
 * Copyright 2014-2023 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import kotlinx.coroutines.*
import kotlin.coroutines.*

@Suppress("DEPRECATION")
public fun CoroutineScope.reader(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend WriteChannelBuilder.() -> Unit
): ByteWriteChannel = TODO()


public class WriteChannelBuilder {
}
